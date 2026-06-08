package com.example.webwidgetapp

import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.example.webwidgetapp.R

class WebViewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val url = intent.getStringExtra(EXTRA_URL) ?: "https://google.com"

        val titleView = findViewById<android.widget.TextView>(R.id.webview_title)
        val backBtn = findViewById<android.widget.TextView>(R.id.webview_back)
        val refreshBtn = findViewById<android.widget.TextView>(R.id.webview_refresh)

        webView = findViewById(R.id.full_webview)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setGeolocationEnabled(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                // Auto-grant geolocation same as widget WebView
                callback?.invoke(origin, true, false)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                titleView.text = title ?: url
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                titleView.text = view?.title ?: url
            }
        }

        backBtn.setOnClickListener { finish() }
        refreshBtn.setOnClickListener { webView.reload() }

        webView.loadUrl(url)
        titleView.text = url
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
