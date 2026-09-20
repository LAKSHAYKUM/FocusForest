package com.example.presentation.focus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.FocusMode
import com.example.domain.model.MovementState
import com.example.domain.model.SessionState
import com.example.domain.model.TreeStage
import com.example.presentation.MainViewModel

@Composable
fun FocusScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (isGranted) {
            viewModel.startPlacementCalibration {
                viewModel.startFocusSession()
            }
        }
    }

    fun initiateStart() {
        if (uiState.selectedMode == FocusMode.PLACEMENT) {
            val hasCamera = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasCamera) {
                showCameraPermissionDialog = true
            } else {
                viewModel.setCameraPermission(true)
                viewModel.startPlacementCalibration {
                    viewModel.startFocusSession()
                }
            }
        } else {
            viewModel.startFocusSession()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_screen")
    ) {
        when (uiState.sessionState) {
            SessionState.CALIBRATING -> {
                PlacementCalibrationView(
                    calibrationProgress = uiState.calibrationProgress,
                    movementState = uiState.movementState,
                    onCancel = { viewModel.cancelPlacementCalibration() },
                    onRetry = { viewModel.retryPlacementCalibration() }
                )
            }

            SessionState.ACTIVE, SessionState.PAUSED, SessionState.MOVED_WARNING, SessionState.RESTORED -> {
                FocusActiveView(
                    remainingSeconds = uiState.remainingSeconds,
                    progress = uiState.timerProgress,
                    sessionState = uiState.sessionState,
                    focusMode = uiState.selectedMode,
                    movementEventsCount = uiState.movementEventsCount,
                    isStrictLockActive = uiState.isStrictLockActive,
                    reducedMotion = uiState.reducedMotion,
                    treeStyle = uiState.treeStyle,
                    activeTreeId = viewModel.currentSessionTreeId.collectAsStateWithLifecycle().value,
                    dayNightMode = uiState.dayNightMode,
                    onPauseClicked = { viewModel.pauseSession() },
                    onResumeClicked = { viewModel.resumeSession() },
                    onEndSessionClicked = { viewModel.requestEndSessionConfirmation() }
                )

                // Overlay warning if phone was moved
                AnimatedVisibility(
                    visible = uiState.sessionState == SessionState.MOVED_WARNING ||
                            uiState.sessionState == SessionState.RESTORED,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PhoneMovedWarningOverlay(
                        movementState = uiState.movementState,
                        autoResumeEnabled = uiState.autoResume,
                        onResumeClicked = { viewModel.resumeSession() },
                        onEndSessionClicked = { viewModel.requestEndSessionConfirmation() },
                        returnProgress = uiState.returnProgress
                    )
                }

                // Debug Diagnostics HUD (when developer mode enabled)
                if (uiState.isDebugModeEnabled) {
                    PlacementDiagnosticsView(
                        diagnostics = uiState.diagnostics,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                    )
                }
            }

            else -> {
                // Idle Configuration View
                FocusConfigurationView(
                    viewModel = viewModel,
                    selectedDuration = uiState.selectedDurationMinutes,
                    selectedMode = uiState.selectedMode,
                    activeTreeId = viewModel.activeTreeId.value,
                    onSelectDuration = { viewModel.selectDuration(it) },
                    onSelectMode = { viewModel.selectMode(it) },
                    onStartClicked = { initiateStart() }
                )
            }
        }

        // Confirmation Dialog before ending session early
        if (uiState.showEndConfirmation) {
            SessionEndConfirmationDialog(
                onContinue = { viewModel.dismissEndConfirmation() },
                onConfirmEnd = { viewModel.confirmEndSession() }
            )
        }

        // Completion Dialog
        uiState.completedResult?.let { result ->
            SessionCompletedDialog(
                result = result,
                onDismiss = { viewModel.dismissCompletedModal() }
            )
        }

        // Camera Permission Contextual Explanation Dialog
        if (showCameraPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showCameraPermissionDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Placement Focus Camera Permission",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Placement Focus uses your phone's camera and motion sensors only to monitor whether your phone stays securely in place during your focus session.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF43A047),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% on-device local processing. No photos or videos are ever saved or uploaded.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCameraPermissionDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant & Begin")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showCameraPermissionDialog = false
                            // Fallback to standard focus
                            viewModel.selectMode(FocusMode.STANDARD)
                            viewModel.startFocusSession()
                        },
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text("Use Standard Focus")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
private fun FocusConfigurationView(
    viewModel: MainViewModel,
    selectedDuration: Int,
    selectedMode: FocusMode,
    activeTreeId: String = "tree_default",
    onSelectDuration: (Int) -> Unit,
    onSelectMode: (FocusMode) -> Unit,
    onStartClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationPresets = listOf(15, 25, 45, 60)
    var isCustomSelected by remember { mutableStateOf(!durationPresets.contains(selectedDuration)) }
    var customHours by remember { mutableStateOf(if (!durationPresets.contains(selectedDuration)) (selectedDuration / 60).toString() else "0") }
    var customMinutes by remember { mutableStateOf(if (!durationPresets.contains(selectedDuration)) (selectedDuration % 60).toString() else "30") }

    val parsedHours = customHours.toIntOrNull() ?: 0
    val parsedMinutes = customMinutes.toIntOrNull() ?: 0
    val totalCustomMinutes = parsedHours * 60 + parsedMinutes

    val customValidationErrorMessage = when {
        customHours.isBlank() && customMinutes.isBlank() -> "Enter hours or minutes"
        customHours.toIntOrNull() == null || customMinutes.toIntOrNull() == null -> "Invalid numbers entered"
        parsedHours < 0 || parsedMinutes < 0 -> "Values cannot be negative"
        parsedHours > 12 -> "Maximum focus duration is 12 hours"
        parsedMinutes > 59 && parsedHours > 0 -> "Minutes must be between 0 and 59"
        totalCustomMinutes == 0 -> "Duration must be at least 1 minute"
        totalCustomMinutes > 720 -> "Duration cannot exceed 12 hours (720 min)"
        else -> null
    }

    val stage = TreeStage.fromDuration(selectedDuration)
    val activeTree = remember(activeTreeId) { com.example.domain.tree.TreeCatalog.findById(activeTreeId) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "Set Focus Session",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your duration and placement mode to grow your forest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Mode Selector Tabs (Placement vs Standard)
        item {
            TabRow(
                selectedTabIndex = if (selectedMode == FocusMode.PLACEMENT) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("mode_selector_tabs")
            ) {
                Tab(
                    selected = selectedMode == FocusMode.PLACEMENT,
                    onClick = { onSelectMode(FocusMode.PLACEMENT) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Placement Focus", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedMode == FocusMode.STANDARD,
                    onClick = { onSelectMode(FocusMode.STANDARD) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Standard Focus", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Mode Explainer Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMode == FocusMode.PLACEMENT)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (selectedMode == FocusMode.PLACEMENT) Icons.Default.Shield else Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (selectedMode == FocusMode.PLACEMENT) "Physical Phone Stillness" else "Timer Focus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedMode == FocusMode.PLACEMENT)
                                "Place your phone resting on your desk. FocusForest locks your position using motion sensors & camera. Picking up your phone triggers a movement warning and pauses progress."
                            else "Standard countdown timer. Session continues in the background with persistent foreground notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Duration Selector
        item {
            Text(
                text = "Session Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationPresets.forEach { minutes ->
                    val isSelected = !isCustomSelected && selectedDuration == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            isCustomSelected = false
                            onSelectDuration(minutes)
                        },
                        label = {
                            Text(
                                text = "${minutes} MIN",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("duration_chip_${minutes}m")
                    )
                }

                // CUSTOM chip alongside existing presets
                FilterChip(
                    selected = isCustomSelected,
                    onClick = {
                        isCustomSelected = true
                        if (totalCustomMinutes in 1..720) {
                            onSelectDuration(totalCustomMinutes)
                        }
                    },
                    label = {
                        Text(
                            text = "CUSTOM",
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("duration_chip_custom")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Custom Duration Selector Card
        if (isCustomSelected) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_duration_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "CUSTOM FOCUS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Hours input
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hours",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customHours,
                                    onValueChange = { newVal ->
                                        if (newVal.all { it.isDigit() } && newVal.length <= 2) {
                                            customHours = newVal
                                            val h = newVal.toIntOrNull() ?: 0
                                            val m = customMinutes.toIntOrNull() ?: 0
                                            val total = h * 60 + m
                                            if (total in 1..720) {
                                                onSelectDuration(total)
                                            }
                                        }
                                    },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_hours_input")
                                )
                            }

                            // Minutes input
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Minutes",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customMinutes,
                                    onValueChange = { newVal ->
                                        if (newVal.all { it.isDigit() } && newVal.length <= 3) {
                                            customMinutes = newVal
                                            val h = customHours.toIntOrNull() ?: 0
                                            val m = newVal.toIntOrNull() ?: 0
                                            val total = h * 60 + m
                                            if (total in 1..720) {
                                                onSelectDuration(total)
                                            }
                                        }
                                    },
                                    placeholder = { Text("30") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_minutes_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Estimated Focus display / validation message
                        val estimatedText = when {
                            customValidationErrorMessage != null -> customValidationErrorMessage
                            totalCustomMinutes >= 60 && totalCustomMinutes % 60 == 0 ->
                                "Estimated Focus: ${totalCustomMinutes / 60} ${if (totalCustomMinutes / 60 == 1) "hour" else "hours"}"
                            totalCustomMinutes >= 60 ->
                                "Estimated Focus: ${totalCustomMinutes / 60} hr ${totalCustomMinutes % 60} min (${totalCustomMinutes} minutes)"
                            totalCustomMinutes > 0 ->
                                "Estimated Focus: $totalCustomMinutes minutes"
                            else -> "Enter focus duration (1 min to 12 hours)"
                        }

                        Text(
                            text = estimatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (customValidationErrorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Growth Reward Forecast Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(activeTree.palette.primaryFoliage.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = activeTree.palette.primaryFoliage,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Grows: ${activeTree.name} (${stage.title})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reward: +${stage.xpValue + if (selectedMode == FocusMode.PLACEMENT) 30 else 0} Forest XP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Begin Focus Button
        item {
            val isCustomValid = !isCustomSelected || (customValidationErrorMessage == null && totalCustomMinutes in 1..720)
            Button(
                onClick = onStartClicked,
                enabled = isCustomValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("begin_focus_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedMode == FocusMode.PLACEMENT) "Place Phone & Begin" else "Begin Focus Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            com.example.presentation.ads.InlineBannerAd(
                viewModel = viewModel,
                placement = com.example.ads.AdPlacement.FOREST_BOTTOM
            )
        }
    }
}
