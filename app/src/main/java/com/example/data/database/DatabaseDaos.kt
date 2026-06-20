package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUser(id: Int = 1): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserSync(id: Int = 1): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface AcademicResourceDao {
    @Query("SELECT * FROM academic_resources ORDER BY timestamp DESC")
    fun getAllResources(): Flow<List<AcademicResourceEntity>>

    @Query("SELECT * FROM academic_resources WHERE id = :id")
    suspend fun getResourceById(id: Int): AcademicResourceEntity?

    @Query("SELECT * FROM academic_resources WHERE title LIKE :query OR subject LIKE :query OR description LIKE :query")
    fun searchResources(query: String): Flow<List<AcademicResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: AcademicResourceEntity)

    @Query("UPDATE academic_resources SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE academic_resources SET rating = :rating, ratingCount = :count, commentsJson = :commentsJson WHERE id = :id")
    suspend fun updateCommentsAndRating(id: Int, rating: Float, count: Int, commentsJson: String)
}

@Dao
interface EmotionalRecordDao {
    @Query("SELECT * FROM emotional_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<EmotionalRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EmotionalRecordEntity)
}

@Dao
interface DiaryEntryDao {
    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntryEntity)
}

@Dao
interface ClinicalCaseDao {
    @Query("SELECT * FROM clinical_cases")
    fun getAllCases(): Flow<List<ClinicalCaseEntity>>

    @Query("SELECT * FROM clinical_cases WHERE id = :id")
    suspend fun getCaseById(id: Int): ClinicalCaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(clinicalCase: ClinicalCaseEntity)
}

@Dao
interface SimulationHistoryDao {
    @Query("SELECT * FROM simulation_histories ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SimulationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SimulationHistoryEntity)
}

@Dao
interface UnlockedBadgeDao {
    @Query("SELECT * FROM unlocked_badges")
    fun getAllBadges(): Flow<List<UnlockedBadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: UnlockedBadgeEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}
