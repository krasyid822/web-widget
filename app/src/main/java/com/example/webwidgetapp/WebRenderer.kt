package com.example.webwidgetapp

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews

object WebRenderer {
    private var webView: WebView? = null

    fun render(context: Context, url: String, width: Int = 800, height: Int = 600, callback: (Bitmap?) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            try {
                val appCtx = context.applicationContext
                val web = webView ?: WebView(appCtx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setGeolocationEnabled(true)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                }
                webView = web

                web.layout(0, 0, width, height)

                web.webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: android.webkit.GeolocationPermissions.Callback?
                    ) {
                        callback?.invoke(origin, true, true)
                    }
                }

                web.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                web.draw(canvas)
                                callback(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(null)
                            }
                        }, 1200)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        callback(null)
                    }
                }

                web.loadUrl(url)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }

    private fun getDimensionsForClass(className: String): Pair<Int, Int> {
        return when {
            className.contains("1x1") -> Pair(300, 300)
            className.contains("2x1") -> Pair(600, 300)
            className.contains("2x2") -> Pair(600, 600)
            className.contains("3x2") -> Pair(800, 500)
            className.contains("3x3") -> Pair(800, 800)
            className.contains("4x1") -> Pair(1000, 250)
            className.contains("4x2") -> Pair(1000, 500)
            className.contains("4x3") -> Pair(1000, 750)
            className.contains("4x4") -> Pair(1000, 1000)
            className.contains("5x5") -> Pair(1200, 1200)
            else -> Pair(800, 600)
        }
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE)
        
        for (appWidgetId in appWidgetIds) {
            var url = prefs.getString("widget_url_$appWidgetId", "")
            if (url.isNullOrBlank()) {
                url = prefs.getString("widget_url", "https://google.com") ?: "https://google.com"
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
            val className = info?.provider?.className ?: ""
            val (width, height) = getDimensionsForClass(className)

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val refreshIntent = WebWidgetProvider.getRefreshIntent(context, intArrayOf(appWidgetId), className)
            views.setOnClickPendingIntent(R.id.widget_image, refreshIntent)

            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)

            val finalUrl = url
            render(context, finalUrl, width, height) { bitmap ->
                Handler(Looper.getMainLooper()).post {
                    val updatedViews = RemoteViews(context.packageName, R.layout.widget_layout)
                    if (bitmap != null) {
                        updatedViews.setImageViewBitmap(R.id.widget_image, bitmap)
                    }
                    updatedViews.setOnClickPendingIntent(R.id.widget_image, refreshIntent)
                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                }
            }
        }
    }
}
