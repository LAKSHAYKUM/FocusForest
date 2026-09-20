package com.example.presentation.focus

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ProceduralTreeGrowthCanvas renders a true live seed-to-tree organic growth cycle.
 *
 * Growth Stages (synchronized with focus timer progress):
 * - 0% - 5%: Seed resting quietly in fertile layered soil.
 * - 5% - 15%: Seed subtly shifts, cracks open with inner vital glow.
 * - 15% - 25%: Taproot and fine lateral roots push downward into the earth.
 * - 25% - 35%: Seed coat splits, tender embryonic cotyledon pushes upward.
 * - 35% - 45%: Green shoot arches through soil line, straightens toward the light.
 * - 45% - 60%: Main stem ascends with natural curvature, primary leaves unfold.
 * - 60% - 70%: First leaf pair fully expands with delicate veins, stem begins woody maturation.
 * - 70% - 85%: Stem thickens, primary lateral branches emerge gracefully at organic angles.
 * - 85% - 95%: Secondary canopy branches spread, lush leaf clusters blossom.
 * - 100%: Full majestic tree completes with subtle radiant spores and crown maturity.
 */
@Composable
fun ProceduralTreeGrowthCanvas(
    progress: Float, // 0.0f to 1.0f
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    isNight: Boolean = false,
    reducedMotion: Boolean = false,
    seedVariation: Long = 42L,
    treeId: String = "tree_default",
    isWithered: Boolean = false
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val treeItem = remember(treeId) { com.example.domain.tree.TreeCatalog.findById(treeId) }
    val palette = treeItem.palette

    val infiniteTransition = rememberInfiniteTransition(label = "tree_ambient")

    val windSway by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wind_sway"
    )

    val lifeGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "life_glow"
    )

    // Seed-based procedural variations so each tree feels unique and organic
    val treeGen = remember(seedVariation) {
        val r = Random(seedVariation)
        TreeGenParams(
            stemCurveFactor = (r.nextFloat() - 0.5f) * 0.15f,
            branchAngleOffset = (r.nextFloat() - 0.5f) * 8f,
            leafJitter = r.nextFloat() * 4f,
            barkTone = r.nextInt(3)
        )
    }

    // Dynamic wind effect: freeze completely if paused, subdue if reducedMotion
    val activeWind = if (isPaused || reducedMotion) 0f else windSway

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Ground anchor coordinates
        val groundY = h * 0.72f
        val centerX = w * 0.5f

        // Draw underground soil layer & surface mound
        drawFertileSoil(w, h, groundY, isNight)

        // 1. Stage: Roots (active from 15% to 100%)
        if (clampedProgress > 0.12f) {
            val rootGrowth = ((clampedProgress - 0.12f) / 0.30f).coerceIn(0f, 1f)
            drawProceduralRoots(
                centerX = centerX,
                groundY = groundY,
                growth = rootGrowth,
                isNight = isNight
            )
        }

        // 2. Stage: Seed & Germination (0% to 35%)
        if (clampedProgress <= 0.40f) {
            val seedFade = if (clampedProgress > 0.28f) {
                1f - ((clampedProgress - 0.28f) / 0.12f).coerceIn(0f, 1f)
            } else 1f

            if (seedFade > 0f) {
                if (treeId == "tree_love") {
                    drawGerminatingLoveSeed(
                        centerX = centerX,
                        groundY = groundY + 4f,
                        progress = clampedProgress,
                        glowPulse = lifeGlow,
                        alpha = seedFade,
                        isNight = isNight,
                        palette = palette
                    )
                } else {
                    drawGerminatingSeed(
                        centerX = centerX,
                        groundY = groundY + 4f,
                        progress = clampedProgress,
                        glowPulse = lifeGlow,
                        alpha = seedFade,
                        isNight = isNight,
                        palette = palette
                    )
                }
            }
        }

        // 3. Stage: Shoot, Stem, Branches & Leaves (emerges at 30% and grows through 100%)
        if (clampedProgress > 0.28f) {
            if (treeId == "tree_love") {
                drawLivingLoveTreeAboveGround(
                    centerX = centerX,
                    groundY = groundY,
                    width = w,
                    height = h,
                    progress = clampedProgress,
                    wind = if (isWithered) activeWind * 0.2f else activeWind,
                    glow = if (isWithered) 0.5f else lifeGlow,
                    params = treeGen,
                    isNight = isNight,
                    palette = palette,
                    isWithered = isWithered
                )
            } else {
                drawLivingTreeAboveGround(
                    centerX = centerX,
                    groundY = groundY,
                    width = w,
                    height = h,
                    progress = clampedProgress,
                    wind = if (isWithered) activeWind * 0.2f else activeWind,
                    glow = if (isWithered) 0.5f else lifeGlow,
                    params = treeGen,
                    isNight = isNight,
                    isPaused = isPaused,
                    palette = palette,
                    isWithered = isWithered
                )
            }
        }

        // 4. Ambient spores or rose blossom petals
        if (clampedProgress > 0.75f && !reducedMotion && !isWithered) {
            val sporeFactor = ((clampedProgress - 0.75f) / 0.25f).coerceIn(0f, 1f)
            if (treeId == "tree_love") {
                drawAmbientLovePetals(
                    centerX = centerX,
                    treeTopY = groundY - (h * 0.48f * clampedProgress),
                    sporeFactor = sporeFactor,
                    phase = windSway,
                    isNight = isNight,
                    color = palette.customParticleColor ?: Color(0xFFFF4081)
                )
            } else if (clampedProgress > 0.80f) {
                drawAmbientPollenSpores(
                    centerX = centerX,
                    treeTopY = groundY - (h * 0.45f * clampedProgress),
                    sporeFactor = sporeFactor,
                    phase = windSway,
                    isNight = isNight,
                    customColor = palette.customParticleColor
                )
            }
        }
    }
}

