package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppRepository
import com.example.data.database.AppDatabase
import com.example.ui.screens.MainScreen
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Instantiate our unified offline Database & Repository
    val database = AppDatabase.getDatabase(this)
    val repository = AppRepository(database)

    setContent {
      val viewModel: AppViewModel = viewModel(
        factory = AppViewModelFactory(repository)
      )
      
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
      ) {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}
