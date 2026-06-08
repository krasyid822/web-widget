package com.example.webwidgetapp

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.webwidgetapp.theme.WebWidgetAppTheme
import com.example.webwidgetapp.ui.main.MainScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val perms = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
      requestPermissions(perms, 1001)
    }

    enableEdgeToEdge()

    val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    val isConfigMode = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID

    if (isConfigMode) {
      setResult(RESULT_CANCELED)
    }

    setContent {
      WebWidgetAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainScreen(
            appWidgetId = appWidgetId,
            onConfigFinished = {
              if (isConfigMode) {
                val resultValue = Intent().apply {
                  putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)
                finish()
              }
            }
          )
        }
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 1001) {
      val allGranted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
      if (allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
          requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1002)
        }
      }
    }
  }
}
