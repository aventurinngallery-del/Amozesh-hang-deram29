package com.sharn.handpan

import com.sharn.handpan.model.AudioFrameQualityAnalyzer
import com.sharn.handpan.model.AudioFrameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFrameQualityTest {
    @Test
    fun silenceIsRejectedAsSilent() {
        val quality = analyze(ShortArray(128), noiseFloor = 0.001f)

        assertEquals(AudioFrameStatus.SILENT, quality.status)
        assertEquals(0f, quality.rms, 0.0001f)
        assertEquals(0f, quality.signalConfidence, 0.0001f)
    }

    @Test
    fun lowSignalIsDistinctFromSilence() {
        val quality = analyze(ShortArray(128) { 300 }, noiseFloor = 0.001f)

        assertEquals(AudioFrameStatus.LOW_SIGNAL, quality.status)
        assertTrue(quality.peak > 0f)
        assertTrue(quality.rms > AudioFrameQualityAnalyzer.SILENCE_RMS)
    }

    @Test
    fun clippingIsClassifiedAsOverload() {
        val quality = analyze(ShortArray(128) { Short.MAX_VALUE }, noiseFloor = 0.001f)

        assertEquals(AudioFrameStatus.OVERLOADED, quality.status)
        assertEquals(1f, quality.clippingRatio, 0.0001f)
    }

    @Test
    fun cleanSignalProducesDeterministicMetrics() {
        val quality = analyze(ShortArray(128) { 16_000 }, noiseFloor = 0.001f)

        assertEquals(AudioFrameStatus.VALID, quality.status)
        assertEquals(16_000f / Short.MAX_VALUE, quality.peak, 0.0001f)
        assertTrue(quality.signalToNoiseRatioDb > 50f)
        assertTrue(quality.signalConfidence > 0.9f)
    }

    @Test
    fun noisySignalIsRejectedWhenNoiseFloorIsCloseToSignal() {
        val quality = analyze(ShortArray(128) { 500 }, noiseFloor = 0.014f)

        assertEquals(AudioFrameStatus.NOISY, quality.status)
        assertTrue(quality.signalConfidence < 1f)
    }

    @Test
    fun latencyIsDerivedFromMonotonicTimestampOrder() {
        val quality = analyze(
            samples = ShortArray(128) { 16_000 },
            noiseFloor = 0.001f,
            captureTimestampNanos = 1_000L,
            analysisStartTimestampNanos = 2_000L,
            analysisEndTimestampNanos = 7_000L
        )

        assertEquals(6_000L, quality.captureToAnalysisLatencyNanos)
        assertEquals(5_000L, quality.analysisDurationNanos)
    }

    @Test
    fun invalidInputCannotProduceUsableQuality() {
        val quality = analyze(
            samples = ShortArray(8),
            sampleCount = 9,
            noiseFloor = 0.001f,
            captureTimestampNanos = 5_000L,
            analysisStartTimestampNanos = 4_000L,
            analysisEndTimestampNanos = 3_000L
        )

        assertEquals(AudioFrameStatus.INVALID, quality.status)
        assertEquals(0f, quality.signalConfidence, 0.0001f)
        assertTrue(quality.captureToAnalysisLatencyNanos >= 0L)
        assertTrue(quality.analysisDurationNanos >= 0L)
    }

    private fun analyze(
        samples: ShortArray,
        sampleCount: Int = samples.size,
        noiseFloor: Float,
        captureTimestampNanos: Long = 1_000L,
        analysisStartTimestampNanos: Long = 2_000L,
        analysisEndTimestampNanos: Long = 3_000L
    ) = AudioFrameQualityAnalyzer.analyze(
        samples = samples,
        sampleCount = sampleCount,
        sampleRateHz = 22_050,
        noiseFloorRms = noiseFloor,
        captureTimestampNanos = captureTimestampNanos,
        analysisStartTimestampNanos = analysisStartTimestampNanos,
        analysisEndTimestampNanos = analysisEndTimestampNanos
    )
}