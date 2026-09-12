package com.sharn.handpan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProcessedAssessmentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(session: ProcessedAssessmentEntity): Long

    @Query("SELECT sessionId FROM processed_assessments WHERE sessionId = :sessionId LIMIT 1")
    suspend fun find(sessionId: String): String?
}