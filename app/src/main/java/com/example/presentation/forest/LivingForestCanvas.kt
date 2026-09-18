package com.example.presentation.forest

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

@Composable
fun LivingForestCanvas(
    treeCount: Int,
    forestLevel: Int,
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "forest_anim")

    val windSway by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wind_sway"
    )

    val leafPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "leaf_drift"
    )

    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Sky Gradient
        val skyBrush = if (isNight) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B1210),
                    Color(0xFF13201C),
                    Color(0xFF1A2C26)
                ),
                startY = 0f,
                endY = h * 0.75f
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFD6EBE4),
                    Color(0xFFEAF5F0),
                    Color(0xFFFAFBF8)
                ),
                startY = 0f,
                endY = h * 0.75f
            )
        }
        drawRect(brush = skyBrush, size = size)

        // 2. Celestial Orb (Sun or Moon)
        val orbColor = if (isNight) Color(0xFFE8F5E9) else Color(0xFFFFE082)
        val orbGlow = if (isNight) Color(0x3381C784) else Color(0x40FFD54F)
        val orbCenter = Offset(w * 0.75f, h * 0.22f)
        val baseOrbRadius = w * 0.08f * sunPulse

        drawCircle(
            color = orbGlow,
            radius = baseOrbRadius * 1.8f,
            center = orbCenter
        )
        drawCircle(
            color = orbColor,
            radius = baseOrbRadius,
            center = orbCenter
        )

        // 3. Distant Mountain Ridge
        val mountainColor = if (isNight) Color(0xFF13221C) else Color(0xFFC0D8CD)
        val mountainPath = Path().apply {
            moveTo(0f, h * 0.58f)
            cubicTo(w * 0.25f, h * 0.46f, w * 0.4f, h * 0.52f, w * 0.65f, h * 0.42f)
            cubicTo(w * 0.8f, h * 0.48f, w * 0.92f, h * 0.45f, w, h * 0.54f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(mountainPath, color = mountainColor)

        // 4. Midground Rolling Hill
        val hillColor = if (isNight) Color(0xFF182C24) else Color(0xFF8BAE9D)
        val hillPath = Path().apply {
            moveTo(0f, h * 0.66f)
            cubicTo(w * 0.35f, h * 0.60f, w * 0.7f, h * 0.70f, w, h * 0.62f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(hillPath, color = hillColor)

        // 5. Midground Pines on Hill
        val midgroundPineColor = if (isNight) Color(0xFF14241E) else Color(0xFF6B9280)
        for (i in 0..8) {
            val px = w * (0.08f + i * 0.11f)
            val py = h * (0.62f + sin(i * 1.3f) * 0.03f)
            val treeH = h * 0.08f
            drawPineSilhouette(px, py, treeH, midgroundPineColor, windSway * 0.5f)
        }

        // 6. Foreground Lush Green Mound
        val foregroundHillColor = if (isNight) Color(0xFF1B382B) else Color(0xFF33634E)
        val fgPath = Path().apply {
            moveTo(0f, h * 0.76f)
            cubicTo(w * 0.3f, h * 0.72f, w * 0.65f, h * 0.78f, w, h * 0.73f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(fgPath, color = foregroundHillColor)

        // 7. Render Foreground Dynamic Forest Trees based on user's earned trees & level
        val fgPineColor = if (isNight) Color(0xFF2E634C) else Color(0xFF1E523E)
        val fgTrunkColor = if (isNight) Color(0xFF4A3828) else Color(0xFF5D4037)

        // Number of foreground trees displayed (scales smoothly from 1 to 15 based on progress)
        val visibleTrees = when {
            treeCount == 0 -> 1 // Solitary sprout
            treeCount < 5 -> 3
            treeCount < 15 -> 6
            treeCount < 30 -> 9
            else -> 12
        }

        // Main Hero Tree (always in center or focal point)
        val heroX = w * 0.48f
        val heroY = h * 0.75f
        val heroScale = when {
            treeCount == 0 -> 0.09f // Sprout size
            treeCount < 5 -> 0.18f
            treeCount < 20 -> 0.24f
            else -> 0.28f // Ancient giant
        }
        drawDetailedTree(
            x = heroX,
            baseY = heroY,
            height = h * heroScale,
            trunkColor = fgTrunkColor,
            foliageColor = fgPineColor,
            highlightColor = if (isNight) Color(0xFF438A6A) else Color(0xFF388E3C),
            windOffset = windSway * 8f,
            isSprout = treeCount == 0
        )

        // Additional Grove Trees
        if (visibleTrees > 1) {
            val positions = listOf(
                Pair(0.22f, 0.78f),
                Pair(0.72f, 0.77f),
                Pair(0.12f, 0.81f),
                Pair(0.85f, 0.80f),
                Pair(0.34f, 0.79f),
                Pair(0.62f, 0.82f),
                Pair(0.40f, 0.83f),
                Pair(0.92f, 0.84f),
                Pair(0.05f, 0.85f),
                Pair(0.55f, 0.84f),
                Pair(0.78f, 0.86f)
            )

            for (i in 0 until (visibleTrees - 1).coerceAtMost(positions.size)) {
                val (relX, relY) = positions[i]
                val tHeight = h * (0.12f + (i % 3) * 0.04f)
                val tWind = windSway * (5f + (i % 3) * 2f)
                drawDetailedTree(
                    x = w * relX,
                    baseY = h * relY,
                    height = tHeight,
                    trunkColor = fgTrunkColor,
                    foliageColor = fgPineColor,
                    highlightColor = if (isNight) Color(0xFF438A6A) else Color(0xFF388E3C),
                    windOffset = tWind,
                    isSprout = false
                )
            }
        }

        // 8. Drifting Atmospheric Leaves / Spores
        val leafColor = if (isNight) Color(0x8081C784) else Color(0x9943A047)
        for (i in 0..10) {
            val progressOffset = (leafPhase + (i * 0.09f)) % 1f
            val lx = w * ((progressOffset * 1.2f - 0.1f))
            val ly = h * (0.45f + sin((progressOffset * 6.28f) + i) * 0.15f)
            val radius = 2.5f + (i % 3)
            drawCircle(color = leafColor, radius = radius, center = Offset(lx, ly))
        }
    }
}

private fun DrawScope.drawPineSilhouette(
    x: Float,
    baseY: Float,
    height: Float,
    color: Color,
    wind: Float
) {
    val width = height * 0.45f
    val path = Path().apply {
        moveTo(x + wind, baseY - height)
        lineTo(x + width / 2, baseY)
        lineTo(x - width / 2, baseY)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawDetailedTree(
    x: Float,
    baseY: Float,
    height: Float,
    trunkColor: Color,
    foliageColor: Color,
    highlightColor: Color,
    windOffset: Float,
    isSprout: Boolean
) {
    if (isSprout) {
        // Draw delicate seedling
        val stemH = height * 0.6f
        drawLine(
            color = highlightColor,
            start = Offset(x, baseY),
            end = Offset(x + windOffset * 0.5f, baseY - stemH),
            strokeWidth = 3f
        )
        drawCircle(
            color = highlightColor,
            radius = 6f,
            center = Offset(x - 5f + windOffset * 0.5f, baseY - stemH - 2f)
        )
        drawCircle(
            color = foliageColor,
            radius = 6f,
            center = Offset(x + 5f + windOffset * 0.5f, baseY - stemH - 2f)
        )
        return
    }

    val trunkW = (height * 0.12f).coerceAtLeast(4f)
    val trunkH = height * 0.28f

    // Trunk
    drawRect(
        color = trunkColor,
        topLeft = Offset(x - trunkW / 2, baseY - trunkH),
        size = Size(trunkW, trunkH)
    )

    // Tier 1 (bottom)
    val t1Bottom = baseY - trunkH * 0.7f
    val t1Height = height * 0.38f
    val t1Width = height * 0.65f
    drawTreeTier(x, t1Bottom, t1Width, t1Height, foliageColor, windOffset * 0.3f)

    // Tier 2 (middle)
    val t2Bottom = t1Bottom - t1Height * 0.55f
    val t2Height = height * 0.35f
    val t2Width = height * 0.52f
    drawTreeTier(x, t2Bottom, t2Width, t2Height, foliageColor, windOffset * 0.6f)

    // Tier 3 (top)
    val t3Bottom = t2Bottom - t2Height * 0.55f
    val t3Height = height * 0.35f
    val t3Width = height * 0.38f
    drawTreeTier(x, t3Bottom, t3Width, t3Height, highlightColor, windOffset)
}

private fun DrawScope.drawTreeTier(
    centerX: Float,
    baseY: Float,
    width: Float,
    height: Float,
    color: Color,
    wind: Float
) {
    val path = Path().apply {
        moveTo(centerX + wind, baseY - height)
        lineTo(centerX + width / 2, baseY)
        lineTo(centerX - width / 2, baseY)
        close()
    }
    drawPath(path, color = color)
}
