package com.euthopiar.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile

enum class ContainerType { TS, FMP4, MP4, UNKNOWN }

/**
 * Sniffs the real container format without trusting the file extension.
 */
fun sniffContainer(file: File): ContainerType {
    if (!file.exists() || file.length() < 16) return ContainerType.UNKNOWN
    return try {
        val b = ByteArray(380)
        file.inputStream().use { it.read(b) }
        when {
            b.size >= 189 && b[0] == 0x47.toByte() && b[188] == 0x47.toByte() -> ContainerType.TS
            file.name.endsWith(".ts", ignoreCase = true) || (b.isNotEmpty() && b[0] == 0x47.toByte()) -> ContainerType.TS
            String(b, 4, 4, Charsets.ISO_8859_1) in setOf("ftyp", "styp", "moov") -> {
                if (scanMoofOffsets(file) != null) ContainerType.FMP4 else ContainerType.MP4
            }
            else -> ContainerType.UNKNOWN
        }
    } catch (_: Throwable) {
        ContainerType.UNKNOWN
    }
}

/**
 * MPEG-TS Seek Index: Contains packet offsets aligned to 188 bytes and corresponding timestamps.
 */
class TsIndex(val offsets: LongArray, val timesUs: LongArray, val durationUs: Long)

object TsIndexer {
    private const val P = 188

    fun build(file: File, minGapUs: Long = 2_000_000L): TsIndex {
        val offs = ArrayList<Long>()
        val times = ArrayList<Long>()
        var firstPts = -1L
        var lastPts = 0L
        var wrapBase = 0L
        var prevRaw = -1L
        var lastPat = -1L
        var lastKeyUs = Long.MIN_VALUE
        val buf = ByteArray(P * 4096)
        var base = 0L

        RandomAccessFile(file, "r").use { raf ->
            val start = findSync(raf)
            raf.seek(start)
            base = start
            while (true) {
                val readBytes = raf.read(buf)
                if (readBytes <= 0) break
                val n = readBytes - (readBytes % P)
                if (n <= 0) break
                for (o in 0 until n step P) {
                    val pos = base + o
                    if (buf[o] != 0x47.toByte()) continue
                    val pusi = (buf[o + 1].toInt() and 0x40) != 0
                    val pid = ((buf[o + 1].toInt() and 0x1F) shl 8) or (buf[o + 2].toInt() and 0xFF)
                    val afc = (buf[o + 3].toInt() shr 4) and 3
                    if (pid == 0 && pusi) {
                        lastPat = pos
                        continue
                    }
                    if (!pusi || (afc and 1) == 0) continue
                    var p = o + 4
                    var rai = false
                    if ((afc and 2) != 0) {
                        val afl = buf[p].toInt() and 0xFF
                        if (afl > 0 && p + 1 < o + P) rai = (buf[p + 1].toInt() and 0x40) != 0
                        p += 1 + afl
                    }
                    if (p + 14 > o + P) continue
                    val isVideoPes = buf[p].toInt() == 0 && buf[p + 1].toInt() == 0 && buf[p + 2].toInt() == 1 &&
                            (buf[p + 3].toInt() and 0xF0) == 0xE0
                    if (!isVideoPes || (!rai && offs.isNotEmpty()) || (buf[p + 7].toInt() and 0x80) == 0) continue
                    val raw = readPts(buf, p + 9)
                    if (prevRaw != -1L && raw + (1L shl 32) < prevRaw) wrapBase += (1L shl 33)
                    prevRaw = raw
                    val pts = raw + wrapBase
                    if (firstPts < 0) firstPts = pts
                    val tUs = (pts - firstPts) * 1_000_000L / 90_000L
                    lastPts = pts
                    if (tUs - lastKeyUs >= minGapUs || offs.isEmpty()) {
                        offs += if (lastPat >= 0 && pos - lastPat <= 10 * P) lastPat else pos
                        times += tUs
                        lastKeyUs = tUs
                    }
                }
                base += n
            }
        }
        val dur = if (lastPts > firstPts) {
            (lastPts - firstPts) * 1_000_000L / 90_000L + 2_000_000L
        } else {
            0L
        }
        return TsIndex(offs.toLongArray(), times.toLongArray(), dur)
    }

