package com.sharn.handpan.audio

import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimeSignature
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PcmAudio(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val sourceChannels: Int,
    val sourceFormat: String = "PCM_FLOAT_MONO"
) {
    val frameCount: Int get() = samples.size
    val durationMs: Long get() = frameCount * 1_000L / sampleRateHz.coerceAtLeast(1)

    init {
        require(sampleRateHz in 8_000..192_000)
        require(sourceChannels in 1..8)
        require(samples.none { it.isNaN() || it.isInfinite() })
    }
}

object PcmCanonicalizer {
    fun fromInterleaved(samples: FloatArray, sampleRateHz: Int, channels: Int): PcmAudio {
        require(channels in 1..8)
        require(sampleRateHz in 8_000..192_000)
        require(samples.size % channels == 0)
        val mono = FloatArray(samples.size / channels)
        var source = 0
        for (frame in mono.indices) {
            var sum = 0.0
            repeat(channels) { sum += samples[source++].takeIf { it.isFinite() } ?: 0f }
            mono[frame] = (sum / channels).toFloat().coerceIn(-1f, 1f)
        }
        return PcmAudio(mono, sampleRateHz, channels)
    }
}

/** PCM input for the offline transcription pipeline. Samples are signed mono PCM. */
data class AudioFile(
    val samples: ShortArray,
    val sampleRateHz: Int,
    val channels: Int = 1,
    val format: String = "PCM_S16LE"
) {
    val durationMs: Long
        get() = samples.size * 1_000L / sampleRateHz.coerceAtLeast(1)
}

data class ConfidenceScore(
    val value: Float,
    val reason: String
) {
    init { require(value in 0f..1f) }
}

data class TempoEstimate(
    val bpm: Int?,
    val confidence: ConfidenceScore,
    val alternatives: List<Int> = emptyList()
)

data class PitchEstimate(
    val frequencyHz: Float?,
    val noteNumber: Int?,
    val pitchName: String?,
    val centsError: Float?,
    val confidence: ConfidenceScore
)

data class OnsetEstimate(
    val timestampMs: Long,
    val confidence: ConfidenceScore,
    val intensity: Float
)

enum class TranscribedHitType { SINGLE, DOUBLE, RAPID, ACCENT, UNKNOWN }

data class TranscribedHit(
    val timestampMs: Long,
    val durationMs: Long?,
    val pitch: PitchEstimate,
    val intensity: Float,
    val onsetConfidence: ConfidenceScore,
    val pitchConfidence: ConfidenceScore,
    val classificationConfidence: ConfidenceScore,
    val hitType: TranscribedHitType,
    val rawBeat: Double?,
    val quantizedBeat: Double?
)

data class TranscribedRest(
    val startMs: Long,
    val durationMs: Long,
    val confidence: ConfidenceScore
)

enum class QualitySeverity { INFO, WARNING, ERROR }

data class AudioQualityReport(
    val durationMs: Long,
    val peak: Float,
    val rms: Float,
    val estimatedNoiseFloor: Float,
    val usable: Boolean,
    val confidence: ConfidenceScore,
    val crestFactor: Float = 0f,
    val dcOffset: Float = 0f,
    val clippingRatio: Float = 0f,
    val signalToNoiseRatioDb: Float = 0f,
    val silenceRatio: Float = 1f,
    val activeRatio: Float = 0f,
    val saturationRatio: Float = 0f,
    val severity: QualitySeverity = QualitySeverity.INFO
)

enum class TranscriptionWarning {
    EMPTY_AUDIO,
    TOO_LONG,
    INVALID_SAMPLE_RATE,
    UNSUPPORTED_FORMAT,
    INVALID_CHANNELS,
    LOW_SIGNAL,
    HIGH_NOISE,
    TEMPO_AMBIGUOUS,
    PITCH_UNKNOWN,
    QUANTIZATION_UNCERTAIN
}

data class TranscribedPattern(
    val pattern: HandpanPattern,
    val hits: List<TranscribedHit>,
    val rests: List<TranscribedRest>,
    val rawDurationMs: Long,
    val subdivision: Subdivision
)

data class TranscriptionResult(
    val pattern: TranscribedPattern?,
    val tempo: TempoEstimate,
    val onsets: List<OnsetEstimate>,
    val quality: AudioQualityReport,
    val warnings: Set<TranscriptionWarning>,
    val confidence: ConfidenceScore
)

