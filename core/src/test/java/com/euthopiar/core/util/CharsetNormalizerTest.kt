package com.euthopiar.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class CharsetNormalizerTest {

    @Test
    fun testUtf8Bom() {
        val original = "Hello World with UTF-8 BOM"
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + original.toByteArray(StandardCharsets.UTF_8)
        val decoded = CharsetNormalizer.decodeBytes(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testUtf16LeBom() {
        val original = "Subtitles in Japanese: 日本語"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + original.toByteArray(StandardCharsets.UTF_16LE)
        val decoded = CharsetNormalizer.decodeBytes(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testUtf16BeBom() {
        val original = "Subtitles in Arabic: مرحبا"
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + original.toByteArray(StandardCharsets.UTF_16BE)
        val decoded = CharsetNormalizer.decodeBytes(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testStandardUtf8() {
        val original = "1\n00:00:01,000 --> 00:00:03,000\n¡Hola! ¿Cómo estás?"
        val bytes = original.toByteArray(StandardCharsets.UTF_8)
        val decoded = CharsetNormalizer.decodeBytes(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun testWindows1252Fallback() {
        // Encode characters that are valid in Windows-1252 but invalid sequences in UTF-8
        val original = "Café Crème, Señor Niño, Über"
        val win1252Bytes = original.toByteArray(Charset.forName("Windows-1252"))
        val decoded = CharsetNormalizer.decodeBytes(win1252Bytes)
        assertEquals(original, decoded)
    }
}