    private fun readPts(b: ByteArray, i: Int): Long =
        ((b[i].toLong() and 0x0E) shl 29) or ((b[i + 1].toLong() and 0xFF) shl 22) or
                ((b[i + 2].toLong() and 0xFE) shl 14) or ((b[i + 3].toLong() and 0xFF) shl 7) or ((b[i + 4].toLong() and 0xFE) shr 1)

    private fun findSync(raf: RandomAccessFile): Long {
        val check = ByteArray(P * 2)
        val read = raf.read(check)
        if (read >= P * 2) {
            for (i in 0 until P) {
                if (check[i] == 0x47.toByte() && check[i + P] == 0x47.toByte()) {
                    return i.toLong()
                }
            }
        }
        return 0L
    }
}

/**
 * Cached loader for TsIndex: scans once and saves movie.ts.idx for instant 0ms loads subsequently.
 */
fun getOrBuildTsIndex(file: File, knownDurationUs: Long = 0L): TsIndex {
    val idxFile = File(file.parentFile, "${file.name}.idx")
    if (idxFile.exists() && idxFile.lastModified() >= file.lastModified()) {
        try {
            DataInputStream(idxFile.inputStream().buffered()).use { dis ->
                val count = dis.readInt()
                val offsets = LongArray(count)
                val times = LongArray(count)
                for (i in 0 until count) offsets[i] = dis.readLong()
                for (i in 0 until count) times[i] = dis.readLong()
                val dur = dis.readLong()
                if (count > 0 && dur > 0) {
                    return TsIndex(offsets, times, dur)
                }
            }
        } catch (_: Throwable) {
            idxFile.delete()
        }
    }

    var index = TsIndexer.build(file)
    if (index.offsets.isEmpty() || index.durationUs <= 0L) {
        val durUs = if (index.durationUs > 0) index.durationUs else (if (knownDurationUs > 0) knownDurationUs else getTsFileDurationMs(file) * 1000L)
        val finalDurUs = if (durUs > 0) durUs else 3600_000_000L
        val numSegments = 120
        val offsets = LongArray(numSegments)
        val times = LongArray(numSegments)
        val len = file.length()
        for (i in 0 until numSegments) {
            times[i] = (finalDurUs * i) / numSegments
            var pos = (len * i) / numSegments
            pos = (pos / 188L) * 188L
            offsets[i] = pos.coerceIn(0L, (len - 188L).coerceAtLeast(0L))
        }
        index = TsIndex(offsets, times, finalDurUs)
    }

    try {
        DataOutputStream(idxFile.outputStream().buffered()).use { dos ->
            dos.writeInt(index.offsets.size)
            for (off in index.offsets) dos.writeLong(off)
            for (t in index.timesUs) dos.writeLong(t)
            dos.writeLong(index.durationUs)
        }
    } catch (_: Throwable) {}

    return index
}

class IndexedSeekMap(private val ix: TsIndex) : SeekMap {
    override fun isSeekable(): Boolean = true
    override fun getDurationUs(): Long = ix.durationUs
    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        if (ix.timesUs.isEmpty()) {
            val pt = SeekPoint(timeUs, 0L)
            return SeekMap.SeekPoints(pt, pt)
        }
        val idx = ix.timesUs.binarySearch(timeUs).let { if (it >= 0) it else -it - 2 }.coerceIn(0, ix.timesUs.size - 1)
        val a = SeekPoint(ix.timesUs[idx], ix.offsets[idx])
        return if (idx + 1 < ix.offsets.size) {
            SeekMap.SeekPoints(a, SeekPoint(ix.timesUs[idx + 1], ix.offsets[idx + 1]))
        } else {
            SeekMap.SeekPoints(a)
        }
    }
}

@OptIn(UnstableApi::class)
class IndexedTsExtractor(private val ix: TsIndex) : Extractor {
    private val delegate = TsExtractor(
        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM
    )

    override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

    override fun init(output: ExtractorOutput) {
        delegate.init(object : ExtractorOutput by output {
            override fun seekMap(map: SeekMap) {
                output.seekMap(IndexedSeekMap(ix))
            }
        })
    }

    override fun read(input: ExtractorInput, pos: PositionHolder): Int = delegate.read(input, pos)

