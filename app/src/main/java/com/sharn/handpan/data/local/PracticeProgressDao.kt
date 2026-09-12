package com.sharn.handpan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeProgressDao {
    @Query("SELECT * FROM practice_progress")
    fun getAllProgress(): Flow<List<PracticeProgressEntity>>

    @Query("SELECT * FROM practice_progress WHERE patternId = :patternId LIMIT 1")
    suspend fun getProgressForPattern(patternId: String): PracticeProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PracticeProgressEntity)
}
