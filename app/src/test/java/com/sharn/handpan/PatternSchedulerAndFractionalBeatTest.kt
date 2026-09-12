package com.sharn.handpan

import com.sharn.handpan.audio.PatternScheduler
import com.sharn.handpan.audio.PracticeClock
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.TimeSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternSchedulerAndFractionalBeatTest {

    @Test
    fun testFractionalBeatSubdivisions_16thNotes() {
        val events = listOf(
            NoteEvent(noteNumber = 1, beatPosition = 0.0),
            NoteEvent(noteNumber = 2, beatPosition = 0.25),
            NoteEvent(noteNumber = 3, beatPosition = 0.5),
            NoteEvent(noteNumber = 4, beatPosition = 0.75),
            NoteEvent(noteNumber = 5, beatPosition = 1.0)
        )

        val schedule = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1,
            startBar = 1,
            endBar = 1
        )

        // Slices created for 0.0, 0.25, 0.5, 0.75, 1.0, 2.0, 3.0
        assertTrue("Schedule must have at least 5 slices", schedule.size >= 5)

        val slice0 = schedule.find { Math.abs(it.beatPosition - 0.0) < 0.01 }
        assertNotNull(slice0)
        assertEquals(1, slice0!!.events.size)
        assertEquals(1, slice0.events[0].noteNumber)
        assertTrue(slice0.isDownbeat)

        val sliceQuarter = schedule.find { Math.abs(it.beatPosition - 0.25) < 0.01 }
        assertNotNull(sliceQuarter)
        assertEquals(1, sliceQuarter!!.events.size)
        assertEquals(2, sliceQuarter.events[0].noteNumber)
        assertFalse(sliceQuarter.isDownbeat)
    }

    @Test
    fun testMultipleSimultaneousNotesOnSameBeat() {
        // Chord on beat 0.0 (Ding + Note 1)
        val events = listOf(
            NoteEvent(noteNumber = 0, beatPosition = 0.0),
            NoteEvent(noteNumber = 1, beatPosition = 0.0),
            NoteEvent(noteNumber = 3, beatPosition = 1.0)
        )

        val schedule = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 1
        )

        val slice0 = schedule.find { Math.abs(it.beatPosition - 0.0) < 0.01 }
        assertNotNull(slice0)
        assertEquals(2, slice0!!.events.size)
        assertEquals(0, slice0.events[0].noteNumber)
        assertEquals(1, slice0.events[1].noteNumber)
    }

    @Test
    fun testPracticeClockMonotonicity() {
        val clock = PracticeClock.Default
        val t1 = clock.nowNanos()
        val t2 = clock.nowNanos()
        assertTrue("Monotonic clock must never move backward", t2 >= t1)
    }

    @Test
    fun testBarLoopRangeExtraction() {
        val events = (0..15).map { beat ->
            NoteEvent(noteNumber = (beat % 8) + 1, beatPosition = beat.toDouble())
        }

        // Loop Bar 2 to Bar 3 in 4/4 (Beats 4.0 to 8.0 inclusive)
        val schedule = PatternScheduler.buildSchedule(
            events = events,
            beatsPerBar = 4,
            totalBars = 4,
            startBar = 2,
            endBar = 3
        )

        assertEquals(8, schedule.size)
        assertEquals(4.0, schedule.first().beatPosition, 0.01)
        assertEquals(11.0, schedule.last().beatPosition, 0.01)
    }
}
