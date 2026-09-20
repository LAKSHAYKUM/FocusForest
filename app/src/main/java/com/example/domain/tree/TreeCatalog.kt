package com.example.domain.tree

import androidx.compose.ui.graphics.Color

/**
 * Tree growth stage identifiers
 */
enum class GrowthStage(val title: String, val minProgress: Float, val maxProgress: Float) {
    SEED("Seed", 0.0f, 0.05f),
    SPROUT("Sprout", 0.05f, 0.25f),
    YOUNG_PLANT("Young Plant", 0.25f, 0.50f),
    GROWING_TREE("Growing Tree", 0.50f, 0.75f),
    ALMOST_MATURE("Almost Mature Tree", 0.75f, 0.999f),
    MATURE("Mature Tree", 1.0f, 1.0f);

    companion object {
        fun fromProgress(progress: Float): GrowthStage {
            val p = progress.coerceIn(0f, 1f)
            return when {
                p >= 1.0f -> MATURE
                p >= 0.75f -> ALMOST_MATURE
                p >= 0.50f -> GROWING_TREE
                p >= 0.25f -> YOUNG_PLANT
                p >= 0.05f -> SPROUT
                else -> SEED
            }
        }
    }
}

/**
 * Withered stage based on interrupted session progress
 */
enum class WitherStage(val title: String) {
    WITHERED_SPROUT("Wilted Sprout"),
    WITHERED_YOUNG_PLANT("Wilted Young Plant"),
    WITHERED_GROWING_TREE("Partially Withered Tree"),
    WITHERED_MATURE("Damaged Near-Mature Tree");

    companion object {
        fun fromProgress(progress: Float): WitherStage {
            val p = progress.coerceIn(0f, 1f)
            return when {
                p >= 0.75f -> WITHERED_MATURE
                p >= 0.45f -> WITHERED_GROWING_TREE
                p >= 0.20f -> WITHERED_YOUNG_PLANT
                else -> WITHERED_SPROUT
            }
        }
    }
}

/**
 * Distinct visual theme definition for each tree in the FocusForest universe.
 */
data class TreeVisualPalette(
    val primaryFoliage: Color,
    val secondaryFoliage: Color,
    val foliageHighlight: Color,
    val barkTone: Color,
    val barkTender: Color,
    val seedColor: Color,
    val seedGlow: Color,
    val auraColor: Color,
    val witherFoliage: Color,
    val witherBark: Color,
    val isBlossom: Boolean = false,
    val isGlowing: Boolean = false,
    val customParticleColor: Color? = null
)

/**
 * Model representing a species in the Tree Collection.
 */
data class TreeCatalogItem(
    val id: String,
    val name: String,
    val titleDescription: String,
    val poeticDescription: String,
    val unlockMethod: String, // "FREE", "CODE"
    val codePrefix: String,
    val palette: TreeVisualPalette
)

/**
 * Centralized Tree Catalog containing all 8 distinct tree species.
 */
object TreeCatalog {
    const val DEFAULT_TREE_ID = "tree_default"

