package com.sharn.handpan.audio

import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent

data class PracticeTimelinePosition(
    val elapsedNanos: Long,
    val elapsedMs: Long,
    val currentBeat: Double,
    val currentBeatInBar: Double,
    val beatNumber: Int,
    val barNumber: Int,
    val currentNoteIndex: Int,
    val currentNote: NoteEvent?,
    val nextNote: NoteEvent?,
    val beatProgress: Float,
    val barProgress: Float,
    val patternProgress: Float,
    val bpm: Int,
    val isOnBeatWindow: Boolean,
    val isDownbeat: Boolean,
    val countdownRemaining: Int = 0
)

class PracticeTimeline(
    private val pattern: HandpanPattern,
    val bpm: Int = pattern.bpm
) {
    private val beatDurationNanos = MusicalTiming.signatureBeatDurationNanos(bpm, pattern.timeSignature)
    private val totalDurationNanos = MusicalTiming.beatToNanos(
        pattern.totalBeats,
        bpm,
        pattern.timeSignature
    )

    val totalBeats: Double
        get() = pattern.totalBeats

    val durationNanos: Long
        get() = totalDurationNanos

    fun beatAt(elapsedNanos: Long): Double {
        val elapsed = elapsedNanos.coerceIn(0L, totalDurationNanos)
        return elapsed.toDouble() / beatDurationNanos.toDouble()
    }

    fun positionAtLoopElapsed(
        elapsedNanos: Long,
        loopStartBeat: Double,
        loopEndBeat: Double
    ): PracticeTimelinePosition {
        val loopBeats = (loopEndBeat - loopStartBeat).coerceAtLeast(0.0)
        if (loopBeats == 0.0) return positionAtBeat(loopStartBeat)
        val loopDuration = MusicalTiming.beatToNanos(loopBeats, bpm, pattern.timeSignature)
        val loopElapsed = elapsedNanos.coerceAtLeast(0L) % loopDuration
        return positionAtBeat(loopStartBeat + beatAt(loopElapsed))
    }

    fun positionAt(elapsedNanos: Long, countdownBeats: Int = 0): PracticeTimelinePosition {
        val elapsed = elapsedNanos.coerceIn(0L, totalDurationNanos)
        val beat = beatAt(elapsed)
        val countdownRemaining = (countdownBeats - kotlin.math.floor(beat).toInt()).coerceAtLeast(0)
        return positionAtBeat(beat, elapsed, countdownRemaining)
    }

    fun positionAtBeat(beat: Double, countdownRemaining: Int = 0): PracticeTimelinePosition {
        val clampedBeat = beat.coerceIn(0.0, pattern.totalBeats)
        val elapsed = MusicalTiming.beatToNanos(clampedBeat, bpm, pattern.timeSignature)
        return positionAtBeat(clampedBeat, elapsed, countdownRemaining)
    }

    private fun positionAtBeat(
        beat: Double,
        elapsed: Long,
        countdownRemaining: Int
    ): PracticeTimelinePosition {
        val orderedEvents = pattern.orderedEvents
        val currentIndex = orderedEvents.indexOfLast { event ->
            !event.isRest && event.beatPosition <= beat + PatternScheduler.BEAT_EPSILON
        }
        val current = orderedEvents.getOrNull(currentIndex)
        val next = orderedEvents.drop(currentIndex + 1).firstOrNull { !it.isRest }
        val beatFraction = (beat - kotlin.math.floor(beat)).toFloat()
        val progress = if (totalDurationNanos == 0L) 1f
        else elapsed.toFloat() / totalDurationNanos.toFloat()
        val beatNumber = kotlin.math.floor(beat).toInt() + 1
        val barNumber = kotlin.math.floor(beat / pattern.timeSignature.beatsPerBar).toInt() + 1
        val beatProgress = beatFraction
        val currentBeatInBar = (beat % pattern.timeSignature.beatsPerBar) + 1.0
        val barProgress = ((beat % pattern.timeSignature.beatsPerBar) /
            pattern.timeSignature.beatsPerBar).toFloat()

        return PracticeTimelinePosition(
            elapsedNanos = elapsed,
            elapsedMs = elapsed / 1_000_000L,
            currentBeat = beat,
            currentBeatInBar = currentBeatInBar,
            beatNumber = beatNumber,
            barNumber = barNumber,
            currentNoteIndex = currentIndex,
            currentNote = current,
            nextNote = next,
            beatProgress = beatFraction,
            barProgress = barProgress,
            patternProgress = progress.coerceIn(0f, 1f),
            bpm = bpm,
            isOnBeatWindow = beatProgress <= PatternScheduler.BEAT_EPSILON ||
                beatProgress >= 1.0f - PatternScheduler.BEAT_EPSILON,
            isDownbeat = currentBeatInBar <= 1.0 + PatternScheduler.BEAT_EPSILON,
            countdownRemaining = countdownRemaining
        )
    }
}