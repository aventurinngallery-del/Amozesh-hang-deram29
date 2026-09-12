package com.sharn.handpan

import com.sharn.handpan.audio.AcousticPracticeEvaluator
import com.sharn.handpan.audio.PatternScheduler
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.TimeSignature
import java.util.concurrent.atomic.AtomicInteger

private val testLoopIndex = AtomicInteger()

fun AcousticPracticeEvaluator.notifyExpectedTestTarget(
    events: List<NoteEvent>,
    targetTimestampNanos: Long
) {
    val activeEvents = events.filterNot(NoteEvent::isRest).map { it.copy(beatPosition = 0.0) }
    if (activeEvents.isEmpty()) {
        expireTargetsAt(targetTimestampNanos)
        return
    }
    val target = PatternScheduler.buildSchedule(
        events = activeEvents,
        beatsPerBar = 4,
        totalBars = 1,
        timeSignature = TimeSignature.Common44,
        assessmentSessionId = assessmentSessionIdForTesting,
        patternId = "test-pattern",
        loopIndex = testLoopIndex.getAndIncrement(),
        scheduleStartTimestampNanos = targetTimestampNanos,
        bpm = 60
    ).firstNotNullOfOrNull { it.target } ?: return
    notifyExpectedTarget(target)
}
