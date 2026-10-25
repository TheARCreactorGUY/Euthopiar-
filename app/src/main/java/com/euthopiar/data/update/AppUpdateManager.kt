package com.euthopiar.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import com.euthopiar.core.network.DohDns
import com.euthopiar.data.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AppUpdateConfig {
    const val GITHUB_OWNER = "TheARCreactorGUY"
    const val GITHUB_REPO = "Euthopiar-"
    const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val GITHUB_RELEASES_URL = "$GITHUB_REPO_URL/releases"
    const val GITHUB_LATEST_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val GITHUB_ALL_RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
}

@Immutable
data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long = 0L,
    val isPrerelease: Boolean = false
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : UpdateCheckState
    data class UpToDate(val currentVersion: String, val latestRelease: ReleaseInfo? = null) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

sealed interface ApkDownloadState {
    data object Idle : ApkDownloadState
    data class Downloading(val progressFraction: Float, val downloadedBytes: Long, val totalBytes: Long) : ApkDownloadState
    data class ReadyToInstall(val apkFile: File) : ApkDownloadState
    data class Error(val message: String) : ApkDownloadState
}

fun isRemoteVersionNewer(currentVersion: String, remoteTag: String): Boolean {
    val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
    val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")

    val currentCore = cleanCurrent.substringBefore("-").substringBefore("+")
    val remoteCore = cleanRemote.substringBefore("-").substringBefore("+")

    val currentParts = currentCore.split(".").mapNotNull { it.toIntOrNull() }
    val remoteParts = remoteCore.split(".").mapNotNull { it.toIntOrNull() }

    val maxLen = maxOf(currentParts.size, remoteParts.size)
    for (i in 0 until maxLen) {
        val curr = currentParts.getOrElse(i) { 0 }
        val rem = remoteParts.getOrElse(i) { 0 }
        if (rem > curr) return true
        if (rem < curr) return false
    }

    val currentHasPreRelease = cleanCurrent.contains("-") && !cleanCurrent.contains("-stable", ignoreCase = true)
    val remoteHasPreRelease = cleanRemote.contains("-") && !cleanRemote.contains("-stable", ignoreCase = true)

    if (currentHasPreRelease && !remoteHasPreRelease) {
        return true
    }

    return false
}

class AppUpdateManager private constructor(private val context: Context) {

    private val preferencesRepository = PreferencesRepository.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _downloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val downloadState: StateFlow<ApkDownloadState> = _downloadState.asStateFlow()

    private val _bannerDismissedForSession = MutableStateFlow(false)
    val bannerDismissedForSession: StateFlow<Boolean> = _bannerDismissedForSession.asStateFlow()

    private val _autoPopupShownForSession = MutableStateFlow(false)
    val autoPopupShownForSession: StateFlow<Boolean> = _autoPopupShownForSession.asStateFlow()

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(forceManual: Boolean = false) {
        _updateState.value = UpdateCheckState.Checking
        val currentVersion = getCurrentVersionName()

        withContext(Dispatchers.IO) {
            try {
                val includePrereleases = preferencesRepository.includePrereleases.first()
                val client = DohDns.sharedBaseClient

                var releaseJson: JSONObject? = null

                if (!includePrereleases) {
                    val request = Request.Builder()
                        .url(AppUpdateConfig.GITHUB_LATEST_API_URL)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Euthopiar-App")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrBlank()) {
                                releaseJson = JSONObject(body)
                            }
                        }
                    }
                }