    val DEFAULT_TREE = TreeCatalogItem(
        id = DEFAULT_TREE_ID,
        name = "Default Tree",
        titleDescription = "Natural Green Forest Tree",
        poeticDescription = "Simple, calm, classic FocusForest appearance. A resilient pine that embodies steady, patient concentration.",
        unlockMethod = "FREE",
        codePrefix = "NONE",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFF2E7D32),
            secondaryFoliage = Color(0xFF43A047),
            foliageHighlight = Color(0xFF81C784),
            barkTone = Color(0xFF5D4037),
            barkTender = Color(0xFF66BB6A),
            seedColor = Color(0xFF8D6E63),
            seedGlow = Color(0x66A5D6A7),
            auraColor = Color(0x334CAF50),
            witherFoliage = Color(0xFF795548),
            witherBark = Color(0xFF3E2723)
        )
    )

    val SAKURA_TREE = TreeCatalogItem(
        id = "tree_sakura",
        name = "Sakura Tree",
        titleDescription = "Pink Cherry Blossoms",
        poeticDescription = "Elegant winding branches with soft pink pastel petals. Brings a tranquil, mindful atmosphere to every session.",
        unlockMethod = "CODE",
        codePrefix = "SAKU",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFFF06292),
            secondaryFoliage = Color(0xFFF48FB1),
            foliageHighlight = Color(0xFFFCE4EC),
            barkTone = Color(0xFF4E342E),
            barkTender = Color(0xFFF8BBD0),
            seedColor = Color(0xFFAD1457),
            seedGlow = Color(0x88F8BBD0),
            auraColor = Color(0x44F06292),
            witherFoliage = Color(0xFF8D6E63),
            witherBark = Color(0xFF3E2723),
            isBlossom = true,
            customParticleColor = Color(0xCCF8BBD0)
        )
    )

    val GOLDEN_TREE = TreeCatalogItem(
        id = "tree_golden",
        name = "Golden Tree",
        titleDescription = "Warm Sunlight Leaves",
        poeticDescription = "Shimmering leaves bathed in perpetual warm dawn sunlight. Symbolizes deep focus and golden hours of achievement.",
        unlockMethod = "CODE",
        codePrefix = "GOLD",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFFFFB300),
            secondaryFoliage = Color(0xFFFFC107),
            foliageHighlight = Color(0xFFFFF9C4),
            barkTone = Color(0xFF6D4C41),
            barkTender = Color(0xFFFFE082),
            seedColor = Color(0xFFFF8F00),
            seedGlow = Color(0x88FFE082),
            auraColor = Color(0x44FFCA28),
            witherFoliage = Color(0xFF8D6E63),
            witherBark = Color(0xFF422F29),
            customParticleColor = Color(0xDDFFE082)
        )
    )

    val AUTUMN_TREE = TreeCatalogItem(
        id = "tree_autumn",
        name = "Autumn Tree",
        titleDescription = "Amber & Russet Foliage",
        poeticDescription = "Rich orange, red, amber, and warm rustic foliage. Reminds you of cozy seasonal afternoons of dedicated work.",
        unlockMethod = "CODE",
        codePrefix = "AUTO",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFFD84315),
            secondaryFoliage = Color(0xFFEF6C00),
            foliageHighlight = Color(0xFFFFCC80),
            barkTone = Color(0xFF4E342E),
            barkTender = Color(0xFFFFAB91),
            seedColor = Color(0xFFBF360C),
            seedGlow = Color(0x88FFAB91),
            auraColor = Color(0x44FF7043),
            witherFoliage = Color(0xFF5D4037),
            witherBark = Color(0xFF3E2723),
            customParticleColor = Color(0xCCFFAB91)
        )
    )

    val MOONLIGHT_TREE = TreeCatalogItem(
        id = "tree_moonlight",
        name = "Moonlight Tree",
        titleDescription = "Cool Nighttime Foliage",
        poeticDescription = "Moonlit silver-blue foliage in a subtle cool atmosphere. Perfect for serene late-night study and peaceful quietude.",
        unlockMethod = "CODE",
        codePrefix = "MOON",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFF3F51B5),
            secondaryFoliage = Color(0xFF5C6BC0),
            foliageHighlight = Color(0xFFC5CAE9),
            barkTone = Color(0xFF263238),
            barkTender = Color(0xFF9FA8DA),
            seedColor = Color(0xFF1A237E),
            seedGlow = Color(0x88C5CAE9),
            auraColor = Color(0x443F51B5),
            witherFoliage = Color(0xFF455A64),
            witherBark = Color(0xFF212121),
            isGlowing = true,
            customParticleColor = Color(0xCCE8EAF6)
        )
    )

    val MYSTIC_TREE = TreeCatalogItem(
        id = "tree_mystic",
        name = "Mystic Tree",
        titleDescription = "Deep Enchanted Foliage",
        poeticDescription = "Fantasy-inspired yet elegant. Deep violet-indigo foliage with subtle luminous details that spark creative flow.",
        unlockMethod = "CODE",
        codePrefix = "MYST",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFF6A1B9A),
            secondaryFoliage = Color(0xFF8E24AA),
            foliageHighlight = Color(0xFFE1BEE7),
            barkTone = Color(0xFF311B92),
            barkTender = Color(0xFFCE93D8),
            seedColor = Color(0xFF4A148C),
            seedGlow = Color(0x88E1BEE7),
            auraColor = Color(0x448E24AA),
            witherFoliage = Color(0xFF4E342E),
            witherBark = Color(0xFF1A0033),
            isGlowing = true,
            customParticleColor = Color(0xCCE1BEE7)
        )
    )

    val BLOSSOM_TREE = TreeCatalogItem(
        id = "tree_blossom",
        name = "Blossom Tree",
        titleDescription = "Vibrant Spring Petals",
        poeticDescription = "Rich colorful floral blooms with a soft, vibrant spring canopy. Celebrates fresh starts and invigorating growth.",
        unlockMethod = "CODE",
        codePrefix = "BLOS",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFFE91E63),
            secondaryFoliage = Color(0xFFFF4081),
            foliageHighlight = Color(0xFFFF80AB),
            barkTone = Color(0xFF5D4037),
            barkTender = Color(0xFFFFCDD2),
            seedColor = Color(0xFFC2185B),
            seedGlow = Color(0x88FF80AB),
            auraColor = Color(0x44E91E63),
            witherFoliage = Color(0xFF6D4C41),
            witherBark = Color(0xFF3E2723),
            isBlossom = true,
            customParticleColor = Color(0xCCFF80AB)
        )
    )

    val FOREST_SPIRIT_TREE = TreeCatalogItem(
        id = "tree_spirit",
        name = "Forest Spirit Tree",
        titleDescription = "Ancient Heart of the Forest",
        poeticDescription = "Distinct majestic silhouette rooted in ancient lore. Subtle emerald spores dance in harmony with deep dedication.",
        unlockMethod = "CODE",
        codePrefix = "SPIR",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFF00695C),
            secondaryFoliage = Color(0xFF00897B),
            foliageHighlight = Color(0xFF80CBC4),
            barkTone = Color(0xFF1B5E20),
            barkTender = Color(0xFF4DB6AC),
            seedColor = Color(0xFF004D40),
            seedGlow = Color(0x8880CBC4),
            auraColor = Color(0x4400897B),
            witherFoliage = Color(0xFF37474F),
            witherBark = Color(0xFF212121),
            isGlowing = true,
            customParticleColor = Color(0xCCB2DFDB)
        )
    )

    val TREE_OF_LOVE = TreeCatalogItem(
        id = "tree_love",
        name = "Tree of LOVE",
        titleDescription = "Grand Heart-Shaped Crown of Blossoms",
        poeticDescription = "A magnificent living tree whose organic branches and lush rose-pink blossoms grow into a large, unmistakable heart. A romantic celebration of deep concentration and devotion.",
        unlockMethod = "CODE",
        codePrefix = "LOVE",
        palette = TreeVisualPalette(
            primaryFoliage = Color(0xFFE91E63),     // Radiant romantic rose
            secondaryFoliage = Color(0xFFF06292),   // Soft blush pink
            foliageHighlight = Color(0xFFFF80AB),   // Glowing blossom highlight
            barkTone = Color(0xFF4E342E),           // Rich warm cocoa/espresso wood
            barkTender = Color(0xFF8D6E63),         // Warm caramel sapling wood
            seedColor = Color(0xFFAD1457),          // Deep ruby love seed
            seedGlow = Color(0xAAFF4081),           // Radiant romantic pink glow
            auraColor = Color(0x44E91E63),          // Warm heart halo
            witherFoliage = Color(0xFF8D6E63),      // Wilted rose dried foliage
            witherBark = Color(0xFF3E2723),         // Dry weathered bark
            isBlossom = true,
            isGlowing = true,
            customParticleColor = Color(0xDDFF4081) // Floating glowing heart petals
        )
    )

    val ALL_TREES: List<TreeCatalogItem> = listOf(
        DEFAULT_TREE,
        SAKURA_TREE,
        GOLDEN_TREE,
        AUTUMN_TREE,
        MOONLIGHT_TREE,
        MYSTIC_TREE,
        BLOSSOM_TREE,
        FOREST_SPIRIT_TREE,
        TREE_OF_LOVE
    )

    private val treeMap = ALL_TREES.associateBy { it.id }

    fun findById(id: String?): TreeCatalogItem {
        return treeMap[id] ?: DEFAULT_TREE
    }

    fun findBySpeciesOrId(speciesOrId: String?): TreeCatalogItem {
        if (speciesOrId == null) return DEFAULT_TREE
        treeMap[speciesOrId]?.let { return it }
        val normalized = speciesOrId.uppercase()
        return when {
            normalized.contains("LOVE") -> TREE_OF_LOVE
            normalized.contains("SAKURA") -> SAKURA_TREE
            normalized.contains("GOLD") -> GOLDEN_TREE
            normalized.contains("AUTUMN") || normalized.contains("MAPLE") -> AUTUMN_TREE
            normalized.contains("MOON") -> MOONLIGHT_TREE
            normalized.contains("MYSTIC") -> MYSTIC_TREE
            normalized.contains("BLOSSOM") -> BLOSSOM_TREE
            normalized.contains("SPIRIT") -> FOREST_SPIRIT_TREE
            else -> DEFAULT_TREE
        }
    }
}
