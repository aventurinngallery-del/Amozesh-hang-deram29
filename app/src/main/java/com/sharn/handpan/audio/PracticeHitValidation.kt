package com.sharn.handpan.audio

import com.sharn.handpan.model.TimingPolicy
import com.sharn.handpan.model.TimingStatus
import kotlin.math.abs

data class TimingWindows(
    val perfectMs: Long = 45L,
    val greatMs: Long = 70L,
    val goodMs: Long = 90L,
    val missMs: Long = 160L,
    val duplicateMs: Long = 130L
) {
    init {
        require(perfectMs >= 0L)
        require(greatMs >= perfectMs)
        require(goodMs >= greatMs)
        require(missMs >= goodMs)
        require(duplicateMs >= 0L)
    }

    fun toTimingPolicy(): TimingPolicy = TimingPolicy(
        earlyWindowNanos = missMs * 1_000_000L,
        lateWindowNanos = missMs * 1_000_000L,
        perfectWindowNanos = perfectMs * 1_000_000L,
        goodWindowNanos = goodMs * 1_000_000L
    )
}

enum class TimingClass { PERFECT, GREAT, GOOD, EARLY, LATE, MISS }
enum class PitchClass { CORRECT, WRONG_NOTE, LOW_CONFIDENCE, NO_TARGET }
enum class HitQuality { VALID, MISS, DUPLICATE }

data class HitCandidate(
    val timestampNanos: Long,
    val detectedNote: Int?,
    val confidence: Float,
    val source: String,
    val pitchErrorCents: Float? = null,
    val duplicateOfTargetId: String? = null,
    val isRetrigger: Boolean = false
)

data class ValidatedHit(
    val timestampNanos: Long,
    val expectedNote: Int?,
    val detectedNote: Int?,
    val pitchErrorCents: Float?,
    val confidence: Float,
    val timingErrorMs: Long,
    val pitchCorrect: Boolean,
    val timingCorrect: Boolean,
    val scoreContribution: Int,
    val timingStatus: TimingStatus,
    val isMiss: Boolean,
    val timingClass: TimingClass,
    val pitchClass: PitchClass,
    val hitQuality: HitQuality,
    val isDuplicate: Boolean,
    val duplicateOfTargetId: String? = null
)

object PracticeHitValidator {
    fun validate(
        candidate: HitCandidate?,
        expectedTimestampNanos: Long,
        expectedNote: Int?,
        timingPolicy: TimingPolicy = TimingPolicy(),
        minimumConfidence: Float = 0.5f,
        windows: TimingWindows = TimingWindows(
            perfectMs = timingPolicy.perfectWindowNanos / 1_000_000L,
            greatMs = timingPolicy.perfectWindowNanos / 1_000_000L,
            goodMs = timingPolicy.goodWindowNanos / 1_000_000L,
            missMs = maxOf(timingPolicy.earlyWindowNanos, timingPolicy.lateWindowNanos) / 1_000_000L
        )
    ): ValidatedHit {
        if (candidate == null) {
            return ValidatedHit(
                timestampNanos = expectedTimestampNanos,
                expectedNote = expectedNote,
                detectedNote = null,
                pitchErrorCents = null,
                confidence = 0f,
                timingErrorMs = 0L,
                pitchCorrect = false,
                timingCorrect = false,
                scoreContribution = 0,
                timingStatus = TimingStatus.OUTSIDE_WINDOW,
                isMiss = true,
                timingClass = TimingClass.MISS,
                pitchClass = PitchClass.NO_TARGET,
                hitQuality = HitQuality.MISS,
                isDuplicate = false
            )
        }

        val errorNanos = candidate.timestampNanos - expectedTimestampNanos
        val errorMs = errorNanos / 1_000_000L
        val isDuplicate = candidate.isRetrigger || candidate.duplicateOfTargetId != null
        val timingStatus = when {
            abs(errorNanos) <= timingPolicy.perfectWindowNanos -> TimingStatus.PERFECT
            abs(errorNanos) <= timingPolicy.goodWindowNanos -> TimingStatus.GOOD
            errorNanos < 0 && abs(errorNanos) <= timingPolicy.earlyWindowNanos -> TimingStatus.EARLY
            errorNanos > 0 && errorNanos <= timingPolicy.lateWindowNanos -> TimingStatus.LATE
            else -> TimingStatus.OUTSIDE_WINDOW
        }
        val pitchClass = when {
            candidate.confidence < minimumConfidence -> PitchClass.LOW_CONFIDENCE
            expectedNote == null -> PitchClass.NO_TARGET
            candidate.detectedNote == expectedNote -> PitchClass.CORRECT
            else -> PitchClass.WRONG_NOTE
        }
        val pitchCorrect = pitchClass == PitchClass.CORRECT
        val timingCorrect = timingStatus != TimingStatus.OUTSIDE_WINDOW
        val timingClass = when {
            !timingCorrect -> TimingClass.MISS
            abs(errorMs) <= windows.perfectMs -> TimingClass.PERFECT
            abs(errorMs) <= windows.greatMs -> TimingClass.GREAT
            abs(errorMs) <= windows.goodMs -> TimingClass.GOOD
            errorMs < 0 -> TimingClass.EARLY
            else -> TimingClass.LATE
        }
        val quality = if (isDuplicate) HitQuality.DUPLICATE else HitQuality.VALID
        val score = when {
            quality == HitQuality.DUPLICATE || !pitchCorrect || !timingCorrect -> 0
            timingClass == TimingClass.PERFECT -> 100
            timingClass == TimingClass.GREAT -> 90
            timingClass == TimingClass.GOOD -> 80
            else -> 50
        }

        return ValidatedHit(
            timestampNanos = candidate.timestampNanos,
            expectedNote = expectedNote,
            detectedNote = candidate.detectedNote,
            pitchErrorCents = candidate.pitchErrorCents,
            confidence = candidate.confidence,
            timingErrorMs = errorMs,
            pitchCorrect = pitchCorrect,
            timingCorrect = timingCorrect,
            scoreContribution = score,
            timingStatus = timingStatus,
            isMiss = false,
            timingClass = timingClass,
            pitchClass = pitchClass,
            hitQuality = quality,
            isDuplicate = isDuplicate,
            duplicateOfTargetId = candidate.duplicateOfTargetId
        )
    }
}