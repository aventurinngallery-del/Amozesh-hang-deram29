package com.sharn.handpan.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

/**
 * Procedural Native Android Ambient Audio Generator & Jam Drone Synthesizer.
 * Supports:
 * 1. "desert_drone" (Warm harmonic D minor root drone pad with gentle LFO modulation)
 * 2. "cajon_groove" (Rhythmic acoustic cajon pulse at configurable tempo)
 * 3. "rain_nature" (Atmospheric soothing pink-noise rain & wind soundscape)
 */
class AmbienceEngine {

    private var currentJob: Job? = null
    private var activeAudioTrack: AudioTrack? = null
    private var activeTrackId: String? = null
    private var volume: Float = 0.5f

    fun getActiveTrackId(): String? = activeTrackId
    fun isPlaying(): Boolean = activeTrackId != null

    fun setVolume(vol: Float) {
        this.volume = vol.coerceIn(0.0f, 1.0f)
        try {
            activeAudioTrack?.setVolume(this.volume)
        } catch (_: Exception) {}
    }

    /**
     * Starts background ambient generation in a background IO coroutine.
     */
    fun startAmbience(trackId: String, rootFreq: Float = 146.83f, bpm: Int = 85) {
        stopAmbience()
        activeTrackId = trackId

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeAudioTrack = audioTrack
            audioTrack.setVolume(volume)
            audioTrack.play()

            currentJob = CoroutineScope(Dispatchers.Default).launch {
                val shortBuffer = ShortArray(bufferSize)
                var phase1 = 0.0
                var phase2 = 0.0
                var phase3 = 0.0
                var lfoPhase = 0.0
                var lastNoiseOut = 0.0
                var sampleIndex = 0L

                val samplesPerBeat = (sampleRate * 60) / bpm

                while (isActive) {
                    when (trackId) {
                        "desert_drone" -> {
                            // Harmonic D Minor Drone (root, 5th, octave) with smooth LFO
                            val f1 = rootFreq * 0.5 // Sub-bass octave
                            val f2 = rootFreq       // Root
                            val f3 = rootFreq * 1.5 // 5th overtone
                            val lfoRate = 0.15      // 0.15 Hz slow breathing swell

                            for (i in shortBuffer.indices) {
                                val lfo = 0.7 + 0.3 * sin(2.0 * PI * lfoPhase)
                                val sample = (
                                    0.45 * sin(2.0 * PI * phase1) +
                                    0.35 * sin(2.0 * PI * phase2) +
                                    0.20 * sin(2.0 * PI * phase3)
                                ) * lfo

                                shortBuffer[i] = (sample * 16000.0).toInt().coerceIn(-32767, 32767).toShort()

                                phase1 += f1 / sampleRate
                                if (phase1 > 1.0) phase1 -= 1.0
                                phase2 += f2 / sampleRate
                                if (phase2 > 1.0) phase2 -= 1.0
                                phase3 += f3 / sampleRate
                                if (phase3 > 1.0) phase3 -= 1.0
                                lfoPhase += lfoRate / sampleRate
                                if (lfoPhase > 1.0) lfoPhase -= 1.0
                            }
                        }

                        "rain_nature" -> {
                            // Pink noise / atmospheric gentle rain filter
                            for (i in shortBuffer.indices) {
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastNoiseOut = (lastNoiseOut + 0.02 * white) / 1.02
                                val lfo = 0.8 + 0.2 * sin(2.0 * PI * lfoPhase)
                                val sample = lastNoiseOut * 2.2 * lfo

                                shortBuffer[i] = (sample * 18000.0).toInt().coerceIn(-32767, 32767).toShort()

                                lfoPhase += 0.08 / sampleRate
                                if (lfoPhase > 1.0) lfoPhase -= 1.0
                            }
                        }

                        "cajon_groove" -> {
                            // Rhythmic percussive acoustic pulse
                            for (i in shortBuffer.indices) {
                                val currentBeatSample = sampleIndex % samplesPerBeat
                                val beatIndex = (sampleIndex / samplesPerBeat) % 4
                                val beatFraction = currentBeatSample.toDouble() / samplesPerBeat

                                var sample = 0.0
                                // Downbeat Bass (Beat 0 and 2)
                                if ((beatIndex == 0L || beatIndex == 2L) && beatFraction < 0.25) {
                                    val env = kotlin.math.exp(-beatFraction * 20.0)
                                    val bassFreq = 65.0 - beatFraction * 25.0
                                    sample += sin(2.0 * PI * bassFreq * (currentBeatSample.toDouble() / sampleRate)) * env * 0.7
                                }
                                // Slap (Beat 1 and 3)
                                if ((beatIndex == 1L || beatIndex == 3L) && beatFraction < 0.15) {
                                    val env = kotlin.math.exp(-beatFraction * 40.0)
                                    val noise = Random.nextDouble(-1.0, 1.0)
                                    sample += noise * env * 0.5
                                }

                                shortBuffer[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
                                sampleIndex++
                            }
                        }

                        else -> {
                            shortBuffer.fill(0)
                        }
                    }

                    audioTrack.write(shortBuffer, 0, shortBuffer.size)
                }
            }
        } catch (e: Exception) {
            Log.e("AmbienceEngine", "Error starting ambient soundscape: ${e.message}", e)
            stopAmbience()
        }
    }

    /**
     * Cleanly stops playback and releases AudioTrack resources.
     */
    fun stopAmbience() {
        currentJob?.cancel()
        currentJob = null
        activeTrackId = null
        try {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        } catch (_: Exception) {}
        activeAudioTrack = null
    }
}
