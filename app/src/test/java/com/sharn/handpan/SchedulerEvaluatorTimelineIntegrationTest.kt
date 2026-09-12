package com.sharn.handpan

import com.sharn.handpan.audio.AcousticPracticeEvaluator
import com.sharn.handpan.audio.PatternScheduler
import com.sharn.handpan.model.AssessmentEventType
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.model.PracticeSessionContext
import com.sharn.handpan.model.TimeSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerEvaluatorTimelineIntegrationTest {
    @Test
    fun schedulerIdentitySurvivesEvaluatorMatcherAndTimeline() {
        val clock = FakePracticeClock()
        val evaluator = AcousticPracticeEvaluator(clock = clock)
        val session = PracticeSessionContext.start("pattern-1", clock.currentNanos)
        val events = listOf(NoteEvent(noteNumber = 0, beatPosition = 0.0))
        val target = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1,
            timeSignature = TimeSignature.Common44,
            assessmentSessionId = session.sessionId,
            patternId = "pattern-1",
            loopIndex = 1,
            scheduleStartTimestampNanos = clock.currentNanos,
            bpm = 60
        ).single { it.target != null }.target!!

        evaluator.startAssessment(
            context = session,
            pattern = com.sharn.handpan.model.HandpanPattern(
                id = "pattern-1",
                title = "identity",
                description = "identity",
                bpm = 60,
                events = events
            ),
            scaleConfig = NotePitchConfig.D_KURD_9
        )
        evaluator.notifyExpectedTarget(target)
        evaluator.evaluateDetectedPitch(146.83f, 0.95f, clock.currentNanos)

        val result = evaluator.timeline.snapshot().last()
        assertEquals(AssessmentEventType.CORRECT, result.eventType)
        assertEquals(target.identity.targetId, result.targetId)
        assertEquals(target.identity.loopId, result.loopId)
        assertEquals(target.identity.sequenceIndex, result.sequenceIndex)
        assertTrue(result.isConsumed)
    }

    @Test
    fun completeChordConsumesEachObligationAndPartialChordLeavesMissedTimeline() {
        val clock = FakePracticeClock()
        val evaluator = AcousticPracticeEvaluator(clock = clock)
        val session = PracticeSessionContext.start("pattern-2", clock.currentNanos)
        val events = listOf(
            NoteEvent(noteNumber = 0, beatPosition = 0.0),
            NoteEvent(noteNumber = 1, beatPosition = 0.0)
        )
        val target = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1,
            timeSignature = TimeSignature.Common44,
            assessmentSessionId = session.sessionId,
            patternId = "pattern-2",
            loopIndex = 0,
            scheduleStartTimestampNanos = clock.currentNanos,
            bpm = 60
        ).single { it.target != null }.target!!
        evaluator.startAssessment(
            context = session,
            com.sharn.handpan.model.HandpanPattern("pattern-2", "chord", "chord", 60, events = events),
            NotePitchConfig.D_KURD_9
        )
        evaluator.notifyExpectedTarget(target)
        evaluator.evaluateDetectedPitch(146.83f, 0.95f, clock.currentNanos)
        evaluator.evaluateDetectedPitch(220.0f, 0.95f, clock.currentNanos)
        assertEquals(2, target.identity.expectedNotes.size)
        assertEquals(2, evaluator.timeline.snapshot().count { it.eventType == AssessmentEventType.CORRECT })
    }

    @Test
    fun simultaneousRepeatedPitchObligationsRequireTwoHits() {
        val clock = FakePracticeClock()
        val evaluator = AcousticPracticeEvaluator(clock = clock)
        val session = PracticeSessionContext.start("same-pitch", clock.currentNanos)
        val events = listOf(
            NoteEvent(noteNumber = 1, beatPosition = 0.0),
            NoteEvent(noteNumber = 1, beatPosition = 0.0)
        )
        val target = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1,
            assessmentSessionId = session.sessionId,
            patternId = session.patternId,
            scheduleStartTimestampNanos = session.startTimestampNanos,
            bpm = 60
        ).single { it.target != null }.target!!

        evaluator.startAssessment(session, HandpanPattern("same-pitch", "same", "same", 60, events = events), NotePitchConfig.D_KURD_9)
        evaluator.notifyExpectedTarget(target)
        evaluator.evaluateDetectedPitch(220.0f, 0.95f, clock.currentNanos)
        evaluator.evaluateDetectedPitch(220.0f, 0.95f, clock.currentNanos + 1L)

        assertEquals(2, target.effectiveObligations.size)
        assertEquals(2, evaluator.timeline.snapshot().count { it.eventType == AssessmentEventType.CORRECT })
        assertEquals(2, evaluator.timeline.snapshot().count { it.eventType == AssessmentEventType.EXPECTED })
        assertEquals(2, evaluator.timeline.snapshot().map { it.obligationId }.distinct().size)
    }
}