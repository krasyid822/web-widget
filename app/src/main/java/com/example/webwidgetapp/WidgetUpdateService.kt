package com.example.webwidgetapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class WidgetUpdateService : Service() {

    companion object {
        const val ACTION_START = "com.example.webwidgetapp.START_AUTO_REFRESH"
        const val ACTION_STOP = "com.example.webwidgetapp.STOP_AUTO_REFRESH"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val DEFAULT_INTERVAL_MS = 1000L
        private const val CHANNEL_ID = "widget_refresh_channel"
        private const val NOTIF_ID = 1

        fun start(context: Context, intervalMs: Long = DEFAULT_INTERVAL_MS) {
            val intent = Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, WidgetUpdateService::class.java).apply {
                action = ACTION_STOP
            })
        }

        fun isRunning(context: Context): Boolean {
            return runningInstance != null
        }

        private var runningInstance: WidgetUpdateService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var intervalMs = DEFAULT_INTERVAL_MS
    private var isRefreshing = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            val mgr = AppWidgetManager.getInstance(this@WidgetUpdateService)
            val ids = mgr.getAppWidgetIds(
                ComponentName(this@WidgetUpdateService, WebWidgetProvider::class.java)
            )

            if (ids.isEmpty()) {
                // No widgets, schedule next check anyway
                handler.postDelayed(this, intervalMs)
                return
            }

            if (!isRefreshing) {
                isRefreshing = true
                WebRenderer.updateWidget(this@WidgetUpdateService, mgr, ids) {
                    // onDone: called when all widgets finish rendering
                    isRefreshing = false
                    handler.postDelayed(this, intervalMs)
                }
            } else {
                // Previous render still running, retry after interval
                handler.postDelayed(this, intervalMs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        runningInstance = this
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Auto-refresh aktif"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, DEFAULT_INTERVAL_MS)
                    .coerceAtLeast(500L) // minimum 500ms
                handler.removeCallbacks(refreshRunnable)
                handler.post(refreshRunnable)
                updateNotification("Auto-refresh setiap ${intervalMs}ms")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
        isRefreshing = false
        runningInstance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web Widget Auto-Refresh",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga widget web selalu diperbarui"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Web Widget")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification(text))
    }
}
