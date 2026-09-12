package com.sharn.handpan.audio

import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimeSignature

/** Shared musical-time conversions used by playback, metronome, and evaluation. */
object MusicalTiming {
    fun beatDurationNanos(bpm: Int): Long {
        return (60_000_000_000L / bpm.coerceIn(40, 300).toLong())
    }

    /** Duration of one position in a pattern, where the position is the signature denominator. */
    fun signatureBeatDurationNanos(bpm: Int, timeSignature: TimeSignature): Long {
        return (beatDurationNanos(bpm) * 4L) / timeSignature.denominator
    }

    fun subdivisionDurationNanos(bpm: Int, subdivision: Subdivision): Long {
        return beatDurationNanos(bpm) / subdivision.divisionsPerBeat
    }

    fun beatToNanos(beatPosition: Double, bpm: Int): Long {
        return (beatPosition * beatDurationNanos(bpm)).toLong()
    }

    fun beatToNanos(beatPosition: Double, bpm: Int, timeSignature: TimeSignature): Long {
        return (beatPosition * signatureBeatDurationNanos(bpm, timeSignature)).toLong()
    }

    fun tickIndexToNanos(tickIndex: Long, bpm: Int, subdivision: Subdivision): Long {
        return tickIndex * subdivisionDurationNanos(bpm, subdivision)
    }
}
