package com.sharn.handpan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(assessment: AssessmentEntity): Long

    @Query("SELECT * FROM assessments WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): AssessmentEntity?

    @Query("SELECT * FROM assessments ORDER BY completedAtEpochMs DESC")
    fun observeAll(): Flow<List<AssessmentEntity>>
}