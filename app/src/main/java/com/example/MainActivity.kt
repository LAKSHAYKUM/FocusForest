package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SessionState
import com.example.presentation.MainViewModel
import com.example.presentation.navigation.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Sync lock status with system state
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        mainViewModel.uiState.collect { state ->
          val isFocusing = state.sessionState == SessionState.ACTIVE ||
              state.sessionState == SessionState.CALIBRATING ||
              state.sessionState == SessionState.MOVED_WARNING ||
              state.sessionState == SessionState.RESTORED

          if (isFocusing && !state.isStrictLockActive) {
            mainViewModel.lockManager.startFocusLock(this@MainActivity, forceStrict = state.strictLockEnabled)
          } else if (!isFocusing && state.isStrictLockActive) {
            mainViewModel.lockManager.stopFocusLock(this@MainActivity)
          }
        }
      }
    }

    setContent {
      val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val isDark = when (uiState.dayNightMode) {
        "DAY" -> false
        "NIGHT" -> true
        else -> systemDark
      }
      MyApplicationTheme(darkTheme = isDark) {
        MainScreen(viewModel = mainViewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    mainViewModel.lockManager.syncCurrentMode(this)
  }
}
