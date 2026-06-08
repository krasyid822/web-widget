package com.example.webwidgetapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import java.util.concurrent.atomic.AtomicBoolean

object WebRenderer {
    // Persistent WebViews per widget – page loaded once, screenshots taken repeatedly
    private val webViews = mutableMapOf<Int, WebView>()
    // Track whether a given widget's page has finished loading at least once
    private val pageReady = mutableMapOf<Int, AtomicBoolean>()
    // Track the last render size to detect if we need to relayout
    private val lastRenderSize = mutableMapOf<Int, Pair<Int, Int>>()

    private const val PREF_ZOOM = "page_zoom_percent"
    private const val DEFAULT_ZOOM = 100

    fun getZoomPercent(context: Context): Int =
        context.getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE)
            .getInt(PREF_ZOOM, DEFAULT_ZOOM)

    fun setZoomPercent(context: Context, percent: Int) {
        context.getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE)
            .edit().putInt(PREF_ZOOM, percent).apply()
        // Force relayout on next screenshot by clearing cached render sizes
        Handler(Looper.getMainLooper()).post {
            lastRenderSize.clear()
        }
    }

    fun getUrlForWidget(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE)
        var url = prefs.getString("widget_url_$appWidgetId", null)
            ?: prefs.getString("widget_url", "https://google.com")
            ?: "https://google.com"
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    /**
     * Takes a screenshot of the WebView scaled to [outputWidth] × [outputHeight].
     *
     * Zoom is implemented by changing the WebView viewport size, not CSS zoom:
     *  - zoom 200% → WebView rendered at half viewport → scale up → larger content, no crop
     *  - zoom  50% → WebView rendered at double viewport → scale down → more content, no crop
     *
     * This ensures the full widget frame is always filled with no clipping.
     */
    fun screenshot(
        context: Context,
        appWidgetId: Int,
        url: String,
        outputWidth: Int,
        outputHeight: Int,
        callback: (Bitmap?) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                val appCtx = context.applicationContext
                val ready = pageReady.getOrPut(appWidgetId) { AtomicBoolean(false) }

                // Compute WebView render size based on zoom
                val zoomScale = getZoomPercent(context) / 100.0
                val renderW = (outputWidth / zoomScale).toInt().coerceIn(200, 3000)
                val renderH = (outputHeight / zoomScale).toInt().coerceIn(150, 2000)

                val web = webViews[appWidgetId] ?: WebView(appCtx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setGeolocationEnabled(true)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: android.webkit.GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!ready.get()) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    ready.set(true)
                                }, 1200)
                            }
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                        }
                    }
                }.also { webViews[appWidgetId] = it }

                // Relayout WebView if render size changed (zoom or widget size changed)
                val prevSize = lastRenderSize[appWidgetId]
                if (prevSize == null || prevSize.first != renderW || prevSize.second != renderH) {
                    web.layout(0, 0, renderW, renderH)
                    lastRenderSize[appWidgetId] = Pair(renderW, renderH)
                    // After relayout, page needs time to reflow before we screenshot
                    if (ready.get()) {
                        ready.set(false)
                        web.loadUrl(web.url ?: url)
                    }
                }

                if (!ready.get()) {
                    val currentUrl = web.url ?: ""
                    if (currentUrl.isEmpty() || currentUrl == "about:blank") {
                        web.loadUrl(url)
                    }
                    callback(null)
                    return@post
                }

                // Capture raw bitmap at render size (viewport size)
                val rawBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                val rawCanvas = Canvas(rawBitmap)
                web.draw(rawCanvas)

                // Scale to output size (fills widget frame completely, no crop)
                if (renderW == outputWidth && renderH == outputHeight) {
                    callback(rawBitmap)
                } else {
                    val scaledBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    val scaleCanvas = Canvas(scaledBitmap)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    val scaleX = outputWidth.toFloat() / renderW
                    val scaleY = outputHeight.toFloat() / renderH
                    scaleCanvas.scale(scaleX, scaleY)
                    scaleCanvas.drawBitmap(rawBitmap, 0f, 0f, paint)
                    rawBitmap.recycle()
                    callback(scaledBitmap)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }

    /**
     * Force reload the page for a widget (e.g. after URL change or manual refresh tap).
     */
    fun reloadPage(appWidgetId: Int, url: String) {
        Handler(Looper.getMainLooper()).post {
            pageReady[appWidgetId]?.set(false)
            lastRenderSize.remove(appWidgetId)
            webViews[appWidgetId]?.loadUrl(url)
        }
    }

    /**
     * Update widget(s): takes a screenshot and pushes to RemoteViews.
     * onDone is called when all widgets are processed (for chaining in auto-refresh).
     */
    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        onDone: (() -> Unit)? = null
    ) {
        val remaining = java.util.concurrent.atomic.AtomicInteger(appWidgetIds.size)

        for (appWidgetId in appWidgetIds) {
            val url = getUrlForWidget(context, appWidgetId)

            val options: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            // Output bitmap size at 6x dp-to-px for crisp rendering
            val outputW = (minW * 6.0).toInt().coerceAtLeast(480)
            val outputH = (minH * 6.0).toInt().coerceAtLeast(360)

            screenshot(context, appWidgetId, url, outputW, outputH) { bitmap ->
                Handler(Looper.getMainLooper()).post {
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_image, bitmap)
                    }
                    // Tap widget = manual refresh
                    views.setOnClickPendingIntent(R.id.widget_image, getRefreshIntent(context, appWidgetId))
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                    if (remaining.decrementAndGet() == 0) {
                        onDone?.invoke()
                    }
                }
            }
        }

        if (appWidgetIds.isEmpty()) {
            onDone?.invoke()
        }
    }

    private fun getRefreshIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, WebWidgetProvider::class.java).apply {
            action = WebWidgetProvider.ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, appWidgetId, intent, flags)
    }
}
