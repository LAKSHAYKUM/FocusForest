package com.example.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SessionState
import com.example.presentation.MainViewModel
import com.example.presentation.focus.FocusScreen
import com.example.presentation.forest.ForestHomeScreen
import com.example.presentation.history.HistoryScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.stats.StatsScreen

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showTreesCatalog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val navItems = listOf(
        NavItem("Forest", Icons.Default.Eco, "nav_forest"),
        NavItem("Focus", Icons.Default.Timer, "nav_focus"),
        NavItem("Stats", Icons.Default.BarChart, "nav_stats"),
        NavItem("History", Icons.Default.History, "nav_history"),
        NavItem("Settings", Icons.Default.Settings, "nav_settings")
    )

    // Distraction-free: Active focus locks navigation and intercepts back button
    val isFocusing = uiState.sessionState != SessionState.IDLE &&
            uiState.sessionState != SessionState.COMPLETED &&
            uiState.sessionState != SessionState.INTERRUPTED

    // Intercept back button during focus sessions to avoid accidental abandonment
    BackHandler(enabled = isFocusing || showTreesCatalog) {
        if (showTreesCatalog) {
            showTreesCatalog = false
        } else {
            viewModel.requestEndSessionConfirmation()
        }
    }

    if (!uiState.onboardingCompleted) {
        OnboardingScreen(
            onFinished = { viewModel.completeOnboarding() }
        )
        return
    }

    if (showTreesCatalog) {
        com.example.presentation.tree.TreeCollectionScreen(
            viewModel = viewModel,
            onBack = { showTreesCatalog = false }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = !isFocusing,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = uiState.activeNavTab == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.setActiveTab(index) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFocusing) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
        ) {
            if (isFocusing) {
                // Focus lock: Strictly keep user on Focus screen
                FocusScreen(viewModel = viewModel)
            } else {
                when (uiState.activeNavTab) {
                    0 -> ForestHomeScreen(
                        viewModel = viewModel,
                        onStartFocusClicked = {
                            viewModel.setActiveTab(1)
                            if (uiState.selectedMode == com.example.domain.model.FocusMode.STANDARD) {
                                viewModel.startFocusSession()
                            } else {
                                val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasCamera) {
                                    viewModel.setCameraPermission(true)
                                    viewModel.startPlacementCalibration {
                                        viewModel.startFocusSession()
                                    }
                                }
                            }
                        },
                        onTreesCatalogClicked = {
                            showTreesCatalog = true
                        }
                    )
                    1 -> FocusScreen(viewModel = viewModel)
                    2 -> StatsScreen(viewModel = viewModel)
                    3 -> HistoryScreen(viewModel = viewModel)
                    4 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
