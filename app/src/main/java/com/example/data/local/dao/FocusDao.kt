package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.ForestProgressEntity
import com.example.data.local.entity.TreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    // Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status = :status ORDER BY startTime DESC")
    fun getSessionsByStatusFlow(status: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE mode = :mode ORDER BY startTime DESC")
    fun getSessionsByModeFlow(mode: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getCompletedSessionCountFlow(): Flow<Int>

    // Trees
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: TreeEntity): Long

    @Query("SELECT * FROM trees ORDER BY createdAt DESC")
    fun getAllTreesFlow(): Flow<List<TreeEntity>>

    @Query("SELECT COUNT(*) FROM trees")
    fun getTreeCountFlow(): Flow<Int>

    // Forest Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProgress(progress: ForestProgressEntity)

    @Query("SELECT * FROM forest_progress WHERE id = 1 LIMIT 1")
    fun getProgressFlow(): Flow<ForestProgressEntity?>

    @Query("SELECT * FROM forest_progress WHERE id = 1 LIMIT 1")
    suspend fun getProgressOnce(): ForestProgressEntity?

    // Daily Stats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStats(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyStats(dateKey: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE dateKey = :dateKey LIMIT 1")
    fun getDailyStatsFlow(dateKey: String): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats ORDER BY dateKey DESC LIMIT 30")
    fun getRecentDailyStatsFlow(): Flow<List<DailyStatsEntity>>

    // Achievements
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, id ASC")
    fun getAllAchievementsFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Query("DELETE FROM focus_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM trees")
    suspend fun clearTrees()

    @Query("DELETE FROM daily_stats")
    suspend fun clearDailyStats()
}
