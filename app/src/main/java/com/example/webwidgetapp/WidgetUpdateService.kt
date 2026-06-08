package com.example.webwidgetapp

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class WidgetUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isScreenOn = true
    private val runnables = mutableMapOf<Int, Runnable>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    startLoops()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopLoops()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        startLoops()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLoops()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLoops() {
        stopLoops()
        if (!isScreenOn) return

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providers = listOf(
            com.example.webwidgetapp.WebWidgetProvider1x1::class.java,
            com.example.webwidgetapp.WebWidgetProvider2x1::class.java,
            com.example.webwidgetapp.WebWidgetProvider2x2::class.java,
            com.example.webwidgetapp.WebWidgetProvider3x2::class.java,
            com.example.webwidgetapp.WebWidgetProvider3x3::class.java,
            com.example.webwidgetapp.WebWidgetProvider4x1::class.java,
            com.example.webwidgetapp.WebWidgetProvider4x2::class.java,
            com.example.webwidgetapp.WebWidgetProvider4x3::class.java,
            com.example.webwidgetapp.WebWidgetProvider4x4::class.java,
            com.example.webwidgetapp.WebWidgetProvider5x5::class.java
        )
        val appWidgetIds = providers.flatMap { clazz ->
            appWidgetManager.getAppWidgetIds(ComponentName(this, clazz)).toList()
        }
        val prefs = getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE)

        var hasAnyActiveLoop = false

        for (appWidgetId in appWidgetIds) {
            val intervalSec = prefs.getInt("widget_interval_$appWidgetId", 0)
            if (intervalSec >= 1) {
                hasAnyActiveLoop = true
                val intervalMs = intervalSec * 1000L
                val runnable = object : Runnable {
                    override fun run() {
                        if (!isScreenOn) return
                        WebRenderer.updateWidget(this@WidgetUpdateService, appWidgetManager, intArrayOf(appWidgetId))
                        handler.postDelayed(this, intervalMs)
                    }
                }
                runnables[appWidgetId] = runnable
                handler.postDelayed(runnable, intervalMs)
            }
        }

        if (!hasAnyActiveLoop) {
            stopSelf()
        }
    }

    private fun stopLoops() {
        for (runnable in runnables.values) {
            handler.removeCallbacks(runnable)
        }
        runnables.clear()
    }

    override fun onDestroy() {
        stopLoops()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore background start exceptions
            }
        }
    }
}
