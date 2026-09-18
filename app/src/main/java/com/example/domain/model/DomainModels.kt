package com.example.domain.model

data class FocusSession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long,
    val mode: FocusMode,
    val status: String,
    val movementEvents: Int = 0,
    val treeGrowthLevel: Int = 1,
    val earnedXP: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Tree(
    val id: Long = 0,
    val growthLevel: Int,
    val growthXP: Int,
    val sessionDurationMinutes: Int,
    val createdFromSessionId: Long,
    val species: String = "PINE",
    val createdAt: Long = System.currentTimeMillis()
)

data class ForestProgress(
    val totalXP: Int = 0,
    val totalFocusMinutes: Long = 0L,
    val treeCount: Int = 0,
    val forestLevel: Int = 1,
    val currentGrowth: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastFocusDate: String = ""
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val xpReward: Int = 50
)
