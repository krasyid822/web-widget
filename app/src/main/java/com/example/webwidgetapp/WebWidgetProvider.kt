package com.example.webwidgetapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

open class WebWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WebRenderer.updateWidget(context, appWidgetManager, appWidgetIds)
        WidgetUpdateService.start(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            var appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (appWidgetIds == null || appWidgetIds.isEmpty()) {
                val componentName = ComponentName(context, this::class.java)
                appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            }
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                WebRenderer.updateWidget(context, appWidgetManager, appWidgetIds)
            }
            WidgetUpdateService.start(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.webwidgetapp.ACTION_REFRESH"

        fun getRefreshIntent(context: Context, appWidgetIds: IntArray, className: String): PendingIntent {
            val clazz = try {
                Class.forName(className)
            } catch (e: Exception) {
                WebWidgetProvider::class.java
            }
            val intent = Intent(context, clazz).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, appWidgetIds.contentHashCode(), intent, flags)
        }
    }
}

class WebWidgetProvider1x1 : WebWidgetProvider()
class WebWidgetProvider2x1 : WebWidgetProvider()
class WebWidgetProvider2x2 : WebWidgetProvider()
class WebWidgetProvider3x2 : WebWidgetProvider()
class WebWidgetProvider3x3 : WebWidgetProvider()
class WebWidgetProvider4x1 : WebWidgetProvider()
class WebWidgetProvider4x2 : WebWidgetProvider()
class WebWidgetProvider4x3 : WebWidgetProvider()
class WebWidgetProvider4x4 : WebWidgetProvider()
class WebWidgetProvider5x5 : WebWidgetProvider()
