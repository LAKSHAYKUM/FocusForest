package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.FocusDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.ForestProgressEntity
import com.example.data.local.entity.TreeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FocusSessionEntity::class,
        TreeEntity::class,
        ForestProgressEntity::class,
        DailyStatsEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusForestDatabase : RoomDatabase() {
    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile
        private var INSTANCE: FocusForestDatabase? = null

        fun getInstance(context: Context): FocusForestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusForestDatabase::class.java,
                    "focus_forest.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).focusDao()
                            // Seed initial progress
                            dao.setProgress(
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
                            // Seed initial achievements
                            dao.insertAchievements(INITIAL_ACHIEVEMENTS)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val INITIAL_ACHIEVEMENTS = listOf(
            AchievementEntity("first_tree", "First Seedling", "Plant your very first tree by completing a focus session.", "park", false, null, 0, 1, 50),
            AchievementEntity("first_session", "Focus Spark", "Complete your initial focus session without interrupting.", "timer", false, null, 0, 1, 50),
            AchievementEntity("one_hour", "Hour of Zen", "Accumulate 60 minutes of uninterrupted focus.", "hourglass_full", false, null, 0, 60, 100),
            AchievementEntity("five_hours", "Canopy Builder", "Accumulate 5 hours (300 minutes) of deep work.", "nature", false, null, 0, 300, 250),
            AchievementEntity("ten_hours", "Ancient Grove", "Accumulate 10 hours (600 minutes) of deep work.", "forest", false, null, 0, 600, 500),
            AchievementEntity("streak_7", "Weekly Flow", "Maintain an uninterrupted 7-day focus streak.", "local_fire_department", false, null, 0, 7, 300),
            AchievementEntity("streak_30", "Forest Master", "Maintain an unbroken 30-day focus streak.", "workspace_premium", false, null, 0, 30, 1000),
            AchievementEntity("trees_100", "Century Forest", "Grow 100 trees in your permanent forest.", "eco", false, null, 0, 100, 1500),
            AchievementEntity("first_placement", "Physical Stillness", "Complete your first session using Placement Focus.", "phonelink_lock", false, null, 0, 1, 100),
            AchievementEntity("placement_10", "Master of Placement", "Complete 10 Placement Focus sessions successfully.", "verified_user", false, null, 0, 10, 500),
            AchievementEntity("deep_focus", "Marathon Mind", "Complete a single 60-minute focus session.", "psychology", false, null, 0, 60, 200)
        )
    }
}
