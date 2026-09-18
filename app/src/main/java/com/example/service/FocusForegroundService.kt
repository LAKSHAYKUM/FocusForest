package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.FocusForestApp
import com.example.domain.model.SessionState
import com.example.notifications.FocusNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FocusForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var notificationManager: FocusNotificationManager
    private var observeJob: Job? = null

    companion object {
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        const val EXTRA_IS_PLACEMENT = "extra_is_placement"

        private val _serviceActions = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val serviceActions = _serviceActions.asSharedFlow()

        var isServiceRunning = false
            private set

        fun startService(context: Context, remainingSeconds: Long, isPlacement: Boolean) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_IS_PLACEMENT, isPlacement)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private var isPlacementMode = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = FocusNotificationManager(this)
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                FocusNotificationManager.ACTION_PAUSE -> {
                    serviceScope.launch { _serviceActions.emit("PAUSE") }
                }
                FocusNotificationManager.ACTION_RESUME -> {
                    serviceScope.launch { _serviceActions.emit("RESUME") }
                }
                FocusNotificationManager.ACTION_STOP -> {
                    serviceScope.launch { _serviceActions.emit("STOP") }
                    stopSelf()
                }
                else -> {
                    val initialSeconds = it.getLongExtra(EXTRA_REMAINING_SECONDS, 25 * 60L)
                    isPlacementMode = it.getBooleanExtra(EXTRA_IS_PLACEMENT, false)

                    val notification = notificationManager.buildFocusNotification(
                        formatTime(initialSeconds),
                        "Your tree is growing.",
                        isPaused = false,
                        isPlacementMode = isPlacementMode
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        } else {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                        }
                        ServiceCompat.startForeground(
                            this,
                            FocusNotificationManager.NOTIFICATION_ID,
                            notification,
                            serviceType
                        )
                    } else {
                        startForeground(FocusNotificationManager.NOTIFICATION_ID, notification)
                    }

                    observeTimerSnapshot()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun observeTimerSnapshot() {
        observeJob?.cancel()
        val app = applicationContext as? FocusForestApp ?: return
        val timerEngine = app.timerEngine

        observeJob = serviceScope.launch {
            timerEngine.snapshot.collectLatest { snap ->
                when (snap.sessionState) {
                    SessionState.ACTIVE -> {
                        notificationManager.updateNotification(
                            formatTime(snap.remainingSeconds),
                            "Your tree is growing.",
                            isPaused = false,
                            isPlacementMode = isPlacementMode
                        )
                    }
                    SessionState.PAUSED -> {
                        notificationManager.updateNotification(
                            formatTime(snap.remainingSeconds),
                            "Focus paused",
                            isPaused = true,
                            isPlacementMode = isPlacementMode
                        )
                    }
                    SessionState.MOVED_WARNING -> {
                        notificationManager.updateNotification(
                            formatTime(snap.remainingSeconds),
                            "Phone moved! Return to desk",
                            isPaused = true,
                            isPlacementMode = isPlacementMode
                        )
                    }
                    SessionState.RESTORED -> {
                        notificationManager.updateNotification(
                            formatTime(snap.remainingSeconds),
                            "Position restored! Ready to resume",
                            isPaused = true,
                            isPlacementMode = isPlacementMode
                        )
                    }
                    SessionState.COMPLETED, SessionState.INTERRUPTED, SessionState.IDLE -> {
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val m = (seconds.coerceAtLeast(0L)) / 60
        val s = (seconds.coerceAtLeast(0L)) % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        observeJob?.cancel()
        serviceScope.cancel()
        notificationManager.cancelNotification()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
