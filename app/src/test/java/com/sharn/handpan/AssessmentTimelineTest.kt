package com.sharn.handpan

import com.sharn.handpan.model.AssessmentEventType
import com.sharn.handpan.model.AssessmentTimeline
import com.sharn.handpan.model.AssessmentTimelineEvent
import com.sharn.handpan.model.TimingResult
import com.sharn.handpan.model.TimingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AssessmentTimelineTest {
    @Test
    fun keepsIndependentExpectedAndResultEvents() {
        val timeline = AssessmentTimeline()

        timeline.append(event("target-1", AssessmentEventType.EXPECTED, targetId = "target-1"))
        timeline.append(
            event(
                eventId = "strike-1",
                eventType = AssessmentEventType.WRONG,
                expectedNote = 0,
                detectedNote = 1,
                targetId = "target-1",
                timingResult = TimingResult(TimingStatus.PERFECT, 0L),
                isConsumed = false
            )
        )

        assertEquals(2, timeline.snapshot().size)
        assertEquals(AssessmentEventType.EXPECTED, timeline.snapshot()[0].eventType)
        assertEquals(AssessmentEventType.WRONG, timeline.snapshot()[1].eventType)
        assertEquals(false, timeline.snapshot()[1].isConsumed)
    }

    @Test
    fun rejectsDuplicateEventIds() {
        val timeline = AssessmentTimeline()
        timeline.append(event("same", AssessmentEventType.EXTRA))

        assertThrows(IllegalArgumentException::class.java) {
            timeline.append(event("same", AssessmentEventType.EXTRA))
        }
    }

    @Test
    fun notifiesSubscribersExactlyOncePerAppend() {
        val timeline = AssessmentTimeline()
        val received = mutableListOf<String>()
        val subscription = timeline.subscribe { received += it.eventId }

        timeline.append(event("one", AssessmentEventType.CORRECT))
        subscription.close()
        timeline.append(event("two", AssessmentEventType.MISSED))

        assertEquals(listOf("one"), received)
    }

    @Test
    fun preservesAllIndependentAssessmentEventTypes() {
        val timeline = AssessmentTimeline()
        val types = listOf(
            AssessmentEventType.EXPECTED,
            AssessmentEventType.CORRECT,
            AssessmentEventType.WRONG,
            AssessmentEventType.UNKNOWN,
            AssessmentEventType.MISSED,
            AssessmentEventType.EXTRA
        )

        types.forEachIndexed { index, type ->
            timeline.append(event("event-$index", type))
        }

        assertEquals(types, timeline.snapshot().map { it.eventType })
    }

    private fun event(
        eventId: String,
        eventType: AssessmentEventType,
        expectedNote: Int? = null,
        detectedNote: Int? = null,
        targetId: String? = null,
        timingResult: TimingResult? = null,
        isConsumed: Boolean = false
    ) = AssessmentTimelineEvent(
        eventId = eventId,
        sessionId = "session-1",
        loopId = "loop-1",
        sequenceIndex = 0,
        expectedNote = expectedNote,
        detectedNote = detectedNote,
        eventType = eventType,
        expectedTimestampNanos = 1_000_000_000L,
        detectedTimestampNanos = if (detectedNote != null) 1_000_000_000L else null,
        deviationNanos = timingResult?.deviationNanos,
        timingResult = timingResult,
        confidence = if (detectedNote == null) 0f else 0.9f,
        targetId = targetId,
        source = "test",
        durationNanos = null,
        isConsumed = isConsumed
    )
}