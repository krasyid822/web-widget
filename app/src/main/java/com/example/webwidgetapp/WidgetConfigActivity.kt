package com.example.webwidgetapp

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result = canceled
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Load previous URL for this widget ID if any
        val prefs = getSharedPreferences("WebWidgetPrefs", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("widget_url_$appWidgetId", "https://google.com") ?: "https://google.com"

        // Simple layout with an EditText and a Confirm button
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val titleView = android.widget.TextView(this).apply {
            text = "URL untuk Widget Ini"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }

        val urlInput = EditText(this).apply {
            hint = "https://example.com"
            setText(savedUrl)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI or android.text.InputType.TYPE_CLASS_TEXT
        }

        val confirmBtn = Button(this).apply {
            text = "Tambahkan Widget"
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isBlank()) {
                    Toast.makeText(this@WidgetConfigActivity, "URL tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else url

                // Save URL for this specific widget
                prefs.edit().putString("widget_url_$appWidgetId", finalUrl).apply()

                // Trigger first widget render
                val appWidgetManager = AppWidgetManager.getInstance(this@WidgetConfigActivity)
                WebRenderer.updateWidget(this@WidgetConfigActivity, appWidgetManager, intArrayOf(appWidgetId))

                // Return OK result
                val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        layout.addView(titleView)
        layout.addView(urlInput)
        layout.addView(confirmBtn)
        setContentView(layout)
    }
}
