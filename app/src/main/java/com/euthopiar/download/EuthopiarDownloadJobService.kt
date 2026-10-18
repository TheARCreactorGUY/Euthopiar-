package com.euthopiar.download

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class EuthopiarDownloadJobService : JobService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeJobCoroutines = ConcurrentHashMap<String, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        val downloadId = params.extras.getString(EXTRA_DOWNLOAD_ID) ?: return false
        val repository = DownloadRepository.getInstance(this)

        EuthopiarDownloadService.createNotificationChannel(this)
        val notification = EuthopiarDownloadService.buildNotification(
            this,
            "Transferring download...",
            "",
            0,
            true
        )
        setNotification(
            params,
            EuthopiarDownloadService.NOTIFICATION_ID,
            notification,
            JOB_END_NOTIFICATION_POLICY_REMOVE
        )

        if (!repository.isDownloading(downloadId)) {
            repository.resumeDownload(downloadId, fromJobService = true)
        }

        val currentItem = repository.downloads.value.firstOrNull { it.id == downloadId }
        if (currentItem == null || currentItem.state != DownloadState.DOWNLOADING) {
            jobFinished(params, false)
            return false
        }

        val job = serviceScope.launch {
            repository.downloads.collect { list ->
                val item = list.firstOrNull { it.id == downloadId }
                if (item == null || item.state != DownloadState.DOWNLOADING) {
                    activeJobCoroutines.remove(downloadId)
                    jobFinished(params, false)
                    cancel()
                }
            }
        }
        activeJobCoroutines[downloadId] = job
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val downloadId = params.extras.getString(EXTRA_DOWNLOAD_ID)
        if (downloadId != null) {
            activeJobCoroutines.remove(downloadId)?.cancel()
            // Only pause if the user explicitly commanded the job to stop.
            // If stopped due to UIDT quota or system migration, leave download running under ForegroundService.
            if (params.stopReason == JobParameters.STOP_REASON_USER) {
                val repository = DownloadRepository.getInstance(this)
                val currentItem = repository.downloads.value.firstOrNull { it.id == downloadId }
                if (currentItem != null && currentItem.state == DownloadState.DOWNLOADING) {
                    repository.pauseDownload(downloadId)
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        activeJobCoroutines.clear()
    }

    companion object {
        const val JOB_ID_BASE = 95000
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun scheduleUidtJob(
            context: Context,
            downloadId: String,
            totalBytes: Long? = null,
            wifiOnly: Boolean = false
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
            try {
                val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
                val jobId = JOB_ID_BASE + (downloadId.hashCode() and 0x7FFF)
                val bundle = PersistableBundle().apply {
                    putString(EXTRA_DOWNLOAD_ID, downloadId)
                }
                val builder = JobInfo.Builder(jobId, ComponentName(context, EuthopiarDownloadJobService::class.java))
                    .setUserInitiated(true)
                    .setRequiredNetworkType(if (wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(bundle)

                // Give generous estimated headroom (or UNKNOWN) so Android 14 does not terminate transfer at 100% estimate
                val estimatedBytes = if (totalBytes != null && totalBytes > 0L) {
                    (totalBytes * 3L).coerceAtLeast(1024L * 1024 * 1024)
                } else {
                    JobInfo.NETWORK_BYTES_UNKNOWN.toLong()
                }
                builder.setEstimatedNetworkBytes(estimatedBytes, 0L)

                scheduler.schedule(builder.build())
            } catch (_: Throwable) {
                // Graceful fallback to foreground service
            }
        }

        fun cancelUidtJob(context: Context, downloadId: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
            try {
                val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
                val jobId = JOB_ID_BASE + (downloadId.hashCode() and 0x7FFF)
                scheduler.cancel(jobId)
            } catch (_: Throwable) {}
        }
    }
}
