package com.sharn.handpan.audio

import com.sharn.handpan.model.HandpanPattern
import kotlin.math.ceil

object PracticePreparation {
    fun previewBeats(pattern: HandpanPattern): Double {
        val lastActiveBeat = pattern.activeNotes.maxOfOrNull { it.beatPosition + it.duration }
        val exerciseBeats = (lastActiveBeat ?: 0.0).coerceAtLeast(1.0)
        return exerciseBeats.coerceAtMost(pattern.totalBeats)
    }

    fun previewBeatCount(pattern: HandpanPattern): Int =
        ceil(previewBeats(pattern)).toInt().coerceAtLeast(1)
}