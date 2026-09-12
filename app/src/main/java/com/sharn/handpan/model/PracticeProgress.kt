package com.sharn.handpan.model

/**
 * Stores local user practice statistics for an exercise or pattern.
 */
data class PracticeProgress(
    val patternId: String,
    val practiceCount: Int = 0,
    val lastPracticedTimestamp: Long = System.currentTimeMillis(),
    val highestBpmAchieved: Int = 70,
    val lastUsedBpm: Int = 70,
    val totalTimeSeconds: Int = 0,
    val completedRounds: Int = 0
)
