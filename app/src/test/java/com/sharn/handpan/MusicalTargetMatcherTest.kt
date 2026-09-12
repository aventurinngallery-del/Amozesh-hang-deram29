package com.sharn.handpan

import com.sharn.handpan.model.DetectedStrikeEvent
import com.sharn.handpan.model.MusicalTarget
import com.sharn.handpan.model.MusicalTargetIdentity
import com.sharn.handpan.model.MusicalTargetMatcher
import com.sharn.handpan.model.TargetMatchDecision
import com.sharn.handpan.model.TargetMatchType
import com.sharn.handpan.model.TargetRegistry
import com.sharn.handpan.model.TimingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicalTargetMatcherTest {
    @Test
    fun wrongThenCorrectKeepsTargetAvailable() {
        val fixture = fixture(target("target-1", setOf(0), 1_000_000_000L))
        val wrong = match(fixture, strike("wrong", 1, 1_000_000_000L))
        val correct = match(fixture, strike("correct", 0, 1_000_000_000L))
        assertEquals(TargetMatchType.WRONG, wrong.type)
        assertEquals(TargetMatchType.CORRECT, correct.type)
        assertTrue(fixture.registry.activeTargets().single().isConsumed)
    }

    @Test
    fun earlyStrikeUsesCandidateAndTiming() {
        val fixture = fixture(target("future", setOf(0), 1_000_000_000L))
        val result = match(fixture, strike("early", 0, 900_000_000L))
        assertEquals(TargetMatchType.CORRECT, result.type)
        assertEquals(TimingStatus.EARLY, result.timing?.status)
    }

    @Test
    fun outsideWindowIsExtraAndDoesNotConsumeTarget() {
        val fixture = fixture(target("target-1", setOf(0), 1_000_000_000L))
        val result = match(fixture, strike("extra", 0, 1_300_000_000L))
        assertEquals(TargetMatchType.EXTRA, result.type)
        assertFalse(fixture.registry.activeTargets().single().isConsumed)
    }

    @Test
    fun duplicateEventIdIsHandledByRegistry() {
        val fixture = fixture(target("target-1", setOf(0), 1_000_000_000L))
        val event = strike("same-event", 0, 1_000_000_000L)
        val first = match(fixture, event)
        val duplicate = if (fixture.registry.markProcessed(event.id)) {
            match(fixture, event)
        } else {
            TargetMatchDecision(TargetMatchType.EXTRA, null, null, null, duplicate = true)
        }
        assertEquals(TargetMatchType.CORRECT, first.type)
        assertTrue(duplicate.duplicate)
        assertEquals(1, fixture.registry.activeTargets().single().consumedNotes.size)
    }

    @Test
    fun chordWrongDoesNotConsumeAndFinalizeMissesRemainder() {
        val fixture = fixture(target("chord", setOf(0, 1), 1_000_000_000L))
        val first = match(fixture, strike("note-1", 1, 1_000_000_000L))
        val wrong = match(fixture, strike("note-extra", 2, 1_000_000_000L))
        val missed = finalize(fixture, 1_200_000_000L)
        assertEquals(TargetMatchType.CORRECT, first.type)
        assertEquals(TargetMatchType.WRONG, wrong.type)
        assertEquals(TargetMatchType.MISSED, missed.single().type)
        assertEquals(setOf(0), missed.single().target?.remainingNotes)
    }

    @Test
    fun equalDistanceUsesSequenceThenLoopThenTargetId() {
        val fixture = fixture(
            target("later", setOf(0), 1_100_000_000L, sequence = 2),
            target("earlier", setOf(0), 900_000_000L, sequence = 1)
        )
        val result = match(fixture, strike("tie", 0, 1_000_000_000L))
        assertEquals("earlier", result.target?.identity?.targetId)
    }

    @Test
    fun finalizedTargetCannotBeMatched() {
        val fixture = fixture(target("expired", setOf(0), 1_000_000_000L))
        val missed = finalize(fixture, 1_200_000_000L)
        val late = match(fixture, strike("late", 0, 1_200_000_000L))
        assertEquals(TargetMatchType.MISSED, missed.single().type)
        assertEquals(TargetMatchType.EXTRA, late.type)
    }

    private data class Fixture(val matcher: MusicalTargetMatcher, val registry: TargetRegistry)

    private fun fixture(vararg targets: MusicalTarget) = Fixture(
        MusicalTargetMatcher(),
        TargetRegistry().also { registry -> targets.forEach(registry::register) }
    )

    private fun match(fixture: Fixture, event: DetectedStrikeEvent): TargetMatchDecision {
        check(fixture.registry.markProcessed(event.id))
        val candidate = fixture.matcher.selectCandidate(fixture.registry.activeTargets(), event)
        return fixture.matcher.classify(candidate, event).also(fixture.registry::apply)
    }

    private fun finalize(fixture: Fixture, nowNanos: Long): List<TargetMatchDecision> =
        fixture.matcher.finalizeCandidates(fixture.registry.activeTargets(), nowNanos).mapNotNull { target ->
            fixture.registry.finalize(target.identity.targetId)?.let {
                TargetMatchDecision(TargetMatchType.MISSED, it, null, null)
            }
        }

    private fun target(
        id: String,
        notes: Set<Int>,
        timestamp: Long,
        loopId: String = "loop-1",
        sequence: Int = 0
    ) = MusicalTarget(
        MusicalTargetIdentity(
            sessionId = "session-1",
            patternId = "pattern-1",
            loopId = loopId,
            sequenceIndex = sequence,
            targetId = id,
            beatIndex = sequence,
            subdivisionIndex = 0,
            expectedTimestampNanos = timestamp,
            expectedNotes = notes,
            chordId = if (notes.size > 1) id else ""
        )
    )

    private fun strike(id: String, note: Int?, timestamp: Long) = DetectedStrikeEvent(
        id = id,
        sessionId = "session-1",
        monotonicTimestampNanos = timestamp,
        detectedFrequencyHz = 220f,
        detectedNoteName = "A3",
        detectedCentsOffset = 0,
        detectedNote = note,
        matchedPitchDiffHz = 0f,
        pitchConfidence = if (note == null) 0.2f else 0.9f,
        onsetStrength = 0.8f,
        energy = 0.8f,
        pitchValid = note != null,
        source = "test"
    )
}
