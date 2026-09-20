package com.example.data.repository

import com.example.data.local.dao.FocusDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.ForestProgressEntity
import com.example.data.local.entity.TreeEntity
import com.example.domain.model.Achievement
import com.example.domain.model.FocusMode
import com.example.domain.model.FocusSession
import com.example.domain.model.ForestProgress
import com.example.domain.model.Tree
import com.example.domain.model.TreeStage
import com.example.domain.repository.FocusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FocusRepositoryImpl(
    private val focusDao: FocusDao
) : FocusRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun getAllSessions(): Flow<List<FocusSession>> {
        return focusDao.getAllSessionsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSessionsByFilter(filter: String): Flow<List<FocusSession>> {
        return when (filter.uppercase()) {
            "COMPLETED" -> focusDao.getSessionsByStatusFlow("COMPLETED").map { l -> l.map { it.toDomain() } }
            "INTERRUPTED" -> focusDao.getSessionsByStatusFlow("INTERRUPTED").map { l -> l.map { it.toDomain() } }
            "PLACEMENT" -> focusDao.getSessionsByModeFlow("PLACEMENT").map { l -> l.map { it.toDomain() } }
            "STANDARD" -> focusDao.getSessionsByModeFlow("STANDARD").map { l -> l.map { it.toDomain() } }
            else -> getAllSessions()
        }
    }

    override fun getAllTrees(): Flow<List<Tree>> {
        return focusDao.getAllTreesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getForestProgress(): Flow<ForestProgress> {
        return focusDao.getProgressFlow().map { entity ->
            entity?.toDomain() ?: ForestProgress(
                totalXP = 0,
                totalFocusMinutes = 0,
                treeCount = 0,
                forestLevel = 1,
                currentGrowth = 0,
                currentStreakDays = 0,
                bestStreakDays = 0
            )
        }
    }

    override fun getTodayStats(): Flow<DailyStatsEntity?> {
        val todayKey = dateFormat.format(Date())
        return focusDao.getDailyStatsFlow(todayKey)
    }

    override fun getRecentStats(): Flow<List<DailyStatsEntity>> {
        return focusDao.getRecentDailyStatsFlow()
    }

    override fun getAchievements(): Flow<List<Achievement>> {
        return focusDao.getAllAchievementsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordCompletedSession(
        startTime: Long,
        endTime: Long,
        plannedMinutes: Int,
        actualSeconds: Long,
        mode: FocusMode,
        movementEvents: Int,
        treeSpecies: String
    ): Pair<FocusSession, Tree> = withContext(Dispatchers.IO) {
        val treeStage = TreeStage.fromDuration(plannedMinutes)
        val earnedXP = treeStage.xpValue + if (mode == FocusMode.PLACEMENT) 30 else 0
        val actualMinutes = (actualSeconds / 60).coerceAtLeast(1L).toInt()

        // 1. Insert Session
        val sessionEntity = FocusSessionEntity(
            startTime = startTime,
            endTime = endTime,
            plannedDurationMinutes = plannedMinutes,
            actualDurationSeconds = actualSeconds,
            mode = mode.name,
            status = "COMPLETED",
            movementEvents = movementEvents,
            treeGrowthLevel = treeStage.level,
            earnedXP = earnedXP
        )
        val sessionId = focusDao.insertSession(sessionEntity)

        // 2. Insert Tree
        val species = if (treeSpecies.isNotBlank() && treeSpecies != "PINE" && treeSpecies != "tree_default") {
            treeSpecies
        } else {
            when (treeStage) {
                TreeStage.SEED, TreeStage.SPROUT -> "PINE"
                TreeStage.YOUNG_TREE -> "OAK"
                TreeStage.MATURE_TREE -> "CEDAR"
                TreeStage.LARGE_TREE -> "ANCIENT"
            }
        }
        val treeEntity = TreeEntity(
            growthLevel = treeStage.level,
            growthXP = earnedXP,
            sessionDurationMinutes = plannedMinutes,
            createdFromSessionId = sessionId,
            species = species
        )
        val treeId = focusDao.insertTree(treeEntity)

        // 3. Update Progress & Streaks
        updateForestProgressAndStats(actualMinutes, earnedXP, isCompleted = true, isPlacement = mode == FocusMode.PLACEMENT, movementEvents = movementEvents)

        val savedSession = sessionEntity.copy(id = sessionId).toDomain()
        val savedTree = treeEntity.copy(id = treeId).toDomain()
        Pair(savedSession, savedTree)
    }

    override suspend fun recordInterruptedSession(
        startTime: Long,
        endTime: Long,
        plannedMinutes: Int,
        actualSeconds: Long,
        mode: FocusMode,
        movementEvents: Int,
        treeSpecies: String
    ): FocusSession = withContext(Dispatchers.IO) {
        val actualMinutes = (actualSeconds / 60).toInt()
        val earnedXP = (actualMinutes * 2).coerceAtLeast(5)

        val sessionEntity = FocusSessionEntity(
            startTime = startTime,
            endTime = endTime,
            plannedDurationMinutes = plannedMinutes,
            actualDurationSeconds = actualSeconds,
            mode = mode.name,
            status = "INTERRUPTED",
            movementEvents = movementEvents,
            treeGrowthLevel = 1,
            earnedXP = earnedXP
        )
        val sessionId = focusDao.insertSession(sessionEntity)

        updateForestProgressAndStats(actualMinutes, earnedXP, isCompleted = false, isPlacement = mode == FocusMode.PLACEMENT, movementEvents = movementEvents)

        sessionEntity.copy(id = sessionId).toDomain()
    }

    private suspend fun updateForestProgressAndStats(
        focusedMinutes: Int,
        earnedXP: Int,
        isCompleted: Boolean,
        isPlacement: Boolean,
        movementEvents: Int
    ) {
        val todayKey = dateFormat.format(Date())

        // 1. Daily Stats
        val existingDaily = focusDao.getDailyStats(todayKey) ?: DailyStatsEntity(dateKey = todayKey)
        val updatedDaily = existingDaily.copy(
            focusedMinutes = existingDaily.focusedMinutes + focusedMinutes,
            completedSessions = existingDaily.completedSessions + if (isCompleted) 1 else 0,
            interruptedSessions = existingDaily.interruptedSessions + if (!isCompleted) 1 else 0,
            treesGrown = existingDaily.treesGrown + if (isCompleted) 1 else 0,
            placementSessions = existingDaily.placementSessions + if (isPlacement) 1 else 0,
            movementInterruptedCount = existingDaily.movementInterruptedCount + movementEvents,
            lastUpdated = System.currentTimeMillis()
        )
        focusDao.insertOrUpdateDailyStats(updatedDaily)

        // 2. Forest Progress
        val currentProgress = focusDao.getProgressOnce() ?: ForestProgressEntity(id = 1)
        val newTotalXP = currentProgress.totalXP + earnedXP
        val newTotalMinutes = currentProgress.totalFocusMinutes + focusedMinutes
        val newTreeCount = currentProgress.treeCount + if (isCompleted) 1 else 0
        val newLevel = (newTotalXP / 200) + 1
        val currentGrowth = newTotalXP % 200

        // Calculate Streak (credit streak on completed session or >= 5 min focus)
        var currentStreak = currentProgress.currentStreakDays
        var newLastDate = currentProgress.lastFocusDate

        if (isCompleted || focusedMinutes >= 5) {
            val lastDate = currentProgress.lastFocusDate
            if (lastDate.isEmpty()) {
                currentStreak = 1
            } else if (lastDate != todayKey) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayKey = dateFormat.format(cal.time)
                if (lastDate == yesterdayKey) {
                    currentStreak += 1
                } else {
                    currentStreak = 1 // Reset streak if missed day
                }
            }
            newLastDate = todayKey
        }

        val bestStreak = maxOf(currentProgress.bestStreakDays, currentStreak)

        val updatedProgress = currentProgress.copy(
            totalXP = newTotalXP,
            totalFocusMinutes = newTotalMinutes,
            treeCount = newTreeCount,
            forestLevel = newLevel,
            currentGrowth = currentGrowth,
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            lastFocusDate = newLastDate,
            updatedAt = System.currentTimeMillis()
        )
        focusDao.setProgress(updatedProgress)

        // 3. Evaluate Achievements
        checkAndUnlockAchievements(newTotalMinutes, newTreeCount, currentStreak, isPlacement)
    }

    private suspend fun checkAndUnlockAchievements(
        totalMinutes: Long,
        treeCount: Int,
        streakDays: Int,
        usedPlacement: Boolean
    ) {
        val now = System.currentTimeMillis()

        suspend fun updateAch(id: String, curProgress: Int, targetProgress: Int) {
            val ach = focusDao.getAchievementById(id) ?: return
            if (!ach.isUnlocked) {
                val newProgress = curProgress.coerceAtMost(targetProgress)
                val isUnlocked = newProgress >= targetProgress
                focusDao.updateAchievement(
                    ach.copy(
                        currentProgress = newProgress,
                        isUnlocked = isUnlocked,
                        unlockedAt = if (isUnlocked) now else null
                    )
                )
            }
        }

        if (treeCount >= 1) updateAch("first_tree", treeCount, 1)
        updateAch("first_session", 1, 1)
        updateAch("one_hour", totalMinutes.toInt(), 60)
        updateAch("five_hours", totalMinutes.toInt(), 300)
        updateAch("ten_hours", totalMinutes.toInt(), 600)
        updateAch("streak_7", streakDays, 7)
        updateAch("streak_30", streakDays, 30)
        updateAch("trees_100", treeCount, 100)
        if (usedPlacement) {
            val ach = focusDao.getAchievementById("first_placement")
            if (ach != null && !ach.isUnlocked) {
                focusDao.updateAchievement(ach.copy(currentProgress = 1, isUnlocked = true, unlockedAt = now))
            }
        }
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        focusDao.clearSessions()
        focusDao.clearTrees()
        focusDao.clearDailyStats()
        focusDao.setProgress(
            ForestProgressEntity(
                id = 1,
                totalXP = 0,
                totalFocusMinutes = 0,
                treeCount = 0,
                forestLevel = 1,
                currentGrowth = 0,
                currentStreakDays = 0,
                bestStreakDays = 0
            )
        )
    }

    private fun FocusSessionEntity.toDomain(): FocusSession {
        return FocusSession(
            id = id,
            startTime = startTime,
            endTime = endTime,
            plannedDurationMinutes = plannedDurationMinutes,
            actualDurationSeconds = actualDurationSeconds,
            mode = if (mode == "PLACEMENT") FocusMode.PLACEMENT else FocusMode.STANDARD,
            status = status,
            movementEvents = movementEvents,
            treeGrowthLevel = treeGrowthLevel,
            earnedXP = earnedXP,
            note = note,
            createdAt = createdAt
        )
    }

    private fun TreeEntity.toDomain(): Tree {
        return Tree(
            id = id,
            growthLevel = growthLevel,
            growthXP = growthXP,
            sessionDurationMinutes = sessionDurationMinutes,
            createdFromSessionId = createdFromSessionId,
            species = species,
            createdAt = createdAt
        )
    }

    private fun ForestProgressEntity.toDomain(): ForestProgress {
        return ForestProgress(
            totalXP = totalXP,
            totalFocusMinutes = totalFocusMinutes,
            treeCount = treeCount,
            forestLevel = forestLevel,
            currentGrowth = currentGrowth,
            currentStreakDays = currentStreakDays,
            bestStreakDays = bestStreakDays,
            lastFocusDate = lastFocusDate
        )
    }

    private fun AchievementEntity.toDomain(): Achievement {
        return Achievement(
            id = id,
            title = title,
            description = description,
            iconKey = iconKey,
            isUnlocked = isUnlocked,
            unlockedAt = unlockedAt,
            currentProgress = currentProgress,
            targetProgress = targetProgress,
            xpReward = xpReward
        )
    }
}
