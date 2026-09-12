package com.sharn.handpan

import com.sharn.handpan.audio.OnsetAndPitchMatcher
import com.sharn.handpan.audio.YinPitchDetector
import com.sharn.handpan.model.AudioFrameQualityAnalyzer
import com.sharn.handpan.model.AudioFrameStatus
import com.sharn.handpan.model.NotePitchConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AudioAnalysisBenchmarkTest {
    private val sampleRate = 22_050
    private val frameSize = 2_048

    @Test
    fun onsetBenchmarkReportsNoFalsePositiveForSilenceAndOneForSustain() {
        val silenceMetrics = benchmarkOnsets(AudioFixtures.silenceFrames(16))
        val sustainMetrics = benchmarkOnsets(AudioFixtures.attackAndSustainFrames())

        println("onset fixture=silence expected=0 detected=${silenceMetrics.detected} falsePositives=${silenceMetrics.falsePositives}")
        println("onset fixture=attack-sustain expected=1 detected=${sustainMetrics.detected} precision=${sustainMetrics.precision} recall=${sustainMetrics.recall} timingErrorMs=${sustainMetrics.timingErrorMs}")
        assertEquals(0, silenceMetrics.detected)
        assertEquals(1, sustainMetrics.detected)
        assertEquals(1f, sustainMetrics.precision, 0f)
        assertEquals(1f, sustainMetrics.recall, 0f)
    }

    @Test
    fun repeatedHandpanStrikesProduceOneOnsetPerAttack() {
        val metrics = benchmarkOnsets(AudioFixtures.repeatedStrikeFrames(strikeCount = 4, silentFramesBetween = 2))

        println("onset fixture=repeated-handpan expected=${metrics.expected} detected=${metrics.detected} truePositives=${metrics.truePositives} falsePositives=${metrics.falsePositives} falseNegatives=${metrics.falseNegatives} precision=${metrics.precision} recall=${metrics.recall} timingErrorMs=${metrics.timingErrorMs}")
        assertEquals(4, metrics.expected)
        assertEquals(4, metrics.detected)
        assertEquals(0, metrics.falsePositives)
        assertEquals(0, metrics.falseNegatives)
    }

    @Test
    fun pitchBenchmarkMeasuresLowMidAndHighHandpanRegisters() {
        val cases = listOf(
            "low" to 146.83,
            "mid" to 220.0,
            "high" to 440.0
        )
        cases.forEach { (name, expectedHz) ->
            val result = YinPitchDetector(sampleRate = sampleRate).detectPitch(
                AudioFixtures.sineFrame(expectedHz, amplitude = 0.8)
            )
            val errorHz = abs(result.frequencyHz - expectedHz.toFloat())
            val centsError = abs(YinPitchDetector.calculateCentsDifference(result.frequencyHz, expectedHz.toFloat()))
            println("pitch fixture=$name expectedHz=$expectedHz detectedHz=${result.frequencyHz} errorHz=$errorHz errorCents=$centsError confidence=${result.confidence}")
            assertTrue("$name pitch should be detected", result.isPitched)
            assertTrue("$name Hz error=$errorHz", errorHz < 3.5f)
            assertTrue("$name cents error=$centsError", centsError < 45f)
            assertTrue("$name confidence=${result.confidence}", result.confidence >= 0.8f)
        }
    }

    @Test
    fun attackAndSustainHaveStablePitchWithoutSecondOnset() {
        val matcher = OnsetAndPitchMatcher(sampleRate)
        val config = NotePitchConfig.D_KURD_9
        var previousRms = 0f
        var onsetCount = 0
        var pitchCount = 0
        val frames = AudioFixtures.attackAndSustainFrames()

        frames.forEach { frame ->
            val evaluation = matcher.processFrame(
                buffer = frame.samples,
                readSamples = frame.samples.size,
                rms = frame.rms,
                lastRms = previousRms,
                scaleConfig = config
            )
            if (evaluation.isStrike) onsetCount++
            if (evaluation.detectedFreqHz > 0f) pitchCount++
            previousRms = frame.rms
        }

        println("interaction fixture=attack-sustain onsets=$onsetCount pitchedFrames=$pitchCount")
        assertEquals(1, onsetCount)
        assertTrue("sustain should provide stable pitch frames", pitchCount >= 4)
    }

    @Test
    fun matchingReportsExactWithinToleranceAndOutsideTolerance() {
        val matcher = OnsetAndPitchMatcher(sampleRate)
        val config = NotePitchConfig.D_KURD_9
        val expected = config.getFrequency(NotePitchConfig.NOTE_DING)
        val within = expected * 2.0.pow(40.0 / 1200.0).toFloat()
        val outside = expected * 2.0.pow(100.0 / 1200.0).toFloat()

        val exactMatch = matcher.matchToScaleByCents(expected, config)
        val withinMatch = matcher.matchToScaleByCents(within, config)
        val outsideMatch = matcher.matchToScaleByCents(outside, config)

        println("matching expected=${exactMatch.first} exactCents=${exactMatch.second} within=${withinMatch.first} withinCents=${withinMatch.second} outside=${outsideMatch.first} outsideCents=${outsideMatch.second}")
        assertEquals(NotePitchConfig.NOTE_DING, exactMatch.first)
        assertEquals(NotePitchConfig.NOTE_DING, withinMatch.first)
        assertEquals(null, outsideMatch.first)
    }

    @Test
    fun qualityPolicyRejectsInvalidHandpanFramesBeforeSuccessfulStrike() {
        val cases = listOf(
            AudioFrameStatus.SILENT to ShortArray(frameSize),
            AudioFrameStatus.LOW_SIGNAL to ShortArray(frameSize) { 300 },
            AudioFrameStatus.NOISY to ShortArray(frameSize) { 500 },
            AudioFrameStatus.OVERLOADED to ShortArray(frameSize) { Short.MAX_VALUE }
        )
        cases.forEach { (expectedStatus, samples) ->
            val noiseFloor = when (expectedStatus) {
                AudioFrameStatus.NOISY -> 0.014f
                else -> 0.001f
            }
            val quality = AudioFrameQualityAnalyzer.analyze(
                samples = samples,
                sampleCount = samples.size,
                sampleRateHz = sampleRate,
                noiseFloorRms = noiseFloor,
                captureTimestampNanos = 1_000L,
                analysisStartTimestampNanos = 2_000L,
                analysisEndTimestampNanos = 3_000L
            )
            println("quality fixture=${expectedStatus.name.lowercase()} status=${quality.status} rms=${quality.rms} peak=${quality.peak} snrDb=${quality.signalToNoiseRatioDb}")
            assertEquals(expectedStatus, quality.status)
            if (expectedStatus == AudioFrameStatus.NOISY) {
                assertTrue(quality.signalConfidence in 0f..1f)
            } else {
                assertEquals(0f, quality.signalConfidence, 0f)
            }
        }
    }

    private fun benchmarkOnsets(frames: List<AudioFixtures.Frame>): OnsetMetrics {
        val matcher = OnsetAndPitchMatcher(sampleRate)
        var previousRms = 0f
        val detectedFrames = mutableListOf<Int>()
        frames.forEachIndexed { index, frame ->
            val evaluation = matcher.processFrame(
                buffer = frame.samples,
                readSamples = frame.samples.size,
                rms = frame.rms,
                lastRms = previousRms,
                scaleConfig = NotePitchConfig.D_KURD_9
            )
            if (evaluation.isStrike) detectedFrames += index
            previousRms = frame.rms
        }
        val expectedFrames = frames.mapIndexedNotNull { index, frame -> if (frame.expectedOnset) index else null }
        val falsePositives = detectedFrames.count { detected -> expectedFrames.none { abs(it - detected) <= 0 } }
        val falseNegatives = expectedFrames.count { expected -> detectedFrames.none { it == expected } }
        val truePositives = detectedFrames.size - falsePositives
        val precision = if (detectedFrames.isEmpty()) 1f else truePositives.toFloat() / detectedFrames.size
        val recall = if (expectedFrames.isEmpty()) 1f else truePositives.toFloat() / expectedFrames.size
        val timingErrorMs = if (truePositives == 0) 0f else {
            detectedFrames.filter { it in expectedFrames }
                .map { detected -> (detected - expectedFrames.first { it == detected }).toFloat() * frameSize * 1_000f / sampleRate }
                .average().toFloat()
        }
        return OnsetMetrics(expectedFrames.size, detectedFrames.size, falsePositives, falseNegatives, precision, recall, truePositives, timingErrorMs)
    }

    private data class OnsetMetrics(
        val expected: Int,
        val detected: Int,
        val falsePositives: Int,
        val falseNegatives: Int,
        val precision: Float,
        val recall: Float,
        val truePositives: Int,
        val timingErrorMs: Float
    )

    private fun Double.pow(exponent: Double): Double = kotlin.math.exp(exponent * kotlin.math.ln(this))
}

