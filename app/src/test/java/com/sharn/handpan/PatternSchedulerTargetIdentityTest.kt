package com.sharn.handpan

import com.sharn.handpan.audio.PatternScheduler
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.TimeSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PatternSchedulerTargetIdentityTest {
    @Test
    fun schedulerCreatesStableDistinctTargetIdentityForEachLoop() {
        val events = listOf(NoteEvent(noteNumber = 0, beatPosition = 0.0))
        val loopZero = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1,
            timeSignature = TimeSignature.Common44,
            assessmentSessionId = "session-1",
            patternId = "pattern-1",
            loopIndex = 0,
            bpm = 120
        ).single { it.target != null }.target!!
        val loopOne = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1,
            timeSignature = TimeSignature.Common44,
            assessmentSessionId = "session-1",
            patternId = "pattern-1",
            loopIndex = 1,
            bpm = 120
        ).single { it.target != null }.target!!

        assertEquals("loop-0", loopZero.identity.loopId)
        assertEquals("loop-1", loopOne.identity.loopId)
        assertNotEquals(loopZero.identity.targetId, loopOne.identity.targetId)
        assertNotEquals(loopZero.identity, loopOne.identity)
    }

    @Test
    fun schedulerPreservesFractionalBeatInTargetIdentity() {
        val schedule = PatternScheduler.buildSchedule(
            events = listOf(NoteEvent(noteNumber = 1, beatPosition = 1.25)),
            beatsPerBar = 4,
            totalBars = 1,
            timeSignature = TimeSignature.Common44,
            assessmentSessionId = "session-1",
            patternId = "pattern-1",
            loopIndex = 0,
            scheduleStartTimestampNanos = 10_000_000_000L,
            bpm = 60
        )
        val target = schedule.firstOrNull { it.target != null }?.target

        assertNotNull(target)
        assertEquals(1, target?.identity?.beatIndex)
        assertEquals(4, target?.identity?.subdivisionIndex)
        assertEquals(11_250_000_000L, target?.identity?.expectedTimestampNanos)
    }
}
