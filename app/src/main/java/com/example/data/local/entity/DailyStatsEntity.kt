package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val dateKey: String, // "YYYY-MM-DD"
    val focusedMinutes: Int = 0,
    val completedSessions: Int = 0,
    val interruptedSessions: Int = 0,
    val treesGrown: Int = 0,
    val placementSessions: Int = 0,
    val movementInterruptedCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
