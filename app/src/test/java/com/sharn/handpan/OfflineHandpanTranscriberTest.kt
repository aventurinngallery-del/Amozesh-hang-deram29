package com.sharn.handpan

import com.sharn.handpan.audio.AudioFile
import com.sharn.handpan.audio.OfflineHandpanTranscriber
import com.sharn.handpan.audio.PcmCanonicalizer
import com.sharn.handpan.audio.TranscriptionWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class OfflineHandpanTranscriberTest {
    private val sampleRate = 8_000

    private fun signal(vararg hitMs: Int): AudioFile {
        val samples = ShortArray(sampleRate * 3)
        hitMs.forEach { startMs ->
            val start = startMs * sampleRate / 1_000
            for (index in 0 until sampleRate / 12) {
                val position = start + index
                if (position < samples.size) {
                    samples[position] = (sin(index * 2.0 * PI * 220.0 / sampleRate) * 20_000).toInt().toShort()
                }
            }
        }
        return AudioFile(samples, sampleRate)
    }

    @Test
    fun silenceProducesNoFalsePositive() {
        val result = OfflineHandpanTranscriber().transcribe(AudioFile(ShortArray(sampleRate), sampleRate))
        assertTrue(result.onsets.isEmpty())
        assertTrue(TranscriptionWarning.LOW_SIGNAL in result.warnings)
        assertEquals(null, result.pattern)
    }

    @Test
    fun detectsRepeatedAndRapidOnsetsDeterministically() {
        val result = OfflineHandpanTranscriber().transcribe(signal(250, 750, 1_100))
        assertEquals(listOf(256L, 752L, 1_104L), result.onsets.map { it.timestampMs })
        assertEquals(3, result.pattern?.hits?.size)
        assertEquals(TranscriptionWarning.PITCH_UNKNOWN !in result.warnings, true)
    }

    @Test
    fun rejectsAudioLongerThanTwentySeconds() {
        val result = OfflineHandpanTranscriber().transcribe(AudioFile(ShortArray(sampleRate * 21), sampleRate))
        assertTrue(TranscriptionWarning.TOO_LONG in result.warnings)
    }

    @Test
    fun downmixesStereoDeterministicallyAndSanitizesAmplitude() {
        val pcm = PcmCanonicalizer.fromInterleaved(floatArrayOf(1f, -1f, 0.5f, 0.25f), sampleRate, 2)
        assertEquals(2, pcm.frameCount)
        assertEquals(0f, pcm.samples[0], 0.0001f)
        assertEquals(0.375f, pcm.samples[1], 0.0001f)
    }

    @Test
    fun qualityReportExposesClippingAndSignalMetrics() {
        val result = OfflineHandpanTranscriber().transcribe(signal(250, 1_250, 2_250))
        assertTrue(result.quality.crestFactor >= 1f)
        assertTrue(result.quality.activeRatio >= 0f)
        assertTrue(result.quality.clippingRatio >= 0f)
    }

    @Test
    fun preservesRawAndQuantizedTimingSeparately() {
        val hits = OfflineHandpanTranscriber().transcribe(signal(487, 1_487, 2_487)).pattern?.hits.orEmpty()
        val hit = hits.firstOrNull()
        assertTrue(hit?.rawBeat != hit?.quantizedBeat)
        assertEquals(496L, hit?.timestampMs)
    }
}