    override fun seek(position: Long, timeUs: Long) {
        val i = ix.offsets.indexOf(position).takeIf { it >= 0 }
        delegate.seek(position, if (i != null && i < ix.timesUs.size) ix.timesUs[i] else timeUs)
    }

    override fun release() = delegate.release()
}

/**
 * Scans an fMP4 file to locate byte offsets of all top-level `moof` (Movie Fragment) boxes.
 */
fun scanMoofOffsets(file: File): LongArray? {
    if (!file.exists() || file.length() < 16) return null
    return try {
        val offsets = mutableListOf<Long>()
        RandomAccessFile(file, "r").use { raf ->
            var pos = 0L
            val len = raf.length()
            val header = ByteArray(8)
            while (pos + 8 <= len) {
                raf.seek(pos)
                raf.readFully(header)
                val size = ((header[0].toLong() and 0xFF) shl 24) or
                        ((header[1].toLong() and 0xFF) shl 16) or
                        ((header[2].toLong() and 0xFF) shl 8) or
                        (header[3].toLong() and 0xFF)
                val type = String(header, 4, 4, Charsets.US_ASCII)

                val boxSize = if (size == 1L) {
                    raf.readLong()
                } else if (size == 0L) {
                    len - pos
                } else {
                    size
                }

                if (type == "moof") {
                    offsets.add(pos)
                }

                if (boxSize <= 0 || boxSize > len - pos) break
                pos += boxSize
            }
        }
        if (offsets.isNotEmpty()) offsets.toLongArray() else null
    } catch (_: Throwable) {
        null
    }
}

/**
 * Scans an MPEG-TS file to extract presentation timestamps (PTS) from the beginning
 * and end of the file, returning media duration in milliseconds.
 */
fun getTsFileDurationMs(file: File): Long {
    if (!file.exists() || file.length() < 188L * 10L) return 0L
    return try {
        RandomAccessFile(file, "r").use { raf ->
            val len = raf.length()
            val bufSize = (188 * 500).coerceAtMost(len.toInt())
            val buf = ByteArray(bufSize)

            raf.seek(0L)
            val readStart = raf.read(buf)
            val firstPts = findFirstPtsInBuffer(buf, readStart) ?: return@use 0L

            val endOffset = (len - bufSize).coerceAtLeast(0L)
            raf.seek(endOffset)
            val readEnd = raf.read(buf)
            val lastPts = findLastPtsInBuffer(buf, readEnd) ?: return@use 0L

            if (lastPts > firstPts) {
                (lastPts - firstPts) / 90L
            } else {
                0L
            }
        }
    } catch (_: Throwable) {
        0L
    }
}

private fun findFirstPtsInBuffer(buf: ByteArray, length: Int): Long? {
    var i = 0
    while (i <= length - 188) {
        if (buf[i] == 0x47.toByte()) {
            val pts = extractPtsFromTsPacket(buf, i)
            if (pts != null) return pts
            i += 188
        } else {
            i++
        }
    }
    return null
}

private fun findLastPtsInBuffer(buf: ByteArray, length: Int): Long? {
    var lastPts: Long? = null
    var i = 0
    while (i <= length - 188) {
        if (buf[i] == 0x47.toByte()) {
            val pts = extractPtsFromTsPacket(buf, i)
            if (pts != null) lastPts = pts
            i += 188
        } else {
            i++
        }
    }
    return lastPts
}