private class TreeGenParams(
    val stemCurveFactor: Float,
    val branchAngleOffset: Float,
    val leafJitter: Float,
    val barkTone: Int
)

/**
 * Draws rich, layered organic soil mound and underground cross-section.
 */
private fun DrawScope.drawFertileSoil(
    w: Float,
    h: Float,
    groundY: Float,
    isNight: Boolean
) {
    val undergroundBrush = if (isNight) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF221A14), Color(0xFF16110D), Color(0xFF0F0B09)),
            startY = groundY,
            endY = h
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF533F31), Color(0xFF3F3025), Color(0xFF2C2219)),
            startY = groundY,
            endY = h
        )
    }

    // Underground soil block
    drawRect(
        brush = undergroundBrush,
        topLeft = Offset(0f, groundY),
        size = Size(w, h - groundY)
    )

    // Gentle organic surface mound
    val moundColor = if (isNight) Color(0xFF2D231B) else Color(0xFF6B513E)
    val grassColor = if (isNight) Color(0xFF234B38) else Color(0xFF43855E)

    val moundPath = Path().apply {
        moveTo(0f, groundY + 12f)
        cubicTo(
            w * 0.25f, groundY + 4f,
            w * 0.40f, groundY - 6f,
            w * 0.50f, groundY - 8f
        )
        cubicTo(
            w * 0.60f, groundY - 6f,
            w * 0.75f, groundY + 4f,
            w, groundY + 12f
        )
        lineTo(w, groundY + 24f)
        lineTo(0f, groundY + 24f)
        close()
    }
    drawPath(moundPath, color = moundColor)

    // Delicate moss / grass crest along the mound
    val crestPath = Path().apply {
        moveTo(w * 0.20f, groundY + 8f)
        cubicTo(
            w * 0.35f, groundY,
            w * 0.50f, groundY - 7f,
            w * 0.65f, groundY
        )
        cubicTo(
            w * 0.75f, groundY + 5f,
            w * 0.80f, groundY + 8f,
            w * 0.85f, groundY + 10f
        )
    }
    drawPath(
        path = crestPath,
        color = grassColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

/**
 * Draws the seed resting in soil, cracking, and sprouting.
 */
private fun DrawScope.drawGerminatingSeed(
    centerX: Float,
    groundY: Float,
    progress: Float,
    glowPulse: Float,
    alpha: Float,
    isNight: Boolean,
    palette: com.example.domain.tree.TreeVisualPalette
) {
    val seedColor = if (isNight) palette.seedColor.copy(alpha = 0.85f) else palette.seedColor
    val innerGlow = if (isNight) palette.seedGlow else palette.foliageHighlight

    // Seed size & micro twitch in early germination (5% - 15%)
    val seedTwitch = if (progress in 0.05f..0.18f) {
        sin(progress * 80f) * 1.5f
    } else 0f

    val seedCenter = Offset(centerX + seedTwitch, groundY - 4f)
    val seedRadiusX = 9f
    val seedRadiusY = 13f

    // Soft vitality aura when seed begins waking
    if (progress > 0.04f) {
        val auraAlpha = ((progress - 0.04f) / 0.20f).coerceIn(0f, 1f) * 0.35f * alpha
        drawCircle(
            color = innerGlow.copy(alpha = auraAlpha),
            radius = seedRadiusY * 2.2f * glowPulse,
            center = seedCenter
        )
    }

    // Outer Seed Coat (Teardrop / oval shape)
    val seedCoatPath = Path().apply {
        moveTo(seedCenter.x, seedCenter.y - seedRadiusY)
        cubicTo(
            seedCenter.x + seedRadiusX, seedCenter.y - seedRadiusY * 0.4f,
            seedCenter.x + seedRadiusX, seedCenter.y + seedRadiusY * 0.8f,
            seedCenter.x, seedCenter.y + seedRadiusY
        )
        cubicTo(
            seedCenter.x - seedRadiusX, seedCenter.y + seedRadiusY * 0.8f,
            seedCenter.x - seedRadiusX, seedCenter.y - seedRadiusY * 0.4f,
            seedCenter.x, seedCenter.y - seedRadiusY
        )
        close()
    }
    drawPath(seedCoatPath, color = seedColor.copy(alpha = alpha))

    // Seed crack line (appears 8% - 30%)
    if (progress > 0.08f) {
        val crackProgress = ((progress - 0.08f) / 0.16f).coerceIn(0f, 1f)
        val crackPath = Path().apply {
            moveTo(seedCenter.x, seedCenter.y - seedRadiusY * 0.6f)
            lineTo(seedCenter.x + 2f, seedCenter.y - seedRadiusY * 0.1f)
            lineTo(seedCenter.x - 1.5f, seedCenter.y + seedRadiusY * 0.3f)
            lineTo(seedCenter.x + 1f, seedCenter.y + seedRadiusY * 0.6f * crackProgress)
        }
        drawPath(
            path = crackPath,
            color = innerGlow.copy(alpha = alpha),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws procedural root network branching downward through the soil.
 */
private fun DrawScope.drawProceduralRoots(
    centerX: Float,
    groundY: Float,
    growth: Float,
    isNight: Boolean
) {
    val rootColor = if (isNight) Color(0xFFB09B82) else Color(0xFFD7CCC8)
    val rootAlpha = growth.coerceIn(0.2f, 0.85f)

    // Taproot (main deep root)
    val taprootDepth = 55f * growth
    val taprootPath = Path().apply {
        moveTo(centerX, groundY + 4f)
        cubicTo(
            centerX + 3f, groundY + taprootDepth * 0.35f,
            centerX - 2f, groundY + taprootDepth * 0.70f,
            centerX + 1f, groundY + taprootDepth
        )
    }
    drawPath(
        path = taprootPath,
        color = rootColor.copy(alpha = rootAlpha),
        style = Stroke(width = 2.4f * growth.coerceAtLeast(0.4f), cap = StrokeCap.Round)
    )

    // Lateral Root 1 (Left spreading)
    if (growth > 0.25f) {
        val lat1Growth = ((growth - 0.25f) / 0.65f).coerceIn(0f, 1f)
        val lat1Path = Path().apply {
            moveTo(centerX - 1f, groundY + taprootDepth * 0.30f)
            cubicTo(
                centerX - 12f * lat1Growth, groundY + taprootDepth * 0.42f,
                centerX - 22f * lat1Growth, groundY + taprootDepth * 0.60f,
                centerX - 28f * lat1Growth, groundY + taprootDepth * 0.75f
            )
        }
        drawPath(
            path = lat1Path,
            color = rootColor.copy(alpha = rootAlpha * 0.8f),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )
    }

    // Lateral Root 2 (Right spreading)
    if (growth > 0.40f) {
        val lat2Growth = ((growth - 0.40f) / 0.55f).coerceIn(0f, 1f)
        val lat2Path = Path().apply {
            moveTo(centerX + 1f, groundY + taprootDepth * 0.45f)
            cubicTo(
                centerX + 10f * lat2Growth, groundY + taprootDepth * 0.55f,
                centerX + 18f * lat2Growth, groundY + taprootDepth * 0.72f,
                centerX + 26f * lat2Growth, groundY + taprootDepth * 0.85f
            )
        }
        drawPath(
            path = lat2Path,
            color = rootColor.copy(alpha = rootAlpha * 0.8f),
            style = Stroke(width = 1.4f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws the above-ground living organism: sprout, main stem, branching tiers, and leaves.
 */
private fun DrawScope.drawLivingTreeAboveGround(
    centerX: Float,
    groundY: Float,
    width: Float,
    height: Float,
    progress: Float,
    wind: Float,
    glow: Float,
    params: TreeGenParams,
    isNight: Boolean,
    isPaused: Boolean,
    palette: com.example.domain.tree.TreeVisualPalette,
    isWithered: Boolean
) {
    // Height scales gracefully from sprout to towering tree
    val maxTreeHeight = height * 0.44f
    val currentHeight = maxTreeHeight * ((progress - 0.28f) / 0.72f).coerceIn(0.08f, 1.0f)

    // Stem color transition based on species palette or wither state
    val barkTender = if (isWithered) palette.witherBark else palette.barkTender
    val barkMature = if (isWithered) palette.witherBark else palette.barkTone
    val stemWoodProgress = ((progress - 0.45f) / 0.50f).coerceIn(0f, 1f)
    val activeBarkColor = lerpColor(barkTender, barkMature, stemWoodProgress)

    val foliageYoung = if (isWithered) palette.witherFoliage else palette.secondaryFoliage
    val foliageMature = if (isWithered) palette.witherFoliage else palette.primaryFoliage
    val activeFoliageColor = lerpColor(foliageYoung, foliageMature, stemWoodProgress)

    // Base trunk width thickens as plant matures
    val trunkBaseWidth = 4f + (14f * stemWoodProgress)

    // Tip point of main trunk with gentle organic curvature and breeze sway
    val topWindOffset = wind * (currentHeight * 0.08f)
    val curveOffset = params.stemCurveFactor * currentHeight
    val trunkTop = Offset(centerX + curveOffset + topWindOffset, groundY - currentHeight)
    val trunkControl = Offset(centerX + (curveOffset * 0.4f) + (topWindOffset * 0.4f), groundY - (currentHeight * 0.55f))

    // Draw Main Trunk
    val trunkPath = Path().apply {
        moveTo(centerX - trunkBaseWidth / 2f, groundY)
        quadraticTo(
            trunkControl.x - (trunkBaseWidth * 0.35f),
            trunkControl.y,
            trunkTop.x - (trunkBaseWidth * 0.15f),
            trunkTop.y
        )
        lineTo(trunkTop.x + (trunkBaseWidth * 0.15f), trunkTop.y)
        quadraticTo(
            trunkControl.x + (trunkBaseWidth * 0.35f),
            trunkControl.y,
            centerX + trunkBaseWidth / 2f,
            groundY
        )
        close()
    }
    drawPath(trunkPath, color = activeBarkColor)

    // Primary Seedling Leaves (active from 35% onward)
    if (progress in 0.32f..0.68f) {
        val cotyledonProgress = ((progress - 0.32f) / 0.28f).coerceIn(0f, 1f)
        drawCotyledonLeaves(
            stemTip = trunkTop,
            growth = cotyledonProgress,
            wind = wind,
            color = foliageYoung
        )
    }

    // Branching Tier 1 (starts growing at 55%)
    if (progress > 0.52f) {
        val tier1Growth = ((progress - 0.52f) / 0.28f).coerceIn(0f, 1f)
        val branch1Origin = Offset(
            centerX + (curveOffset * 0.35f),
            groundY - (currentHeight * 0.42f)
        )
        drawBranchWithFoliage(
            origin = branch1Origin,
            length = (currentHeight * 0.36f) * tier1Growth,
            angleDeg = -42f + params.branchAngleOffset,
            wind = wind * 0.6f,
            barkColor = activeBarkColor,
            foliageColor = activeFoliageColor,
            leafScale = tier1Growth,
            isLeft = true
        )

        drawBranchWithFoliage(
            origin = branch1Origin.copy(x = branch1Origin.x + 2f),
            length = (currentHeight * 0.34f) * tier1Growth,
            angleDeg = 40f - params.branchAngleOffset,
            wind = wind * 0.6f,
            barkColor = activeBarkColor,
            foliageColor = activeFoliageColor,
            leafScale = tier1Growth,
            isLeft = false
        )
    }

    // Branching Tier 2 (Upper canopy, starts growing at 72%)
    if (progress > 0.70f) {
        val tier2Growth = ((progress - 0.70f) / 0.24f).coerceIn(0f, 1f)
        val branch2Origin = Offset(
            centerX + (curveOffset * 0.65f),
            groundY - (currentHeight * 0.68f)
        )
        drawBranchWithFoliage(
            origin = branch2Origin,
            length = (currentHeight * 0.30f) * tier2Growth,
            angleDeg = -34f + params.branchAngleOffset * 0.5f,
            wind = wind * 0.8f,
            barkColor = activeBarkColor,
            foliageColor = activeFoliageColor,
            leafScale = tier2Growth,
            isLeft = true
        )

        drawBranchWithFoliage(
            origin = branch2Origin.copy(x = branch2Origin.x + 1f),
            length = (currentHeight * 0.28f) * tier2Growth,
            angleDeg = 36f - params.branchAngleOffset * 0.5f,
            wind = wind * 0.8f,
            barkColor = activeBarkColor,
            foliageColor = activeFoliageColor,
            leafScale = tier2Growth,
            isLeft = false
        )
    }

    // Crown Foliage Canopy (starts developing at 65% and finishes at 100%)
    if (progress > 0.62f) {
        val crownGrowth = ((progress - 0.62f) / 0.38f).coerceIn(0f, 1f)
        drawCanopyCrown(
            topCenter = trunkTop,
            scale = crownGrowth,
            wind = wind,
            color = activeFoliageColor,
            highlightColor = if (isWithered) palette.witherFoliage else palette.foliageHighlight,
            glow = glow
        )
    }
}

/**
 * Draws embryonic cotyledon seedling leaves at the shoot tip.
 */
private fun DrawScope.drawCotyledonLeaves(
    stemTip: Offset,
    growth: Float,
    wind: Float,
    color: Color
) {
    val leafLen = 14f * growth
    val leafWid = 9f * growth

    // Left Leaf
    val leftCenter = Offset(stemTip.x - leafLen * 0.7f + wind, stemTip.y - leafLen * 0.3f)
    drawOval(
        color = color,
        topLeft = Offset(leftCenter.x - leafWid / 2f, leftCenter.y - leafWid / 2f),
        size = Size(leafLen, leafWid)
    )

    // Right Leaf
    val rightCenter = Offset(stemTip.x + leafLen * 0.7f + wind, stemTip.y - leafLen * 0.3f)
    drawOval(
        color = color,
        topLeft = Offset(rightCenter.x - leafWid / 2f, rightCenter.y - leafWid / 2f),
        size = Size(leafLen, leafWid)
    )
}

/**
 * Draws a tapering branch with organic leaf clusters attached at its extremity.
 */
private fun DrawScope.drawBranchWithFoliage(
    origin: Offset,
    length: Float,
    angleDeg: Float,
    wind: Float,
    barkColor: Color,
    foliageColor: Color,
    leafScale: Float,
    isLeft: Boolean
) {
    if (length <= 2f) return

    val rad = (angleDeg * PI / 180f).toFloat()
    val tipX = origin.x + sin(rad) * length + wind
    val tipY = origin.y - cos(rad) * length
    val tip = Offset(tipX, tipY)

    // Draw branch stem
    drawLine(
        color = barkColor,
        start = origin,
        end = tip,
        strokeWidth = (2.8f * leafScale).coerceAtLeast(1.2f),
        cap = StrokeCap.Round
    )

    // Draw leaf cluster at branch tip
    if (leafScale > 0.25f) {
        val clusterRadius = (16f * leafScale).coerceAtLeast(4f)
        drawCircle(
            color = foliageColor.copy(alpha = 0.90f),
            radius = clusterRadius,
            center = tip
        )
        // Secondary subtle leaf overlay
        val offsetDir = if (isLeft) -4f else 4f
        drawCircle(
            color = foliageColor.copy(alpha = 0.75f),
            radius = clusterRadius * 0.75f,
            center = Offset(tip.x + offsetDir, tip.y - 4f)
        )
    }
}

/**
 * Draws the lush majestic crown canopy at the peak of the tree.
 */
private fun DrawScope.drawCanopyCrown(
    topCenter: Offset,
    scale: Float,
    wind: Float,
    color: Color,
    highlightColor: Color,
    glow: Float
) {
    val baseRadius = 24f * scale

    // Central primary crown
    drawCircle(
        color = color,
        radius = baseRadius,
        center = Offset(topCenter.x + wind * 0.5f, topCenter.y - baseRadius * 0.4f)
    )

    // Left flourishing lobe
    drawCircle(
        color = color.copy(alpha = 0.92f),
        radius = baseRadius * 0.85f,
        center = Offset(topCenter.x - baseRadius * 0.65f + wind * 0.4f, topCenter.y - baseRadius * 0.2f)
    )

    // Right flourishing lobe
    drawCircle(
        color = color.copy(alpha = 0.92f),
        radius = baseRadius * 0.85f,
        center = Offset(topCenter.x + baseRadius * 0.65f + wind * 0.4f, topCenter.y - baseRadius * 0.2f)
    )

    // Highlight sun/moon kissed top dome
    drawCircle(
        color = highlightColor.copy(alpha = 0.75f),
        radius = baseRadius * 0.60f,
        center = Offset(topCenter.x + wind * 0.6f, topCenter.y - baseRadius * 0.75f)
    )

    // Mature glow at 100%
    if (scale >= 0.95f) {
        drawCircle(
            color = highlightColor.copy(alpha = 0.22f * glow),
            radius = baseRadius * 1.55f,
            center = Offset(topCenter.x + wind * 0.5f, topCenter.y - baseRadius * 0.4f)
        )
    }
}

/**
 * Draws delicate ambient pollen/spore particles floating around mature tree.
 */
private fun DrawScope.drawAmbientPollenSpores(
    centerX: Float,
    treeTopY: Float,
    sporeFactor: Float,
    phase: Float,
    isNight: Boolean,
    customColor: Color? = null
) {
    val sporeColor = if (customColor != null && customColor != Color.Unspecified) {
        customColor.copy(alpha = 0.70f)
    } else if (isNight) {
        Color(0x99B8F8D3)
    } else {
        Color(0x99FFD54F)
    }
    for (i in 0..7) {
        val angle = (i * 45f) + (phase * 20f)
        val rad = (angle * PI / 180f).toFloat()
        val dist = 32f + (i * 9f) * sporeFactor
        val sx = centerX + cos(rad) * dist
        val sy = treeTopY + sin(rad) * (dist * 0.65f) - (sporeFactor * 12f)
        drawCircle(
            color = sporeColor,
            radius = 1.8f + (i % 2),
            center = Offset(sx, sy)
        )
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

/**
 * Stage 1: Love Seed — A special romantic seed with heart-tapered contours and soft inner glow.
 */
private fun DrawScope.drawGerminatingLoveSeed(
    centerX: Float,
    groundY: Float,
    progress: Float,
    glowPulse: Float,
    alpha: Float,
    isNight: Boolean,
    palette: com.example.domain.tree.TreeVisualPalette
) {
    val seedColor = if (isNight) palette.seedColor.copy(alpha = 0.9f) else palette.seedColor
    val glowColor = palette.seedGlow

    val seedTwitch = if (progress in 0.05f..0.18f) sin(progress * 80f) * 1.5f else 0f
    val seedCenter = Offset(centerX + seedTwitch, groundY - 4f)
    val seedRadiusX = 10f
    val seedRadiusY = 14f

    // Soft romantic vitality aura
    if (progress > 0.04f) {
        val auraAlpha = ((progress - 0.04f) / 0.20f).coerceIn(0f, 1f) * 0.40f * alpha
        drawCircle(
            color = glowColor.copy(alpha = auraAlpha),
            radius = seedRadiusY * 2.4f * glowPulse,
            center = seedCenter
        )
    }

    // Heart-contoured seed coat
    val seedPath = Path().apply {
        moveTo(seedCenter.x, seedCenter.y + seedRadiusY * 0.85f)
        // Left lobe curve
        cubicTo(
            seedCenter.x - seedRadiusX * 1.2f, seedCenter.y + seedRadiusY * 0.3f,
            seedCenter.x - seedRadiusX * 1.1f, seedCenter.y - seedRadiusY * 0.8f,
            seedCenter.x, seedCenter.y - seedRadiusY * 0.4f
        )
        // Right lobe curve
        cubicTo(
            seedCenter.x + seedRadiusX * 1.1f, seedCenter.y - seedRadiusY * 0.8f,
            seedCenter.x + seedRadiusX * 1.2f, seedCenter.y + seedRadiusY * 0.3f,
            seedCenter.x, seedCenter.y + seedRadiusY * 0.85f
        )
        close()
    }
    drawPath(seedPath, color = seedColor.copy(alpha = alpha))

    // Delicate fissure of inner pink-rose life (8% - 30%)
    if (progress > 0.08f) {
        val crackProgress = ((progress - 0.08f) / 0.18f).coerceIn(0f, 1f)
        val crackPath = Path().apply {
            moveTo(seedCenter.x, seedCenter.y - seedRadiusY * 0.35f)
            lineTo(seedCenter.x - 2f, seedCenter.y - seedRadiusY * 0.05f)
            lineTo(seedCenter.x + 2f, seedCenter.y + seedRadiusY * 0.25f)
            lineTo(seedCenter.x, seedCenter.y + seedRadiusY * 0.65f * crackProgress)
        }
        drawPath(
            path = crackPath,
            color = palette.foliageHighlight.copy(alpha = alpha),
            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Organic growth engine for the Tree of LOVE (tree_love).
 * Branches, foliage, and canopy naturally grow into a magnificent, large heart ♥.
 */
private fun DrawScope.drawLivingLoveTreeAboveGround(
    centerX: Float,
    groundY: Float,
    width: Float,
    height: Float,
    progress: Float,
    wind: Float,
    glow: Float,
    params: TreeGenParams,
    isNight: Boolean,
    palette: com.example.domain.tree.TreeVisualPalette,
    isWithered: Boolean
) {
    val growthFraction = ((progress - 0.28f) / 0.72f).coerceIn(0.08f, 1.0f)
    val maxTreeHeight = height * 0.50f
    val currentHeight = maxTreeHeight * growthFraction

    // Bark and Foliage color transitions
    val stemWoodProgress = ((progress - 0.42f) / 0.52f).coerceIn(0f, 1f)
    val barkColor = if (isWithered) palette.witherBark else lerpColor(palette.barkTender, palette.barkTone, stemWoodProgress)
    val foliageBase = if (isWithered) palette.witherFoliage else lerpColor(palette.secondaryFoliage, palette.primaryFoliage, stemWoodProgress)
    val foliageHighlight = if (isWithered) Color(0xFF5D4037) else palette.foliageHighlight
    val deepShadowFoliage = if (isWithered) Color(0xFF3E2723) else Color(0xFF880E4F)

    // Fallen withered petals on soil if interrupted
    if (isWithered) {
        val dryPetalColor = palette.witherFoliage.copy(alpha = 0.75f)
        drawCircle(color = dryPetalColor, radius = 3.5f, center = Offset(centerX - 16f, groundY + 3f))
        drawCircle(color = dryPetalColor, radius = 3.0f, center = Offset(centerX + 22f, groundY + 5f))
        drawCircle(color = dryPetalColor, radius = 2.8f, center = Offset(centerX - 8f, groundY + 7f))
        drawCircle(color = dryPetalColor, radius = 3.2f, center = Offset(centerX + 12f, groundY + 4f))
    }

    // Stage 2: Love Sprout (progress 28% to 42%)
    if (progress <= 0.42f) {
        val sproutGrowth = ((progress - 0.28f) / 0.14f).coerceIn(0f, 1f)
        val sproutH = 34f * sproutGrowth
        val sway = wind * 0.5f

        // Tender twin curving shoots that arch toward each other like an embryonic baby heart
        val leftShoot = Path().apply {
            moveTo(centerX - 2f, groundY)
            cubicTo(
                centerX - 10f * sproutGrowth + sway, groundY - sproutH * 0.4f,
                centerX - 12f * sproutGrowth + sway, groundY - sproutH * 0.85f,
                centerX - 1f + sway, groundY - sproutH
            )
        }
        val rightShoot = Path().apply {
            moveTo(centerX + 2f, groundY)
            cubicTo(
                centerX + 10f * sproutGrowth + sway, groundY - sproutH * 0.4f,
                centerX + 12f * sproutGrowth + sway, groundY - sproutH * 0.85f,
                centerX + 1f + sway, groundY - sproutH
            )
        }
        val sproutStroke = if (isWithered) 2.0f else 2.6f
        drawPath(leftShoot, color = barkColor, style = Stroke(width = sproutStroke, cap = StrokeCap.Round))
        drawPath(rightShoot, color = barkColor, style = Stroke(width = sproutStroke, cap = StrokeCap.Round))

        // Baby rose leaves at shoot tips
        val leafSize = (8f * sproutGrowth).coerceAtLeast(2f)
        val leftTip = Offset(centerX - 1f + sway, groundY - sproutH)
        val rightTip = Offset(centerX + 1f + sway, groundY - sproutH)

        val droopOffset = if (isWithered) 6f else 0f
        drawOval(
            color = foliageBase,
            topLeft = Offset(leftTip.x - leafSize * 1.2f, leftTip.y - leafSize * 0.5f + droopOffset),
            size = Size(leafSize * 1.4f, leafSize * 0.9f)
        )
        drawOval(
            color = foliageBase,
            topLeft = Offset(rightTip.x - leafSize * 0.2f, rightTip.y - leafSize * 0.5f + droopOffset),
            size = Size(leafSize * 1.4f, leafSize * 0.9f)
        )
        return
    }

    // Above-ground living tree structure for Stages 3, 4, 5
    val trunkBaseWidth = 5f + (14f * stemWoodProgress)
    val splitY = groundY - (currentHeight * 0.34f)
    val trunkTop = Offset(centerX + (wind * currentHeight * 0.04f), splitY)

    // 1. Natural Elegant Main Trunk
    val trunkPath = Path().apply {
        moveTo(centerX - trunkBaseWidth / 2f, groundY)
        quadraticTo(
            centerX - trunkBaseWidth * 0.35f, groundY - (currentHeight * 0.17f),
            trunkTop.x - trunkBaseWidth * 0.22f, trunkTop.y
        )
        lineTo(trunkTop.x + trunkBaseWidth * 0.22f, trunkTop.y)
        quadraticTo(
            centerX + trunkBaseWidth * 0.35f, groundY - (currentHeight * 0.17f),
            centerX + trunkBaseWidth / 2f, groundY
        )
        close()
    }
    drawPath(trunkPath, color = barkColor)

    // Branch width scaling
    val branchThick = (3.5f + (4.5f * stemWoodProgress)).coerceAtLeast(1.8f)

    // Stage 3, 4, 5: Symmetrical Heart-Forming Boughs
    // Crown center where the heart is positioned
    val heartCrownH = currentHeight * 0.66f
    val heartCrownW = (currentHeight * 0.72f).coerceAtLeast(40f)
    val heartCenterY = splitY - (heartCrownH * 0.45f)
    val heartTopY = splitY - heartCrownH
    val sway = wind * (currentHeight * 0.07f)

    // Wither droop factor: branches droop downward if session was interrupted
    val droop = if (isWithered) 12f * growthFraction else 0f

    // LEFT PRIMARY BOUGH (forms the left curve and upper left lobe of heart)
    val leftBoughPath = Path().apply {
        moveTo(trunkTop.x, trunkTop.y)
        // Curves outward wide to the left, arcs up into the top left lobe, and curls back toward top center
        cubicTo(
            centerX - heartCrownW * 0.48f + (sway * 0.6f), splitY - heartCrownH * 0.15f + droop,
            centerX - heartCrownW * 0.52f + sway, heartCenterY - heartCrownH * 0.10f + droop,
            centerX - heartCrownW * 0.25f + sway, heartTopY + (heartCrownH * 0.06f) + droop
        )
    }
    drawPath(
        path = leftBoughPath,
        color = barkColor,
        style = Stroke(width = branchThick, cap = StrokeCap.Round)
    )

    // RIGHT PRIMARY BOUGH (forms the right curve and upper right lobe of heart)
    val rightBoughPath = Path().apply {
        moveTo(trunkTop.x, trunkTop.y)
        // Curves outward wide to the right, arcs up into the top right lobe, and curls back toward top center
        cubicTo(
            centerX + heartCrownW * 0.48f + (sway * 0.6f), splitY - heartCrownH * 0.15f + droop,
            centerX + heartCrownW * 0.52f + sway, heartCenterY - heartCrownH * 0.10f + droop,
            centerX + heartCrownW * 0.25f + sway, heartTopY + (heartCrownH * 0.06f) + droop
        )
    }
    drawPath(
        path = rightBoughPath,
        color = barkColor,
        style = Stroke(width = branchThick, cap = StrokeCap.Round)
    )

    // Lower interior support branches filling the bottom "V" of the heart
    if (progress > 0.50f) {
        val innerGrowth = ((progress - 0.50f) / 0.50f).coerceIn(0f, 1f)
        val leftInner = Path().apply {
            moveTo(trunkTop.x, trunkTop.y - 2f)
            quadraticTo(
                centerX - heartCrownW * 0.22f * innerGrowth + sway * 0.4f,
                splitY - heartCrownH * 0.35f + droop,
                centerX - heartCrownW * 0.10f * innerGrowth + sway * 0.5f,
                heartCenterY + droop
            )
        }
        val rightInner = Path().apply {
            moveTo(trunkTop.x, trunkTop.y - 2f)
            quadraticTo(
                centerX + heartCrownW * 0.22f * innerGrowth + sway * 0.4f,
                splitY - heartCrownH * 0.35f + droop,
                centerX + heartCrownW * 0.10f * innerGrowth + sway * 0.5f,
                heartCenterY + droop
            )
        }
        drawPath(leftInner, color = barkColor, style = Stroke(width = branchThick * 0.7f, cap = StrokeCap.Round))
        drawPath(rightInner, color = barkColor, style = Stroke(width = branchThick * 0.7f, cap = StrokeCap.Round))
    }

    // 2. Heart Blossom Canopy
    // Sample points along an organic parametric heart curve to populate lush blossom clusters
    // Parametric heart formula:
    // x = 16 sin^3(t) / 16  [-1..1]
    // y = -(13 cos(t) - 5 cos(2t) - 2 cos(3t) - cos(4t)) / 17  [-0.95..1]
    // Scale the blossom clusters across Stages 3, 4, 5
    val foliageDensity = when {
        progress >= 0.85f -> 1.0f  // Stage 5: Full Mature Big Heart
        progress >= 0.60f -> 0.70f // Stage 4: Growing Heart
        else -> 0.40f              // Stage 3: Young Heart
    }

    val clusterScale = (heartCrownW * 0.14f * foliageDensity).coerceAtLeast(7f)

    // Soft radiant aura behind mature heart tree
    if (progress > 0.80f && !isWithered) {
        val auraAlpha = ((progress - 0.80f) / 0.20f) * 0.28f * glow
        drawCircle(
            color = palette.foliageHighlight.copy(alpha = auraAlpha),
            radius = heartCrownW * 0.58f,
            center = Offset(centerX + sway, heartCenterY - heartCrownH * 0.08f)
        )
    }

    // Parametric sample angles along the heart silhouette: t from -PI to PI
    val heartSamples = listOf(
        -2.9f, -2.5f, -2.1f, -1.7f, -1.3f, -0.9f, -0.5f, -0.15f,
        0.15f, 0.5f, 0.9f, 1.3f, 1.7f, 2.1f, 2.5f, 2.9f
    )

    // Layer 1: Deep shadow blossoms underneath for organic volume & depth
    for (t in heartSamples) {
        val sinT = sin(t)
        val cosT = cos(t)
        val sin3 = sinT * sinT * sinT
        val normX = sin3
        val normY = -(13f * cosT - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)) / 17f

        val hx = centerX + normX * (heartCrownW * 0.50f) + sway * (0.5f + normY * 0.3f)
        val hy = heartCenterY + normY * (heartCrownH * 0.46f) + droop

        drawCircle(
            color = deepShadowFoliage.copy(alpha = 0.85f),
            radius = clusterScale * 1.05f,
            center = Offset(hx, hy + 2f)
        )
    }

    // Layer 2: Main vibrant rose blossoms along the heart perimeter
    for (t in heartSamples) {
        val sinT = sin(t)
        val cosT = cos(t)
        val sin3 = sinT * sinT * sinT
        val normX = sin3
        val normY = -(13f * cosT - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)) / 17f

        val hx = centerX + normX * (heartCrownW * 0.50f) + sway * (0.5f + normY * 0.3f)
        val hy = heartCenterY + normY * (heartCrownH * 0.46f) + droop

        drawCircle(
            color = foliageBase.copy(alpha = 0.92f),
            radius = clusterScale,
            center = Offset(hx, hy)
        )

        // Delicate petals highlight
        if (progress > 0.65f) {
            val hHighlightX = hx + (if (normX < 0) -2f else 2f)
            drawCircle(
                color = foliageHighlight.copy(alpha = if (isWithered) 0.4f else 0.75f),
                radius = clusterScale * 0.52f,
                center = Offset(hHighlightX, hy - clusterScale * 0.28f)
            )
        }
    }

    // Layer 3: Interior filling clusters (creates lush living body without empty gap)
    if (progress > 0.55f) {
        val interiorPoints = listOf(
            Offset(-0.25f, -0.35f), Offset(0.25f, -0.35f),
            Offset(-0.28f, 0.05f), Offset(0.28f, 0.05f),
            Offset(0.0f, -0.10f), Offset(0.0f, 0.25f),
            Offset(-0.15f, 0.45f), Offset(0.15f, 0.45f)
        )
        for (pt in interiorPoints) {
            val ix = centerX + pt.x * heartCrownW * 0.46f + sway * 0.6f
            val iy = heartCenterY + pt.y * heartCrownH * 0.42f + droop
            drawCircle(
                color = foliageBase.copy(alpha = 0.94f),
                radius = clusterScale * 1.15f,
                center = Offset(ix, iy)
            )
            if (progress > 0.75f && !isWithered) {
                drawCircle(
                    color = foliageHighlight.copy(alpha = 0.60f),
                    radius = clusterScale * 0.55f,
                    center = Offset(ix, iy - 2f)
                )
            }
        }
    }

    // Top center heart indentation blossom accent (ensures top cleft is beautifully defined)
    val cleftX = centerX + sway * 0.8f
    val cleftY = heartCenterY - (heartCrownH * 0.20f) + droop
    drawCircle(
        color = deepShadowFoliage.copy(alpha = 0.70f),
        radius = clusterScale * 0.70f,
        center = Offset(cleftX, cleftY)
    )
}

/**
 * Draws floating rose blossom petals and romantic heart spores around mature Tree of LOVE.
 */
private fun DrawScope.drawAmbientLovePetals(
    centerX: Float,
    treeTopY: Float,
    sporeFactor: Float,
    phase: Float,
    isNight: Boolean,
    color: Color
) {
    val petalColor = color.copy(alpha = 0.80f)
    for (i in 0..8) {
        val angle = (i * 40f) + (phase * 28f)
        val rad = (angle * PI / 180f).toFloat()
        val dist = 36f + (i * 10f) * sporeFactor
        val px = centerX + cos(rad) * dist
        val py = treeTopY + sin(rad) * (dist * 0.55f) - (sporeFactor * 14f)

        // Draw delicate floating petal (tilted oval with petal contour)
        drawOval(
            color = petalColor,
            topLeft = Offset(px - 3.5f, py - 2.5f),
            size = Size(7f, 5f)
        )
    }
}
