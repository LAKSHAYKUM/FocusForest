package com.example.domain.repository

import com.example.data.local.entity.DailyStatsEntity
import com.example.domain.model.Achievement
import com.example.domain.model.FocusMode
import com.example.domain.model.FocusSession
import com.example.domain.model.ForestProgress
import com.example.domain.model.Tree
import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    fun getAllSessions(): Flow<List<FocusSession>>
    fun getSessionsByFilter(filter: String): Flow<List<FocusSession>>
    fun getAllTrees(): Flow<List<Tree>>
    fun getForestProgress(): Flow<ForestProgress>
    fun getTodayStats(): Flow<DailyStatsEntity?>
    fun getRecentStats(): Flow<List<DailyStatsEntity>>
    fun getAchievements(): Flow<List<Achievement>>

    suspend fun recordCompletedSession(
        startTime: Long,
        endTime: Long,
        plannedMinutes: Int,
        actualSeconds: Long,
        mode: FocusMode,
        movementEvents: Int,
        treeSpecies: String = "PINE"
    ): Pair<FocusSession, Tree>

    suspend fun recordInterruptedSession(
        startTime: Long,
        endTime: Long,
        plannedMinutes: Int,
        actualSeconds: Long,
        mode: FocusMode,
        movementEvents: Int,
        treeSpecies: String = "PINE"
    ): FocusSession

    suspend fun clearAllData()
}
