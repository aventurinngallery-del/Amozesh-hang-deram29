package com.sharn.handpan.audio

import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.PracticeScore

enum class PracticeTargetPresentation {
    IDLE,
    TARGET,
    HIT_PERFECT,
    HIT_GOOD,
    HIT_EARLY,
    HIT_LATE,
    MISS
}

data class PracticeTargetState(
    val currentNote: NoteEvent? = null,
    val nextNote: NoteEvent? = null,
    val currentNoteIndex: Int = -1,
    val beatNumber: Int = 1,
    val currentBeatInBar: Double = 1.0,
    val barNumber: Int = 1,
    val beatProgress: Float = 0f,
    val barProgress: Float = 0f,
    val patternProgress: Float = 0f,
    val countdown: Int = 0,
    val phase: PracticePhase = PracticePhase.IDLE,
    val bpm: Int = 70,
    val timingWindowMs: Long = 90L,
    val score: PracticeScore? = null,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val presentation: PracticeTargetPresentation = PracticeTargetPresentation.IDLE
) {
    companion object {
        fun from(
            position: PracticeTimelinePosition,
            phase: PracticePhase,
            score: PracticeScore? = null,
            combo: Int = 0,
            maxCombo: Int = score?.maxCombo ?: 0,
            presentation: PracticeTargetPresentation = PracticeTargetPresentation.TARGET
        ) = PracticeTargetState(
            currentNote = position.currentNote,
            nextNote = position.nextNote,
            currentNoteIndex = position.currentNoteIndex,
            beatNumber = position.beatNumber,
            currentBeatInBar = position.currentBeatInBar,
            barNumber = position.barNumber,
            beatProgress = position.beatProgress,
            barProgress = position.barProgress,
            patternProgress = position.patternProgress,
            countdown = position.countdownRemaining,
            phase = phase,
            bpm = position.bpm,
            score = score,
            combo = combo,
            maxCombo = maxCombo,
            presentation = presentation
        )
    }
}