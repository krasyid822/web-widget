package com.example.webwidgetapp.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.webwidgetapp.WebRenderer
import com.example.webwidgetapp.WebViewActivity
import com.example.webwidgetapp.WebWidgetProvider
import com.example.webwidgetapp.WidgetUpdateService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE) }

    // Track widget list + their URLs, refresh when screen is opened
    var widgetIds by remember { mutableStateOf(intArrayOf()) }
    var urlMap by remember { mutableStateOf(mapOf<Int, String>()) }
    var autoRefreshEnabled by remember { mutableStateOf(WidgetUpdateService.isRunning(context)) }
    var zoomPercent by remember { mutableStateOf(WebRenderer.getZoomPercent(context)) }

    fun refreshWidgetList() {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, WebWidgetProvider::class.java))
        widgetIds = ids
        urlMap = ids.associate { id ->
            id to WebRenderer.getUrlForWidget(context, id)
        }
    }

    LaunchedEffect(Unit) { refreshWidgetList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengelola Widget Web", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // --- Auto-Refresh Toggle ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (autoRefreshEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Refresh",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (autoRefreshEnabled) "Aktif · screenshot tiap 1 detik"
                                       else "Nonaktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoRefreshEnabled,
                            onCheckedChange = { enabled ->
                                autoRefreshEnabled = enabled
                                if (enabled) {
                                    WidgetUpdateService.start(context, 1000L)
                                    Toast.makeText(context, "Auto-refresh dimulai", Toast.LENGTH_SHORT).show()
                                } else {
                                    WidgetUpdateService.stop(context)
                                    Toast.makeText(context, "Auto-refresh dihentikan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // --- Zoom Page ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zoom Halaman",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "$zoomPercent%",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Slider(
                            value = zoomPercent.toFloat(),
                            onValueChange = { value ->
                                zoomPercent = value.toInt()
                            },
                            onValueChangeFinished = {
                                WebRenderer.setZoomPercent(context, zoomPercent)
                            },
                            valueRange = 25f..300f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("25%", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("100%", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("300%", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // --- Active Widgets ---
            if (widgetIds.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Belum ada widget aktif",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tambahkan widget dari launcher untuk mengelolanya di sini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(widgetIds.toList()) { widgetId ->
                    var urlText by remember { mutableStateOf(urlMap[widgetId] ?: "https://google.com") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Widget #$widgetId",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "🌐 Buka",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .run {
                                            this.then(Modifier.padding(0.dp))
                                        }
                                )
                            }

                            OutlinedTextField(
                                value = urlText,
                                onValueChange = { urlText = it },
                                label = { Text("URL Widget") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Update widget URL
                                Button(
                                    onClick = {
                                        val url = urlText.trim()
                                        if (url.isBlank()) {
                                            Toast.makeText(context, "URL tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                            "https://$url"
                                        } else url
                                        urlText = finalUrl
                                        prefs.edit().putString("widget_url_$widgetId", finalUrl).apply()
                                        val mgr = AppWidgetManager.getInstance(context)
                                        WebRenderer.updateWidget(context, mgr, intArrayOf(widgetId))
                                        Toast.makeText(context, "Widget #$widgetId diperbarui!", Toast.LENGTH_SHORT).show()
                                        refreshWidgetList()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Perbarui")
                                }

                                // Open in in-app WebView
                                OutlinedButton(
                                    onClick = {
                                        val url = urlText.trim().let {
                                            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
                                        }
                                        val intent = Intent(context, WebViewActivity::class.java).apply {
                                            putExtra(WebViewActivity.EXTRA_URL, url)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🌐 Buka")
                                }
                            }
                        }
                    }
                }
            }

            // --- Instructions ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Cara menambahkan widget:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("1. Tahan layar beranda launcher.", style = MaterialTheme.typography.bodyMedium)
                        Text("2. Pilih 'Widget'.", style = MaterialTheme.typography.bodyMedium)
                        Text("3. Cari 'Web Widget App' dan seret ke layar.", style = MaterialTheme.typography.bodyMedium)
                        Text("4. Masukkan URL saat dialog muncul.", style = MaterialTheme.typography.bodyMedium)
                        Text("5. Setiap widget bisa menampilkan URL yang berbeda!", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
