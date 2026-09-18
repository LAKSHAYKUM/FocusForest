package com.example.presentation.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MovementState

@Composable
fun PhoneMovedWarningOverlay(
    movementState: MovementState,
    autoResumeEnabled: Boolean,
    onResumeClicked: () -> Unit,
    onEndSessionClicked: () -> Unit,
    returnProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val isRestored = movementState == MovementState.NORMAL
    val isReturning = movementState == MovementState.RETURNING

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = when {
                        isRestored -> listOf(Color(0xEE0A291E), Color(0xF2143B2C))
                        isReturning -> listOf(Color(0xEE0D263B), Color(0xF215354F))
                        else -> listOf(Color(0xEE2A1207), Color(0xF538180A))
                    }
                )
            )
            .testTag("phone_moved_warning_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isRestored -> Color(0xFF143B2C)
                    isReturning -> Color(0xFF15354F)
                    else -> Color(0xFF2C160B)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Warning / Checking / Restored Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            when {
                                isRestored -> Color(0x334CAF50)
                                isReturning -> Color(0x3329B6F6)
                                else -> Color(0x33FF9800)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isRestored -> Icons.Default.CheckCircle
                            isReturning -> Icons.Default.CheckCircle
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when {
                            isRestored -> Color(0xFF4CAF50)
                            isReturning -> Color(0xFF29B6F6)
                            else -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = when {
                        isRestored -> "Phone is back in place ✓"
                        isReturning -> "Checking position..."
                        else -> "Phone moved"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        isRestored -> {
                            if (autoResumeEnabled) "Resuming focus session..."
                            else "Phone returned to original position. Tap Resume when ready."
                        }
                        isReturning -> "Verifying phone stability in original position. Keep still..."
                        else -> "Put your phone back in its original position to continue growing your tree."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                if (isReturning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { returnProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(6.dp),
                        color = Color(0xFF29B6F6),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isRestored || !autoResumeEnabled) {
                    Button(
                        onClick = onResumeClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("resume_focus_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Resume Focus",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = onEndSessionClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "End Focus Early",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
