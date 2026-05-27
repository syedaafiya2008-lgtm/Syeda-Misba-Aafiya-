package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import com.example.notification.WaterNotificationHelper
import com.example.ui.WaterTrackerScreen
import com.example.ui.WaterViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Setup Local Push Notification Channel on Startup
    WaterNotificationHelper.createNotificationChannel(applicationContext)

    // Build DB Singleton, Repos, and viewmodel factory
    val database = WaterDatabase.getDatabase(applicationContext)
    val repository = WaterRepository(database.waterDao())
    val factory = WaterViewModel.Factory(application, repository)
    val viewModel: WaterViewModel by viewModels { factory }

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          WaterTrackerScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
