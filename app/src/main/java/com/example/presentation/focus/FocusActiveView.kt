package com.example.presentation.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FocusMode
import com.example.domain.model.SessionState

@Composable
fun FocusActiveView(
    remainingSeconds: Long,
    progress: Float,
    sessionState: SessionState,
    focusMode: FocusMode,
    movementEventsCount: Int,
    isStrictLockActive: Boolean = false,
    reducedMotion: Boolean = false,
    treeStyle: String = "PINE",
    dayNightMode: String = "SYSTEM",
    onPauseClicked: () -> Unit,
    onResumeClicked: () -> Unit,
    onEndSessionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = sessionState == SessionState.PAUSED
    val isSystemDark = isSystemInDarkTheme()
    val isNight = when (dayNightMode) {
        "DAY" -> false
        "NIGHT" -> true
        else -> isSystemDark
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    // Growth stage description based on progress
    val growthPhaseTitle = when {
        progress < 0.05f -> "Seed settling into soil"
        progress < 0.20f -> "Germination & Awakening"
        progress < 0.35f -> "Root system extending"
        progress < 0.50f -> "Tender shoot emerging"
        progress < 0.70f -> "Sapling stem & first leaves"
        progress < 0.88f -> "Canopy branching out"
        else -> "Canopy flourishing"
    }

    val backgroundBrush = if (isNight) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF101613), Color(0xFF0C100E), Color(0xFF090D0B))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF4F7F4), Color(0xFFEAF1EB), Color(0xFFE2EBE3))
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .testTag("focus_active_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Status Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Badge (Placement vs Standard)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (focusMode == FocusMode.PLACEMENT) {
                            Color(0x2243A047)
                        } else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("session_mode_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (focusMode == FocusMode.PLACEMENT) Icons.Default.Lock else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (focusMode == FocusMode.PLACEMENT) Color(0xFF43A047) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (focusMode == FocusMode.PLACEMENT) "Position Locked" else "Standard Focus",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (focusMode == FocusMode.PLACEMENT) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (isStrictLockActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x223F51B5)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Strict Lock",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3F51B5)
                                )
                            }
                        }
                    }

                    if (isPaused) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x33FF9800)
                        ) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF6C00),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Big Serene Time Display
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 58.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPaused) "Session paused • Plant resting" else growthPhaseTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Center Living Seed-To-Tree Growth Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val seedVar = when (treeStyle.uppercase()) {
                    "OAK" -> 108L
                    "CEDAR" -> 256L
                    else -> 42L
                }
                ProceduralTreeGrowthCanvas(
                    progress = progress,
                    isPaused = isPaused,
                    isNight = isNight,
                    reducedMotion = reducedMotion,
                    seedVariation = seedVar,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Bottom Controls (Minimal, clean Nothing / Apple aesthetic)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // End Session Early
                OutlinedIconButton(
                    onClick = onEndSessionClicked,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("end_session_button"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End Session",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Pause / Resume Pill
                Button(
                    onClick = {
                        if (isPaused) onResumeClicked() else onPauseClicked()
                    },
                    modifier = Modifier
                        .height(54.dp)
                        .width(160.dp)
                        .testTag("pause_resume_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) "Resume" else "Pause",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
