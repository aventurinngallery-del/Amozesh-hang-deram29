package com.sharn.handpan

import com.sharn.handpan.model.AudioCalibrationSession
import com.sharn.handpan.model.AudioCalibrationState
import com.sharn.handpan.model.AudioFrameQualityAnalyzer
import com.sharn.handpan.model.AudioFrameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCalibrationSessionTest {
    @Test
    fun validSignalBecomesReadyAfterStableFrames() {
        val session = AudioCalibrationSession(requiredValidFrames = 2)
        session.start()

        assertEquals(AudioCalibrationState.LISTENING, session.observe(validQuality(1_000L)).state)
        assertEquals(AudioCalibrationState.READY, session.observe(validQuality(2_000L)).state)
    }

    @Test
    fun silenceNoiseOverloadAndInvalidInputHaveExplicitStates() {
        val session = AudioCalibrationSession()
        session.start()

        assertEquals(AudioCalibrationState.NO_SIGNAL, session.observe(quality(AudioFrameStatus.SILENT, 1_000L)).state)
        assertEquals(AudioCalibrationState.TOO_NOISY, session.observe(quality(AudioFrameStatus.NOISY, 2_000L)).state)
        assertEquals(AudioCalibrationState.OVERLOADED, session.observe(quality(AudioFrameStatus.OVERLOADED, 3_000L)).state)
        val failed = session.observe(quality(AudioFrameStatus.INVALID, 4_000L))
        assertEquals(AudioCalibrationState.FAILED, failed.state)
        assertTrue(failed.failureReason!!.isNotBlank())
    }

    @Test
    fun resetReturnsToNotStartedAndRejectsOutOfOrderTimestamps() {
        val session = AudioCalibrationSession()
        session.start()
        session.observe(validQuality(2_000L))

        val failed = session.observe(validQuality(1_000L))
        assertEquals(AudioCalibrationState.FAILED, failed.state)
        assertEquals(AudioCalibrationState.NOT_STARTED, session.reset().state)
        assertEquals(AudioCalibrationState.NOT_STARTED, session.observe(validQuality(3_000L)).state)
    }

    private fun validQuality(timestamp: Long) = quality(AudioFrameStatus.VALID, timestamp)

    private fun quality(status: AudioFrameStatus, timestamp: Long) = AudioFrameQualityAnalyzer.analyze(
        samples = when (status) {
            AudioFrameStatus.SILENT -> ShortArray(128)
            AudioFrameStatus.LOW_SIGNAL -> ShortArray(128) { 300 }
            AudioFrameStatus.NOISY -> ShortArray(128) { 500 }
            AudioFrameStatus.OVERLOADED -> ShortArray(128) { Short.MAX_VALUE }
            AudioFrameStatus.VALID,
            AudioFrameStatus.INVALID -> ShortArray(128) { 16_000 }
        },
        sampleCount = if (status == AudioFrameStatus.INVALID) 129 else 128,
        sampleRateHz = 22_050,
        noiseFloorRms = if (status == AudioFrameStatus.NOISY) 0.014f else 0.001f,
        captureTimestampNanos = timestamp,
        analysisStartTimestampNanos = timestamp + 1L,
        analysisEndTimestampNanos = timestamp + 2L
    )
}