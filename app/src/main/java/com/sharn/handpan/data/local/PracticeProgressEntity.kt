package com.sharn.handpan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sharn.handpan.model.PracticeProgress

@Entity(tableName = "practice_progress")
data class PracticeProgressEntity(
    @PrimaryKey
    val patternId: String,
    val practiceCount: Int,
    val lastPracticedTimestamp: Long,
    val highestBpmAchieved: Int,
    val lastUsedBpm: Int,
    val totalTimeSeconds: Int,
    val completedRounds: Int
) {
    fun toDomain() = PracticeProgress(
        patternId = patternId,
        practiceCount = practiceCount,
        lastPracticedTimestamp = lastPracticedTimestamp,
        highestBpmAchieved = highestBpmAchieved,
        lastUsedBpm = lastUsedBpm,
        totalTimeSeconds = totalTimeSeconds,
        completedRounds = completedRounds
    )

    companion object {
        fun fromDomain(progress: PracticeProgress) = PracticeProgressEntity(
            patternId = progress.patternId,
            practiceCount = progress.practiceCount,
            lastPracticedTimestamp = progress.lastPracticedTimestamp,
            highestBpmAchieved = progress.highestBpmAchieved,
            lastUsedBpm = progress.lastUsedBpm,
            totalTimeSeconds = progress.totalTimeSeconds,
            completedRounds = progress.completedRounds
        )
    }
}
