package com.sharn.handpan

import com.sharn.handpan.audio.AudioAnalysisSession
import com.sharn.handpan.audio.AudioCaptureError
import com.sharn.handpan.audio.AcousticPracticeEvaluator
import com.sharn.handpan.audio.AudioCaptureErrorKind
import com.sharn.handpan.audio.MicrophoneState
import com.sharn.handpan.audio.PitchDetector
import com.sharn.handpan.audio.PracticeClock
import com.sharn.handpan.model.DetectedStrikeEvent
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotePitchConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioCaptureFailureTest {
    @Test
    fun detectorStartupFailureIsVisibleAndLeavesAssessmentInactive() {
        val evaluator = AcousticPracticeEvaluator(
            clock = PracticeClock.Default,
            analysisSession = AudioAnalysisSession(FailingPitchDetector())
        )
        val pattern = HandpanPattern(
            id = "capture-failure",
            title = "Capture failure",
            description = "Capture failure",
            events = listOf(NoteEvent(0, 0.0))
        )

        evaluator.startAssessment(
            pattern = pattern,
            scaleConfig = NotePitchConfig.D_KURD_9
        )

        assertEquals(MicrophoneState.MIC_ERROR, evaluator.state.value.microphoneState)
        assertFalse(evaluator.state.value.isListening)
        assertFalse(evaluator.state.value.assessmentActive)
        evaluator.release()
    }

    @Test
    fun unavailableMicrophoneIsTerminalAndCannotProduceFinalizedEvidence() {
        val analysisSession = UnavailableAnalysisSession()
        val evaluator = AcousticPracticeEvaluator(analysisSession = analysisSession)
        val pattern = HandpanPattern(
            id = "unavailable-microphone",
            title = "Unavailable microphone",
            description = "Unavailable microphone",
            events = listOf(NoteEvent(0, 0.0))
        )

        evaluator.startAssessment(
            context = com.sharn.handpan.model.PracticeSessionContext.start("unavailable-microphone", 1_000L),
            pattern = pattern,
            scaleConfig = NotePitchConfig.D_KURD_9,
            bpm = pattern.bpm
        )
        evaluator.stopAssessment(showSummary = false)

        assertEquals(MicrophoneState.MIC_UNAVAILABLE, evaluator.state.value.microphoneState)
        assertFalse(evaluator.state.value.assessmentActive)
        assertNull(evaluator.finalizedAssessment(2_000L))
        assertEquals(0, evaluator.timeline.snapshot().size)
        evaluator.release()
    }

    private class FailingPitchDetector : PitchDetector() {
        override fun startListening(
            scaleConfig: NotePitchConfig,
            onStrikeDetected: (com.sharn.handpan.audio.DetectedPitchResult, Long) -> Unit,
            onContinuousPitch: (com.sharn.handpan.audio.DetectedPitchResult) -> Unit,
            onCaptureError: (AudioCaptureError) -> Unit
        ): Boolean {
            onCaptureError(AudioCaptureError(AudioCaptureErrorKind.STARTUP, IllegalStateException("test startup failure")))
            return false
        }
    }

    private class UnavailableAnalysisSession : AudioAnalysisSession() {
        override fun acquire(
            scaleConfig: NotePitchConfig,
            onStrike: (DetectedStrikeEvent) -> Unit,
            onPitch: (com.sharn.handpan.audio.DetectedPitchResult) -> Unit,
            sessionId: String
        ): Subscription {
            onCaptureError?.invoke(
                AudioCaptureError(
                    AudioCaptureErrorKind.UNAVAILABLE,
                    IllegalStateException("test microphone unavailable")
                )
            )
            return Subscription({}, isActive = false)
        }
    }
}