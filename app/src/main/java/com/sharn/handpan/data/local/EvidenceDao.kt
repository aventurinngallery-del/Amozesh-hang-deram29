package com.sharn.handpan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(evidence: EvidenceEntity): Long

    @Query("SELECT * FROM assessment_evidence WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): EvidenceEntity?
}