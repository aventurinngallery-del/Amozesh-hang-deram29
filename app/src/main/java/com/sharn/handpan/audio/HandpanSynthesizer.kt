package com.sharn.handpan.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Modal and physical-acoustic resonance synthesizer for Handpan notes, Slap transients, and Metronome clicks.
 *
 * Implements:
 * 1. Fundamental mode (f0)
 * 2. Octave mode (2*f0) - dominant on tuned steel membrane
 * 3. Compound fifth mode (3*f0)
 * 4. Helmholtz cavity body resonance for center Ding (0.5*f0)
 * 5. Attack transient mallet strike with soft compression limiter to strictly prevent clipping within [-1.0, 1.0].
 */
object HandpanSynthesizer {

    private const val SAMPLE_RATE = 44100

    /**
     * Generates a 16-bit PCM buffer for an acoustic Handpan note.
     *
     * @param frequency Fundamental frequency in Hz (e.g. 146.83 Hz for Ding D3)
     * @param durationSeconds Duration of resonant decay
     * @param isDing Whether this is the center bass Ding
     * @param velocity Amplitude scaling factor (0.0 to 1.0)
     */
    fun generateHandpanSample(
        frequency: Float,
        durationSeconds: Float = 1.8f,
        isDing: Boolean = false,
        velocity: Float = 0.9f
    ): ByteArray {
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val shortBuffer = ShortArray(numSamples)

        val f0 = frequency.toDouble()
        val f1 = f0 * 2.0 // Octave
        val f2 = f0 * 3.0 // Compound 5th
        val fHelmholtz = if (isDing) f0 * 0.5 else 0.0

        val twoPi = 2.0 * PI

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // 5ms smooth attack transient
            val attack = if (t < 0.005) t / 0.005 else 1.0

            // Multi-exponential acoustic modal decay
            val decay0 = exp(-t * if (isDing) 1.8 else 2.5)
            val decay1 = exp(-t * if (isDing) 3.0 else 4.0)
            val decay2 = exp(-t * if (isDing) 5.0 else 6.5)
            val decayHelmholtz = if (isDing) exp(-t * 1.2) else 0.0

            // Modal synthesis
            var wave = 0.52 * sin(twoPi * f0 * t) * decay0 +
                    0.28 * sin(twoPi * f1 * t) * decay1 +
                    0.12 * sin(twoPi * f2 * t) * decay2

            if (isDing && decayHelmholtz > 0.0) {
                wave += 0.18 * sin(twoPi * fHelmholtz * t) * decayHelmholtz
            }

            // Normalization limiter & velocity scaling
            val amplitude = wave * attack * velocity.coerceIn(0.1f, 1.0f)
            val clamped = amplitude.coerceIn(-0.98, 0.98)

            shortBuffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        val byteBuffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in shortBuffer) {
            byteBuffer.putShort(sample)
        }
        return byteBuffer.array()
    }

    /**
     * Generates a percussive Slap / Tak hit on the interstitial steel body.
     */
    fun generateSlapSample(velocity: Float = 0.9f): ByteArray {
        val durationSeconds = 0.12f
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val shortBuffer = ShortArray(numSamples)

        val twoPi = 2.0 * PI
        val fSlapLow = 420.0
        val fSlapMid = 980.0
        val fSlapHigh = 1850.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (t < 0.003) t / 0.003 else 1.0

            val decayLow = exp(-t * 35.0)
            val decayMid = exp(-t * 55.0)
            val decayHigh = exp(-t * 90.0)

            val wave = 0.40 * sin(twoPi * fSlapLow * t) * decayLow +
                    0.45 * sin(twoPi * fSlapMid * t) * decayMid +
                    0.25 * sin(twoPi * fSlapHigh * t) * decayHigh

            val amplitude = wave * attack * velocity.coerceIn(0.1f, 1.0f)
            val clamped = amplitude.coerceIn(-0.98, 0.98)
            shortBuffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        val byteBuffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in shortBuffer) {
            byteBuffer.putShort(sample)
        }
        return byteBuffer.array()
    }

    /**
     * Generates a crisp wooden clave / click metronome tick.
     */
    fun generateClickSample(isAccent: Boolean): ByteArray {
        val durationSeconds = 0.045f
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val freq = if (isAccent) 1500.0 else 850.0
        val shortBuffer = ShortArray(numSamples)

        val twoPi = 2.0 * PI
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (t < 0.002) t / 0.002 else 1.0
            val decay = exp(-t * 90.0)
            val wave = sin(twoPi * freq * t) * attack * decay * (if (isAccent) 0.95 else 0.70)
            val clamped = wave.coerceIn(-0.98, 0.98)
            shortBuffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        val byteBuffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in shortBuffer) {
            byteBuffer.putShort(sample)
        }
        return byteBuffer.array()
    }

    /**
     * Converts 16-bit PCM bytes into standard WAV format header for SoundPool.
     */
    fun pcmToWav(pcmData: ByteArray): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val channels: Short = 1
        val sampleRate = SAMPLE_RATE
        val byteRate = sampleRate * channels * 2
        val blockAlign = (channels * 2).toShort()
        val bitsPerSample: Short = 16

        val buffer = ByteBuffer.allocate(44 + totalAudioLen).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1.toShort()) // PCM format
        buffer.putShort(channels)
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample)

        // data chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(totalAudioLen)
        buffer.put(pcmData)

        return buffer.array()
    }
}
