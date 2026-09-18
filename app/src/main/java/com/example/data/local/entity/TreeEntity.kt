package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val growthLevel: Int, // 1: Seed, 2: Sprout, 3: Young Tree, 4: Mature Tree, 5: Ancient Large Tree
    val growthXP: Int,
    val sessionDurationMinutes: Int,
    val createdFromSessionId: Long,
    val species: String = "PINE", // PINE, OAK, CEDAR, WILLOW, CHERRY
    val createdAt: Long = System.currentTimeMillis()
)
