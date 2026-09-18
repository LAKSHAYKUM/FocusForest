package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forest_progress")
data class ForestProgressEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalXP: Int = 0,
    val totalFocusMinutes: Long = 0L,
    val treeCount: Int = 0,
    val forestLevel: Int = 1,
    val currentGrowth: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastFocusDate: String = "", // "YYYY-MM-DD"
    val updatedAt: Long = System.currentTimeMillis()
)
