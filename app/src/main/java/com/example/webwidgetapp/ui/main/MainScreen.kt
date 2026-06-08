package com.example.webwidgetapp.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.webwidgetapp.WebRenderer
import com.example.webwidgetapp.WebWidgetProvider
import com.example.webwidgetapp.WidgetUpdateService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appWidgetId: Int,
    onConfigFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE) }
    val isConfigMode = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val activeWidgetIds = remember(refreshTrigger) {
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
        val allIds = providers.flatMap { clazz ->
            appWidgetManager.getAppWidgetIds(ComponentName(context, clazz)).toList()
        }
        allIds.filter { id ->
            prefs.contains("widget_url_$id")
        }.toIntArray()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isConfigMode) "Configure Widget #$appWidgetId" else "Active Web Widgets",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        if (isConfigMode) {
            val sizeLabel = remember(appWidgetId) {
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                val className = info?.provider?.className ?: ""
                when {
                    className.contains("1x1") -> "1x1"
                    className.contains("2x1") -> "2x1"
                    className.contains("2x2") -> "2x2"
                    className.contains("3x2") -> "3x2"
                    className.contains("3x3") -> "3x3"
                    className.contains("4x1") -> "4x1"
                    className.contains("4x2") -> "4x2"
                    className.contains("4x3") -> "4x3"
                    className.contains("4x4") -> "4x4"
                    className.contains("5x5") -> "5x5"
                    else -> ""
                }
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (sizeLabel.isNotEmpty()) {
                    Text(
                        text = "Widget Size: $sizeLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                WidgetConfigForm(
                    appWidgetId = appWidgetId,
                    prefs = prefs,
                    onSave = {
                        WebRenderer.updateWidget(context, appWidgetManager, intArrayOf(appWidgetId))
                        WidgetUpdateService.start(context)
                        onConfigFinished()
                        refreshTrigger++
                    }
                )
            }
        } else {
            if (activeWidgetIds.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Active Widgets",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Long-press on your home screen, tap 'Widgets', and drag 'Web Widget' to your home screen to create a new live web widget.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Manage your launcher widgets below:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(activeWidgetIds.toList()) { id ->
                        var isExpanded by remember { mutableStateOf(false) }
                        val url = prefs.getString("widget_url_$id", "https://google.com") ?: "https://google.com"
                        val sizeLabel = remember(id) {
                            val info = appWidgetManager.getAppWidgetInfo(id)
                            val className = info?.provider?.className ?: ""
                            when {
                                className.contains("1x1") -> "1x1"
                                className.contains("2x1") -> "2x1"
                                className.contains("2x2") -> "2x2"
                                className.contains("3x2") -> "3x2"
                                className.contains("3x3") -> "3x3"
                                className.contains("4x1") -> "4x1"
                                className.contains("4x2") -> "4x2"
                                className.contains("4x3") -> "4x3"
                                className.contains("4x4") -> "4x4"
                                className.contains("5x5") -> "5x5"
                                else -> "Unknown Size"
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Widget #$id ($sizeLabel)", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (isExpanded) "▲ Close" else "▼ Edit",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        HorizontalDivider()
                                        WidgetConfigForm(
                                            appWidgetId = id,
                                            prefs = prefs,
                                            onSave = {
                                                WebRenderer.updateWidget(context, appWidgetManager, intArrayOf(id))
                                                WidgetUpdateService.start(context)
                                                isExpanded = false
                                                refreshTrigger++
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetConfigForm(
    appWidgetId: Int,
    prefs: android.content.SharedPreferences,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    var urlText by remember { mutableStateOf(prefs.getString("widget_url_$appWidgetId", "https://google.com") ?: "https://google.com") }
    var intervalText by remember { mutableStateOf(prefs.getInt("widget_interval_$appWidgetId", 0).toString()) }

    var previewUrl by remember { mutableStateOf(urlText) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    val sizeLabel = remember(appWidgetId) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        info?.provider?.className ?: ""
    }

    val dims = remember(sizeLabel) {
        when {
            sizeLabel.contains("1x1") -> Pair(300, 300)
            sizeLabel.contains("2x1") -> Pair(600, 300)
            sizeLabel.contains("2x2") -> Pair(600, 600)
            sizeLabel.contains("3x2") -> Pair(800, 500)
            sizeLabel.contains("3x3") -> Pair(800, 800)
            sizeLabel.contains("4x1") -> Pair(1000, 250)
            sizeLabel.contains("4x2") -> Pair(1000, 500)
            sizeLabel.contains("4x3") -> Pair(1000, 750)
            sizeLabel.contains("4x4") -> Pair(1000, 1000)
            sizeLabel.contains("5x5") -> Pair(1200, 1200)
            else -> Pair(800, 600)
        }
    }
    val ratio = dims.first.toFloat() / dims.second.toFloat()
    val labelText = remember(dims) {
        val presetName = when {
            sizeLabel.contains("1x1") -> "1x1"
            sizeLabel.contains("2x1") -> "2x1"
            sizeLabel.contains("2x2") -> "2x2"
            sizeLabel.contains("3x2") -> "3x2"
            sizeLabel.contains("3x3") -> "3x3"
            sizeLabel.contains("4x1") -> "4x1"
            sizeLabel.contains("4x2") -> "4x2"
            sizeLabel.contains("4x3") -> "4x3"
            sizeLabel.contains("4x4") -> "4x4"
            sizeLabel.contains("5x5") -> "5x5"
            else -> "Default (4x3)"
        }
        "Live Preview (Aspect Ratio preset $presetName - ${dims.first}x${dims.second} px):"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Website URL") },
            placeholder = { Text("e.g. https://google.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = intervalText,
            onValueChange = { intervalText = it },
            label = { Text("Auto-refresh Interval (seconds)") },
            placeholder = { Text("0 to disable, min 1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    var url = urlText.trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    previewUrl = url
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Load Preview")
            }

            Button(
                onClick = {
                    webViewRef?.apply {
                        clearCache(true)
                        clearHistory()
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.WebStorage.getInstance().deleteAllData()
                        var url = urlText.trim()
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://$url"
                        }
                        previewUrl = url
                        loadUrl(previewUrl)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear & Reload")
            }
        }

        if (previewUrl.isNotBlank()) {
            Text(
                text = labelText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setGeolocationEnabled(true)
                            
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?,
                                    callback: android.webkit.GeolocationPermissions.Callback?
                                ) {
                                    callback?.invoke(origin, true, true)
                                }
                            }
                            
                            webViewClient = android.webkit.WebViewClient()
                            webViewRef = this
                            loadUrl(previewUrl)
                        }
                    },
                    update = { web ->
                        if (web.url != previewUrl) {
                            web.loadUrl(previewUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Button(
            onClick = {
                val interval = intervalText.toIntOrNull() ?: 0

                if (interval < 0) {
                    Toast.makeText(context, "Interval cannot be negative", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (urlText.isNotBlank()) {
                    var trimmedUrl = urlText.trim()
                    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                        trimmedUrl = "https://$trimmedUrl"
                    }
                    prefs.edit().apply {
                        putString("widget_url_$appWidgetId", trimmedUrl)
                        putInt("widget_interval_$appWidgetId", interval)
                    }.apply()

                    Toast.makeText(context, "Widget settings saved!", Toast.LENGTH_SHORT).show()
                    onSave()
                } else {
                    Toast.makeText(context, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Configuration")
        }

        if (prefs.contains("widget_url_$appWidgetId")) {
            Button(
                onClick = {
                    prefs.edit().apply {
                        remove("widget_url_$appWidgetId")
                        remove("widget_interval_$appWidgetId")
                    }.apply()

                    // Clear visual contents on launcher
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = android.widget.RemoteViews(context.packageName, com.example.webwidgetapp.R.layout.widget_layout)
                    views.setImageViewBitmap(com.example.webwidgetapp.R.id.widget_image, null)
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                    // Restart service to stop update loop for this widget
                    WidgetUpdateService.start(context)

                    Toast.makeText(context, "Widget configuration deleted!", Toast.LENGTH_SHORT).show()
                    onSave()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Configuration")
            }
        }
    }
}
