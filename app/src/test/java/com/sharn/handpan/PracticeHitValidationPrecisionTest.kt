package com.sharn.handpan

import com.sharn.handpan.audio.HitCandidate
import com.sharn.handpan.audio.HitQuality
import com.sharn.handpan.audio.PitchClass
import com.sharn.handpan.audio.PracticeHitValidator
import com.sharn.handpan.audio.TimingClass
import com.sharn.handpan.audio.TimingWindows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeHitValidationPrecisionTest {
    private val expectedTime = 1_000_000_000L
    private val windows = TimingWindows()

    private fun hit(offsetMs: Long, note: Int = 5, confidence: Float = 0.95f) =
        PracticeHitValidator.validate(
            candidate = HitCandidate(expectedTime + offsetMs * 1_000_000L, note, confidence, "test"),
            expectedTimestampNanos = expectedTime,
            expectedNote = 5,
            windows = windows
        )

    @Test
    fun classifiesExactAndTimingBoundariesDeterministically() {
        assertEquals(TimingClass.PERFECT, hit(0).timingClass)
        assertEquals(TimingClass.PERFECT, hit(45).timingClass)
        assertEquals(TimingClass.GREAT, hit(46).timingClass)
        assertEquals(TimingClass.GREAT, hit(70).timingClass)
        assertEquals(TimingClass.GOOD, hit(71).timingClass)
        assertEquals(TimingClass.GOOD, hit(90).timingClass)
        assertEquals(TimingClass.LATE, hit(91).timingClass)
        assertEquals(TimingClass.EARLY, hit(-91).timingClass)
    }

    @Test
    fun classifiesMissOutsideConfiguredWindow() {
        val result = hit(161)
        assertEquals(TimingClass.MISS, result.timingClass)
        assertFalse(result.timingCorrect)
        assertEquals(0, result.scoreContribution)
    }

    @Test
    fun separatesWrongNoteAndLowConfidence() {
        assertEquals(PitchClass.WRONG_NOTE, hit(0, note = 6).pitchClass)
        assertEquals(PitchClass.LOW_CONFIDENCE, hit(0, confidence = 0.49f).pitchClass)
        assertFalse(hit(0, note = 6).pitchCorrect)
    }

    @Test
    fun duplicateOnsetCannotContributeScore() {
        val result = PracticeHitValidator.validate(
            HitCandidate(
                timestampNanos = expectedTime + 10_000_000L,
                detectedNote = 5,
                confidence = 0.95f,
                source = "microphone",
                duplicateOfTargetId = "target-1",
                isRetrigger = true
            ),
            expectedTimestampNanos = expectedTime,
            expectedNote = 5,
            windows = windows
        )

        assertTrue(result.isDuplicate)
        assertEquals(HitQuality.DUPLICATE, result.hitQuality)
        assertEquals(0, result.scoreContribution)
    }

    @Test
    fun nullCandidateIsTimelineMiss() {
        val result = PracticeHitValidator.validate(null, expectedTime, 5, windows = windows)
        assertTrue(result.isMiss)
        assertEquals(HitQuality.MISS, result.hitQuality)
        assertEquals(TimingClass.MISS, result.timingClass)
    }
}