private object AudioFixtures {
    data class Frame(
        val samples: ShortArray,
        val rms: Float,
        val expectedOnset: Boolean = false
    )

    fun silenceFrames(count: Int): List<Frame> = List(count) { Frame(ShortArray(2_048), 0f) }

    fun attackAndSustainFrames(): List<Frame> {
        val silence = silenceFrames(8)
        val attack = Frame(sineFrame(220.0, 0.8), rms = 0.08f, expectedOnset = true)
        val sustain = List(5) { Frame(sineFrame(220.0, 0.8), rms = 0.08f) }
        return silence + attack + sustain
    }

    fun repeatedStrikeFrames(strikeCount: Int, silentFramesBetween: Int): List<Frame> {
        val frames = mutableListOf<Frame>()
        frames += silenceFrames(8)
        repeat(strikeCount) { index ->
            frames += Frame(sineFrame(220.0, 0.8), rms = 0.08f, expectedOnset = true)
            if (index < strikeCount - 1) frames += silenceFrames(silentFramesBetween)
        }
        return frames
    }

    fun sineFrame(frequencyHz: Double, amplitude: Double): ShortArray = ShortArray(2_048) { index ->
        val sample = sin(2.0 * PI * frequencyHz * index / 22_050.0) * amplitude
        (sample * Short.MAX_VALUE).toInt().toShort()
    }
}
