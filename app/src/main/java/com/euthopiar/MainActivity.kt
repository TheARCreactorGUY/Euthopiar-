package com.euthopiar

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import com.euthopiar.core.registry.PluginRegistry
import com.euthopiar.ui.navigation.AppNavHost
import com.euthopiar.ui.player.PipController
import com.euthopiar.ui.splash.CinematicSplashScreen
import com.euthopiar.ui.theme.EuthopiarTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.euthopiar.PIP_PLAY_PAUSE"
        const val ACTION_PIP_REWIND = "com.euthopiar.PIP_REWIND"
        const val ACTION_PIP_FAST_FORWARD = "com.euthopiar.PIP_FAST_FORWARD"

        private var hasShownSplashInProcess: Boolean = false
        private var pendingCredentialCallback: ((Boolean) -> Unit)? = null

        fun launchDeviceCredential(activity: Activity, intent: Intent, onResult: (Boolean) -> Unit) {
            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                pendingCredentialCallback = onResult
                mainActivity.deviceCredentialLauncher.launch(intent)
            } else {
                onResult(false)
            }
        }
    }

    private val deviceCredentialLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isSuccess = result.resultCode == Activity.RESULT_OK
            val cb = pendingCredentialCallback
            pendingCredentialCallback = null
            cb?.invoke(isSuccess)
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission granted or dismissed
        }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    PipController.onTogglePlayPause?.invoke()
                    val newIsPlaying = PipController.isPlaying()
                    updatePipActions(newIsPlaying)
                }
                ACTION_PIP_REWIND -> {
                    val delta = PipController.seekIntervalMs
                    PipController.onSeekRelative?.invoke(-delta)
                }
                ACTION_PIP_FAST_FORWARD -> {
                    val delta = PipController.seekIntervalMs
                    PipController.onSeekRelative?.invoke(delta)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pipSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        PipController.setPipSupported(pipSupported)
        PipController.onRequestPip = { enterPipMode() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Register PiP action receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_REWIND)
            addAction(ACTION_PIP_FAST_FORWARD)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }

        val isWarmStart = savedInstanceState != null || hasShownSplashInProcess
        setContent {
            EuthopiarTheme {
                var showSplash by rememberSaveable { mutableStateOf(!isWarmStart) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppNavHost(registry = PluginRegistry.Default)

                    if (showSplash) {
                        CinematicSplashScreen(
                            onAnimationFinished = {
                                hasShownSplashInProcess = true
                                showSplash = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PipController.onRequestPip = { enterPipMode() }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    val params = buildPipParams(PipController.isPlaying())
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to enter PiP: ${e.message}", e)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PipController.isPlaying()) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipController.setInPipMode(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            updatePipActions(PipController.isPlaying())
        }
    }

    fun updatePipActions(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    setPictureInPictureParams(buildPipParams(isPlaying))
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to update PiP params: ${e.message}", e)
                }
            }
        }
    }

    fun disableAutoPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build()
                )
            } catch (_: Exception) {}
        }
    }

    private fun buildPipParams(isPlaying: Boolean): PictureInPictureParams {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw UnsupportedOperationException("PiP requires Android 8.0+")
        }

        val actions = mutableListOf<RemoteAction>()

        // 1. Rewind 10s
        val rewindIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_PIP_REWIND).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_rew),
                "Rewind",
                "Rewind 10 seconds",
                rewindIntent
            )
        )

        // 2. Play / Pause
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, playPauseIcon),
                playPauseTitle,
                playPauseTitle,
                playPauseIntent
            )
        )

        // 3. Fast Forward 10s
        val ffIntent = PendingIntent.getBroadcast(
            this,
            3,
            Intent(ACTION_PIP_FAST_FORWARD).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                "Fast Forward",
                "Forward 10 seconds",
                ffIntent
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
        PipController.onRequestPip = null
        PipController.clearCallbacks()
    }
}