                // If latest release was not found (404, or prereleases requested), query all releases
                if (releaseJson == null) {
                    val request = Request.Builder()
                        .url(AppUpdateConfig.GITHUB_ALL_RELEASES_API_URL)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Euthopiar-App")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrBlank()) {
                                val array = JSONArray(body)
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    if (obj.optBoolean("draft", false)) continue
                                    val isPre = obj.optBoolean("prerelease", false)
                                    if (includePrereleases || !isPre) {
                                        releaseJson = obj
                                        break
                                    }
                                }
                            }
                        }
                    }
                }

                preferencesRepository.setLastUpdateCheckTimestamp(System.currentTimeMillis())

                if (releaseJson == null) {
                    _updateState.value = UpdateCheckState.UpToDate(currentVersion, null)
                    return@withContext
                }

                val release = parseReleaseInfo(releaseJson)
                if (isRemoteVersionNewer(currentVersion, release.tagName)) {
                    _updateState.value = UpdateCheckState.UpdateAvailable(release, currentVersion)
                } else {
                    _updateState.value = UpdateCheckState.UpToDate(currentVersion, release)
                }
            } catch (e: Exception) {
                if (forceManual) {
                    _updateState.value = UpdateCheckState.Error(e.message ?: "Failed to check for updates")
                } else {
                    _updateState.value = UpdateCheckState.UpToDate(currentVersion, null)
                }
            }
        }
    }

    private fun parseReleaseInfo(json: JSONObject): ReleaseInfo {
        val tagName = json.optString("tag_name", "").ifBlank { "v1.0.0" }
        val versionName = tagName.removePrefix("v").removePrefix("V")
        val releaseTitle = json.optString("name", "").ifBlank { tagName }
        val changelog = json.optString("body", "").ifBlank { "Bug fixes and performance improvements." }
        val rawPublishedAt = json.optString("published_at", "")
        val publishedAt = formatPublishedDate(rawPublishedAt)
        val htmlUrl = json.optString("html_url", AppUpdateConfig.GITHUB_RELEASES_URL)
        val isPrerelease = json.optBoolean("prerelease", false)

        var apkDownloadUrl: String? = null
        var apkSizeBytes = 0L

        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url", null)
                    apkSizeBytes = asset.optLong("size", 0L)
                    break
                }
            }
        }

        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            releaseTitle = releaseTitle,
            changelog = changelog,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkDownloadUrl,
            apkSizeBytes = apkSizeBytes,
            isPrerelease = isPrerelease
        )
    }

    private fun formatPublishedDate(isoStr: String): String {
        if (isoStr.isBlank()) return "Recently"
        return try {
            val instant = Instant.parse(isoStr)
            val zdt = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US).format(zdt)
        } catch (_: Exception) {
            isoStr.substringBefore("T")
        }
    }

    fun downloadAndInstallUpdate(release: ReleaseInfo) {
        val downloadUrl = release.apkDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            openInBrowser(release.htmlUrl)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                _downloadState.value = ApkDownloadState.Downloading(0f, 0L, release.apkSizeBytes)
                val updatesDir = File(context.cacheDir, "updates")
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }

                val apkFile = File(updatesDir, "Euthopiar-${release.versionName}.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val client = DohDns.sharedBaseClient
                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Euthopiar-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _downloadState.value = ApkDownloadState.Error("HTTP download error: ${response.code}")
                        return@launch
                    }

                    val body = response.body ?: run {
                        _downloadState.value = ApkDownloadState.Error("Empty response body from GitHub")
                        return@launch
                    }

                    val totalBytes = if (body.contentLength() > 0) body.contentLength() else release.apkSizeBytes
                    var downloadedBytes = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            var lastProgressUpdate = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdate > 100 || downloadedBytes == totalBytes) {
                                    lastProgressUpdate = now
                                    val fraction = if (totalBytes > 0) {
                                        (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    _downloadState.value = ApkDownloadState.Downloading(fraction, downloadedBytes, totalBytes)
                                }
                            }
                            output.flush()
                        }
                    }
                }

                _downloadState.value = ApkDownloadState.ReadyToInstall(apkFile)
                withContext(Dispatchers.Main) {
                    triggerInstallApk(apkFile, release.htmlUrl)
                }
            } catch (e: Exception) {
                _downloadState.value = ApkDownloadState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun triggerInstallApk(apkFile: File, fallbackUrl: String) {
        try {
            if (!apkFile.exists()) {
                openInBrowser(fallbackUrl)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (_: Exception) {
            openInBrowser(fallbackUrl)
        }
    }

    fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun dismissBannerForSession() {
        _bannerDismissedForSession.value = true
    }

    fun markAutoPopupShownForSession() {
        _autoPopupShownForSession.value = true
    }

    fun skipVersion(versionName: String) {
        scope.launch {
            preferencesRepository.setSkippedUpdateVersion(versionName)
        }
    }

    fun resetDownloadState() {
        _downloadState.value = ApkDownloadState.Idle
    }

    companion object {
        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
