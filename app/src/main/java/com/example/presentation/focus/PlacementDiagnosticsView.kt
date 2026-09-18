package com.example.presentation.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.placement.PlacementDiagnostics

@Composable
fun PlacementDiagnosticsView(
    diagnostics: PlacementDiagnostics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("placement_diagnostics_hud"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xDD111A15)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLACEMENT DIAGNOSTICS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF81C784)
                )

                Text(
                    text = diagnostics.movementState.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = when (diagnostics.movementState.name) {
                        "NORMAL" -> Color(0xFF4CAF50)
                        "MOVED" -> Color(0xFFFF7043)
                        "RETURNING" -> Color(0xFF29B6F6)
                        "CALIBRATING" -> Color(0xFFFFD54F)
                        else -> Color.White
                    },
                    modifier = Modifier
                        .background(Color(0x33000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tilt Metric
            MetricRow(
                label = "Tilt Angle Δ",
                value = String.format("%.1f°", diagnostics.tiltDeltaDeg),
                limit = "mv: %.0f° / rt: %.0f°".format(diagnostics.currentThresholdTilt, diagnostics.currentReturnTilt),
                isExceeded = diagnostics.tiltDeltaDeg >= diagnostics.currentThresholdTilt
            )

            // Rotation Metric
            MetricRow(
                label = "Rotation Yaw Δ",
                value = String.format("%.1f°", diagnostics.rotationDeltaDeg),
                limit = "mv: %.0f° / rt: %.0f°".format(diagnostics.currentThresholdRotation, diagnostics.currentReturnRotation),
                isExceeded = diagnostics.rotationDeltaDeg >= diagnostics.currentThresholdRotation
            )

            // Gyroscope Stillness Metric
            MetricRow(
                label = "Angular Vel (ω)",
                value = String.format("%.2f rad/s", diagnostics.angularVelocity),
                limit = if (diagnostics.isStill) "STILL (<= 0.25)" else "MOVING (> 0.25)",
                isExceeded = !diagnostics.isStill
            )

            // Visual Grid Metric
            MetricRow(
                label = "Visual Luma Δ",
                value = String.format("%.2f", diagnostics.visualDiff),
                limit = "samples: ${diagnostics.calibrationSampleCount}",
                isExceeded = diagnostics.visualDiff >= 0.25f
            )

            // Return Confirmation Progress
            if (diagnostics.movementState.name == "RETURNING" || diagnostics.returnProgress > 0f) {
                MetricRow(
                    label = "Return Stillness",
                    value = String.format("%.0f%%", diagnostics.returnProgress * 100f),
                    limit = "${diagnostics.consecutiveReturnFrames}/12 frames",
                    isExceeded = false
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    limit: String,
    isExceeded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.7f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isExceeded) Color(0xFFFF8A80) else Color(0xFFA5D6A7)
            )
            Text(
                text = " [$limit]",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