class OfflineHandpanTranscriber(
    private val pitchConfig: NotePitchConfig = NotePitchConfig.D_KURD_9,
    private val onsetConfig: OnsetConfig = OnsetConfig()
) {
    fun transcribe(audio: PcmAudio): TranscriptionResult {
        val samples = ShortArray(audio.samples.size) { index ->
            (audio.samples[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return transcribe(AudioFile(samples, audio.sampleRateHz, 1, "PCM_S16LE"))
    }

    fun transcribe(audio: AudioFile): TranscriptionResult {
        val warnings = linkedSetOf<TranscriptionWarning>()
        if (audio.sampleRateHz !in 8_000..192_000) warnings += TranscriptionWarning.INVALID_SAMPLE_RATE
        if (audio.samples.isEmpty()) warnings += TranscriptionWarning.EMPTY_AUDIO
        if (audio.durationMs > MAX_DURATION_MS) warnings += TranscriptionWarning.TOO_LONG
        if (audio.samples.size > MAX_SAMPLE_COUNT) warnings += TranscriptionWarning.TOO_LONG
        if (audio.channels != 1) warnings += TranscriptionWarning.INVALID_CHANNELS
        if (audio.format != "PCM_S16LE") warnings += TranscriptionWarning.UNSUPPORTED_FORMAT
        if (warnings.any { it in REJECTION_WARNINGS }) {
            val quality = quality(audio, scanSignal = false)
            return TranscriptionResult(null, TempoEstimate(null, ConfidenceScore(0f, "Input rejected")), emptyList(), quality, warnings, ConfidenceScore(0f, "Input rejected"))
        }
        val quality = quality(audio)
        if (quality.rms < onsetConfig.minimumSignalRms) warnings += TranscriptionWarning.LOW_SIGNAL
        if (quality.estimatedNoiseFloor > quality.peak * 0.35f) warnings += TranscriptionWarning.HIGH_NOISE

        val onsets = detectOnsets(audio)
        val tempo = estimateTempo(onsets.map { it.timestampMs })
        if (tempo.alternatives.size > 1) warnings += TranscriptionWarning.TEMPO_AMBIGUOUS
        if (onsets.isEmpty()) {
            return TranscriptionResult(null, tempo, onsets, quality, warnings, ConfidenceScore(0f, "No reliable onset"))
        }

        val hits = onsets.mapIndexed { index, onset ->
            val next = onsets.getOrNull(index + 1)?.timestampMs
            val segmentStart = (onset.timestampMs * audio.sampleRateHz / 1_000L).toInt()
            val pitch = estimatePitch(audio.samples, audio.sampleRateHz, segmentStart)
            val rawBeat = tempo.bpm?.let { onset.timestampMs / (60_000.0 / it) }
            val step = 1.0 / Subdivision.SIXTEENTH.divisionsPerBeat / 4.0
            val quantized = rawBeat?.let { (it / step).roundToInt() * step }
            if (pitch.noteNumber == null) warnings += TranscriptionWarning.PITCH_UNKNOWN
            TranscribedHit(
                timestampMs = onset.timestampMs,
                durationMs = next?.let { (it - onset.timestampMs).coerceAtMost(2_000L) },
                pitch = pitch,
                intensity = onset.intensity,
                onsetConfidence = onset.confidence,
                pitchConfidence = pitch.confidence,
                classificationConfidence = ConfidenceScore(
                    (onset.confidence.value * pitch.confidence.value).coerceIn(0f, 1f),
                    "Onset and pitch agreement"
                ),
                hitType = when {
                    onset.intensity >= onsetConfig.accentRms -> TranscribedHitType.ACCENT
                    index > 0 && onset.timestampMs - onsets[index - 1].timestampMs <= onsetConfig.rapidHitMs -> TranscribedHitType.RAPID
                    else -> TranscribedHitType.SINGLE
                },
                rawBeat = rawBeat,
                quantizedBeat = quantized
            )
        }
        val pattern = reconstructPattern(hits, tempo.bpm ?: 60, audio.durationMs)
        if (hits.any { it.quantizedBeat != null && abs(it.rawBeat!! - it.quantizedBeat!!) > 0.12 }) {
            warnings += TranscriptionWarning.QUANTIZATION_UNCERTAIN
        }
        val confidence = ConfidenceScore(
            (quality.confidence.value * onsets.map { it.confidence.value }.average().toFloat()).coerceIn(0f, 1f),
            "Audio quality and onset agreement"
        )
        return TranscriptionResult(
            pattern = TranscribedPattern(pattern, hits, restsBetween(hits), audio.durationMs, Subdivision.SIXTEENTH),
            tempo = tempo,
            onsets = onsets,
            quality = quality,
            warnings = warnings,
            confidence = confidence
        )
    }

    private fun quality(audio: AudioFile, scanSignal: Boolean = true): AudioQualityReport {
        if (audio.samples.isEmpty()) return AudioQualityReport(0L, 0f, 0f, 0f, false, ConfidenceScore(0f, "Empty"))
        if (!scanSignal) return AudioQualityReport(audio.durationMs, 0f, 0f, 0f, false, ConfidenceScore(0f, "Input rejected"))
        var peak = 0f
        var sumSquares = 0.0
        var sum = 0.0
        var noiseSum = 0.0
        val noiseCount = max(1, audio.samples.size / 10)
        var silentCount = 0
        var clippingCount = 0
        var saturatedCount = 0
        for (index in audio.samples.indices) {
            val sample = audio.samples[index].toFloat() / Short.MAX_VALUE
            val amplitude = abs(sample)
            peak = max(peak, amplitude)
            sum += sample
            sumSquares += amplitude * amplitude
            if (index < noiseCount) noiseSum += amplitude
            if (amplitude <= SILENCE_THRESHOLD) silentCount++
            if (amplitude >= CLIPPING_THRESHOLD) clippingCount++
            if (amplitude >= SATURATION_THRESHOLD) saturatedCount++
        }
        val rms = sqrt(sumSquares / audio.samples.size).toFloat()
        val noise = (noiseSum / noiseCount).toFloat()
        val dcOffset = (sum / audio.samples.size).toFloat()
        val snr = if (noise <= 0f) 60f else (20.0 * kotlin.math.log10(max(rms / noise, 1.0f).toDouble())).toFloat()
        val silenceRatio = silentCount.toFloat() / audio.samples.size
        val clippingRatio = clippingCount.toFloat() / audio.samples.size
        val saturationRatio = saturatedCount.toFloat() / audio.samples.size
        val usable = peak >= onsetConfig.minimumPeak && rms >= onsetConfig.minimumSignalRms
        val severity = when {
            !usable -> QualitySeverity.ERROR
            clippingRatio > MAX_CLIPPING_RATIO || abs(dcOffset) > MAX_DC_OFFSET -> QualitySeverity.WARNING
            else -> QualitySeverity.INFO
        }
        return AudioQualityReport(
            durationMs = audio.durationMs,
            peak = peak,
            rms = rms,
            estimatedNoiseFloor = noise,
            usable = usable,
            confidence = ConfidenceScore(if (usable) 0.9f else 0.1f, "Signal level"),
            crestFactor = if (rms > 0f) peak / rms else 0f,
            dcOffset = dcOffset,
            clippingRatio = clippingRatio,
            signalToNoiseRatioDb = snr,
            silenceRatio = silenceRatio,
            activeRatio = 1f - silenceRatio,
            saturationRatio = saturationRatio,
            severity = severity
        )
    }

    private fun detectOnsets(audio: AudioFile): List<OnsetEstimate> {
        if (audio.samples.isEmpty()) return emptyList()
        val frame = onsetConfig.frameSize
        val hop = onsetConfig.hopSize
        val energies = mutableListOf<Float>()
        var cursor = 0
        while (cursor + frame <= audio.samples.size) {
            var sum = 0.0
            for (index in cursor until cursor + frame) {
                val value = audio.samples[index].toFloat() / Short.MAX_VALUE
                sum += value * value
            }
            energies += sqrt(sum / frame).toFloat()
            cursor += hop
        }
        val baseline = energies.sorted().take(max(1, energies.size / 2)).average().toFloat()
        val threshold = max(onsetConfig.minimumPeak, baseline * onsetConfig.thresholdMultiplier)
        val result = mutableListOf<OnsetEstimate>()
        for (index in 1 until energies.lastIndex) {
            val energy = energies[index]
            val previous = energies[index - 1]
            val next = energies[index + 1]
            val rising = energy >= previous && energy >= next && energy - previous >= onsetConfig.minimumRise
            val timestamp = (index * hop * 1_000L / audio.sampleRateHz).toLong()
            if (rising && energy >= threshold && (result.lastOrNull()?.timestampMs ?: Long.MIN_VALUE) + onsetConfig.refractoryMs <= timestamp) {
                val confidence = ((energy - baseline) / max(energy, 0.001f)).coerceIn(0f, 1f)
                result += OnsetEstimate(timestamp, ConfidenceScore(confidence, "Adaptive energy rise"), (energy / max(peak(audio), 0.001f)).coerceIn(0f, 1f))
            }
        }
        return result
    }

    private fun estimateTempo(timestamps: List<Long>): TempoEstimate {
        val intervals = timestamps.zipWithNext().map { it.second - it.first }.filter { it in 180L..2_000L }
        if (intervals.isEmpty()) return TempoEstimate(null, ConfidenceScore(0f, "Not enough intervals"))
        val median = intervals.sorted()[intervals.size / 2].toDouble()
        val raw = (60_000.0 / median).roundToInt()
        val candidates = listOf(raw / 2, raw, raw * 2).filter { it in 40..240 }.distinct()
        val confidence = (intervals.size / 4f).coerceIn(0.2f, 1f)
        return TempoEstimate(raw.coerceIn(40, 240), ConfidenceScore(confidence, "Median multi-onset interval"), candidates)
    }

    private fun estimatePitch(samples: ShortArray, sampleRateHz: Int, start: Int): PitchEstimate {
        val window = 2_048
        val from = start.coerceIn(0, max(0, samples.size - window))
        val to = min(samples.size, from + window)
        if (to - from < 512) return PitchEstimate(null, null, null, null, ConfidenceScore(0f, "Short segment"))
        var bestLag = 0
        var bestCorrelation = 0.0
        val minLag = 22
        val maxLag = min(550, (to - from) / 2)
        for (lag in minLag..maxLag) {
            var correlation = 0.0
            var energy = 0.0
            for (index in from until to - lag) {
                val a = samples[index].toDouble()
                val b = samples[index + lag].toDouble()
                correlation += a * b
                energy += a * a
            }
            val normalized = if (energy == 0.0) 0.0 else correlation / energy
            if (normalized > bestCorrelation) { bestCorrelation = normalized; bestLag = lag }
        }
        if (bestLag == 0 || bestCorrelation < 0.35) return PitchEstimate(null, null, null, null, ConfidenceScore(bestCorrelation.toFloat().coerceIn(0f, 1f), "Ambiguous harmonic"))
        val frequency = sampleRateHz.toFloat() / bestLag
        val nearest = pitchConfig.baseFrequencies.minByOrNull { abs(it.value - frequency) } ?: return PitchEstimate(null, null, null, null, ConfidenceScore(0f, "No supported note"))
        val cents = (1200.0 * kotlin.math.log2((frequency / nearest.value).toDouble())).toFloat()
        if (abs(cents) > 55f) return PitchEstimate(frequency, null, null, cents, ConfidenceScore(0.25f, "Outside note tolerance"))
        return PitchEstimate(frequency, nearest.key, pitchConfig.getPitchName(nearest.key), cents, ConfidenceScore(bestCorrelation.toFloat().coerceIn(0f, 1f), "Autocorrelation"))
    }

    private fun reconstructPattern(hits: List<TranscribedHit>, bpm: Int, durationMs: Long): HandpanPattern {
        val events = hits.map { hit ->
            NoteEvent(
                noteNumber = hit.pitch.noteNumber ?: 0,
                beatPosition = hit.quantizedBeat ?: 0.0,
                duration = 0.25,
                velocity = hit.intensity,
                accent = hit.hitType == TranscribedHitType.ACCENT,
                isRest = hit.pitch.noteNumber == null
            )
        }.distinctBy { "${it.beatPosition}:${it.noteNumber}:${it.isRest}" }.sortedBy { it.beatPosition }
        val beats = max(1.0, durationMs / (60_000.0 / bpm))
        val bars = max(1, kotlin.math.ceil(beats / 4.0).toInt())
        return HandpanPattern("transcription-${durationMs}-${events.size}", "Offline transcription", "Generated from audio", bpm, TimeSignature.Common44, bars, events, recommendedSubdivision = Subdivision.SIXTEENTH)
    }

    private fun restsBetween(hits: List<TranscribedHit>): List<TranscribedRest> = hits.zipWithNext().mapNotNull {
        val gap = it.second.timestampMs - it.first.timestampMs
        if (gap > 120L) TranscribedRest(it.first.timestampMs, gap, ConfidenceScore(0.8f, "Observed silence")) else null
    }

    private fun peak(audio: AudioFile): Float = audio.samples.maxOfOrNull { abs(it.toFloat() / Short.MAX_VALUE) } ?: 0f

    data class OnsetConfig(
        val frameSize: Int = 256,
        val hopSize: Int = 128,
        val thresholdMultiplier: Float = 2.5f,
        val minimumRise: Float = 0.02f,
        val minimumPeak: Float = 0.08f,
        val minimumSignalRms: Float = 0.01f,
        val accentRms: Float = 0.65f,
        val refractoryMs: Long = 35L,
        val rapidHitMs: Long = 180L
    )

    companion object {
        const val MAX_DURATION_MS = 20_000L
        private const val MAX_SAMPLE_COUNT = 192_000 * 20
        private const val SILENCE_THRESHOLD = 0.01f
        private const val CLIPPING_THRESHOLD = 0.999f
        private const val SATURATION_THRESHOLD = 0.98f
        private const val MAX_CLIPPING_RATIO = 0.02f
        private const val MAX_DC_OFFSET = 0.1f
        private val REJECTION_WARNINGS = setOf(
            TranscriptionWarning.EMPTY_AUDIO,
            TranscriptionWarning.TOO_LONG,
            TranscriptionWarning.INVALID_SAMPLE_RATE,
            TranscriptionWarning.INVALID_CHANNELS,
            TranscriptionWarning.UNSUPPORTED_FORMAT
        )
    }
}