private fun extractPtsFromTsPacket(buf: ByteArray, offset: Int): Long? {
    val b1 = buf[offset + 1].toInt() and 0xFF
    val pusi = (b1 and 0x40) != 0
    if (!pusi) return null

    val b3 = buf[offset + 3].toInt() and 0xFF
    val adaptFieldControl = (b3 and 0x30) shr 4
    var payloadStart = offset + 4
    if (adaptFieldControl == 2 || adaptFieldControl == 3) {
        val adaptLength = buf[offset + 4].toInt() and 0xFF
        payloadStart = offset + 5 + adaptLength
    }
    if (payloadStart + 14 > offset + 188) return null

    if (buf[payloadStart] == 0.toByte() && buf[payloadStart + 1] == 0.toByte() && buf[payloadStart + 2] == 1.toByte()) {
        val streamId = buf[payloadStart + 3].toInt() and 0xFF
        if (streamId in 0xE0..0xEF || streamId in 0xC0..0xDF) {
            val ptsFlags = (buf[payloadStart + 7].toInt() and 0xC0) shr 6
            if (ptsFlags == 2 || ptsFlags == 3) {
                val ptsOffset = payloadStart + 9
                val p0 = buf[ptsOffset].toLong() and 0xFF
                val p1 = buf[ptsOffset + 1].toLong() and 0xFF
                val p2 = buf[ptsOffset + 2].toLong() and 0xFF
                val p3 = buf[ptsOffset + 3].toLong() and 0xFF
                val p4 = buf[ptsOffset + 4].toLong() and 0xFF
                return ((p0 and 0x0E) shl 29) or ((p1 and 0xFF) shl 22) or ((p2 and 0xFE) shl 14) or ((p3 and 0xFF) shl 7) or ((p4 and 0xFE) shr 1)
            }
        }
    }
    return null
}

/**
 * Wraps an Extractor to ensure it is always seekable for local fMP4 playback.
 */
class ForcedSeekableExtractorWrapper(
    private val delegate: Extractor,
    private val durationUs: Long,
    private val fileLengthBytes: Long,
    private val moofOffsets: LongArray? = null
) : Extractor {

    override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

    override fun init(output: ExtractorOutput) {
        delegate.init(object : ExtractorOutput by output {
            override fun seekMap(originalSeekMap: SeekMap) {
                if (originalSeekMap.isSeekable && originalSeekMap.durationUs > 0) {
                    output.seekMap(originalSeekMap)
                    return
                }

                val finalDurationUs = if (originalSeekMap.durationUs > 0) {
                    originalSeekMap.durationUs
                } else if (durationUs > 0) {
                    durationUs
                } else {
                    C.TIME_UNSET
                }

                val customSeekMap = object : SeekMap {
                    override fun isSeekable(): Boolean = true
                    override fun getDurationUs(): Long = finalDurationUs

                    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
                        if (fileLengthBytes <= 0 || finalDurationUs <= 0) {
                            val pt = SeekPoint(timeUs, 0L)
                            return SeekMap.SeekPoints(pt, pt)
                        }

                        val safeTimeUs = timeUs.coerceIn(0L, finalDurationUs)
                        val fraction = safeTimeUs.toDouble() / finalDurationUs.toDouble()

                        val moofs = moofOffsets
                        val position = if (moofs != null && moofs.isNotEmpty()) {
                            val index = ((moofs.size - 1) * fraction).toInt().coerceIn(0, moofs.size - 1)
                            moofs[index]
                        } else {
                            val pos = (fraction * fileLengthBytes).toLong()
                            pos.coerceIn(0L, fileLengthBytes - 1L)
                        }

                        val pt = SeekPoint(safeTimeUs, position)
                        return SeekMap.SeekPoints(pt, pt)
                    }
                }
                output.seekMap(customSeekMap)
            }
        })
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int = delegate.read(input, seekPosition)
    override fun seek(position: Long, timeUs: Long) = delegate.seek(position, timeUs)
    override fun release() = delegate.release()
}

/**
 * Custom ExtractorsFactory for local playback that ensures all extractors produce
 * a seekable SeekMap with valid duration.
 */
@OptIn(UnstableApi::class)
class SeekableExtractorsFactory(
    private val durationUs: Long,
    private val file: File?
) : ExtractorsFactory {
    private val fileLengthBytes = file?.length() ?: 0L
    private val moofOffsets = file?.let { scanMoofOffsets(it) }

    override fun createExtractors(): Array<Extractor> {
        val baseExtractors = mutableListOf<Extractor>()
        if (moofOffsets != null) {
            baseExtractors.add(
                FragmentedMp4Extractor()
            )
        }

        val defaultFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                        DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                        DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM
            )
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)

        baseExtractors.addAll(defaultFactory.createExtractors())

        return baseExtractors.map {
            ForcedSeekableExtractorWrapper(it, durationUs, fileLengthBytes, moofOffsets)
        }.toTypedArray()
    }

    override fun createExtractors(uri: Uri, responseHeaders: MutableMap<String, MutableList<String>>): Array<Extractor> {
        return createExtractors()
    }
}
