package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long,
    val mode: String, // "STANDARD" or "PLACEMENT"
    val status: String, // "COMPLETED", "INTERRUPTED", "CANCELLED"
    val movementEvents: Int = 0,
    val treeGrowthLevel: Int = 1, // 1: Seed, 2: Sprout, 3: Young, 4: Mature, 5: Large
    val earnedXP: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
