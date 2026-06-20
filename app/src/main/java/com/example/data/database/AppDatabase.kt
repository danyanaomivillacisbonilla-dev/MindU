package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        AcademicResourceEntity::class,
        EmotionalRecordEntity::class,
        DiaryEntryEntity::class,
        ClinicalCaseEntity::class,
        SimulationHistoryEntity::class,
        UnlockedBadgeEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun academicResourceDao(): AcademicResourceDao
    abstract fun emotionalRecordDao(): EmotionalRecordDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun clinicalCaseDao(): ClinicalCaseDao
    abstract fun simulationHistoryDao(): SimulationHistoryDao
    abstract fun unlockedBadgeDao(): UnlockedBadgeDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindu_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
