package com.sharn.handpan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    @Query("SELECT * FROM patterns WHERE isCustom = 1 ORDER BY createdAt DESC")
    fun getCustomPatterns(): Flow<List<PatternEntity>>

    @Query("SELECT * FROM patterns WHERE id = :id LIMIT 1")
    suspend fun getPatternById(id: String): PatternEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: PatternEntity)

    @Query("DELETE FROM patterns WHERE id = :id")
    suspend fun deletePatternById(id: String)

    @Query("SELECT COUNT(*) FROM patterns WHERE isCustom = 1")
    fun getCustomPatternCount(): Flow<Int>
}
