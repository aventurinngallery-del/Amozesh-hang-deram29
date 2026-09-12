package com.sharn.handpan.audio

import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * High-accuracy YIN Pitch Detection Algorithm.
 * Implements the full YIN pipeline:
 * 1. Squared Difference Function: d(tau)
 * 2. Cumulative Mean Normalized Difference Function: d'(tau)
 * 3. Absolute Threshold with true local minimum search
 * 4. Parabolic Interpolation for sub-sample fundamental frequency estimation
 */
class YinPitchDetector(
    private val sampleRate: Int = 22050,
    private val threshold: Double = 0.15,
    private val minFrequency: Double = 80.0,
    private val maxFrequency: Double = 900.0
) {
    data class PitchResult(
        val frequencyHz: Float,
        val noteName: String,
        val centsOffset: Int,
        val confidence: Float,
        val isPitched: Boolean
    )

    private val minPeriod = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
    private val maxPeriod = (sampleRate / minFrequency).toInt()

    companion object {
        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        fun frequencyToNoteAndCents(freq: Float): Pair<String, Int> {
            if (freq <= 20f) return Pair("--", 0)
            val midi = 69.0 + 12.0 * log2(freq.toDouble() / 440.0)
            val roundedMidi = midi.roundToInt()
            val cents = ((midi - roundedMidi) * 100.0).roundToInt()
            val noteIndex = (roundedMidi % 12 + 12) % 12
            val octave = (roundedMidi / 12) - 1
            return Pair("${NOTE_NAMES[noteIndex]}$octave", cents)
        }

        fun calculateCentsDifference(actualFreq: Float, expectedFreq: Float): Float {
            if (actualFreq <= 0f || expectedFreq <= 0f) return 9999f
            return (1200.0 * log2((actualFreq / expectedFreq).toDouble())).toFloat()
        }
    }

    fun detectPitch(buffer: ShortArray, bufferLength: Int = buffer.size): PitchResult {
        val windowSize = bufferLength / 2
        if (windowSize <= maxPeriod) {
            return PitchResult(0f, "--", 0, 0f, false)
        }

        val floatBuffer = FloatArray(bufferLength)
        for (i in 0 until bufferLength) {
            floatBuffer[i] = buffer[i] / 32768.0f
        }

        // Step 1: Difference Function
        val difference = DoubleArray(maxPeriod + 1)
        for (tau in minPeriod..maxPeriod) {
            var sum = 0.0
            for (j in 0 until windowSize) {
                val diff = (floatBuffer[j] - floatBuffer[j + tau]).toDouble()
                sum += diff * diff
            }
            difference[tau] = sum
        }

        // Step 2: Cumulative Mean Normalized Difference Function (CMNDF)
        val cmndf = DoubleArray(maxPeriod + 1)
        cmndf[0] = 1.0
        var runningSum = 0.0
        for (tau in 1..maxPeriod) {
            runningSum += difference[tau]
            if (runningSum > 0.0) {
                cmndf[tau] = difference[tau] * tau / runningSum
            } else {
                cmndf[tau] = 1.0
            }
        }

        // Step 3: Absolute Threshold - search for first dip below threshold then local minimum
        var tauEstimate = -1
        var tau = minPeriod
        while (tau <= maxPeriod) {
            if (cmndf[tau] < threshold) {
                while (tau + 1 <= maxPeriod && cmndf[tau + 1] < cmndf[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }

        // Fallback to global minimum if no dip below threshold
        if (tauEstimate == -1) {
            var minVal = Double.MAX_VALUE
            for (t in minPeriod..maxPeriod) {
                if (cmndf[t] < minVal) {
                    minVal = cmndf[t]
                    tauEstimate = t
                }
            }
            if (minVal > 0.60) {
                return PitchResult(0f, "--", 0, 0f, false)
            }
        }

        // Step 4: Parabolic Interpolation
        val refinedTau: Double = if (tauEstimate in (minPeriod + 1) until maxPeriod) {
            val s0 = cmndf[tauEstimate - 1]
            val s1 = cmndf[tauEstimate]
            val s2 = cmndf[tauEstimate + 1]
            val bottom = 2.0 * (2.0 * s1 - s0 - s2)
            if (bottom != 0.0) {
                tauEstimate + (s2 - s0) / bottom
            } else {
                tauEstimate.toDouble()
            }
        } else {
            tauEstimate.toDouble()
        }

        if (refinedTau <= 0.0) return PitchResult(0f, "--", 0, 0f, false)

        val detectedFreq = (sampleRate / refinedTau).toFloat()
        if (detectedFreq !in minFrequency.toFloat()..maxFrequency.toFloat()) {
            return PitchResult(0f, "--", 0, 0f, false)
        }

        val confidence = (1.0 - cmndf[tauEstimate].coerceIn(0.0, 1.0)).toFloat()
        val (noteName, cents) = frequencyToNoteAndCents(detectedFreq)

        return PitchResult(
            frequencyHz = detectedFreq,
            noteName = noteName,
            centsOffset = cents,
            confidence = confidence,
            isPitched = true
        )
    }
}
