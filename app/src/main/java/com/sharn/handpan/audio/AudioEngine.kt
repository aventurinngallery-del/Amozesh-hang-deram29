package com.sharn.handpan.audio

import com.sharn.handpan.BuildConfig
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.sharn.handpan.model.NotePitchConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance audio engine managing SoundPool polyphonic playback for Handpan notes and Metronome.
 * Supports Ding (0), Tone fields 1..8, and Slap strike (9/S).
 */
open class AudioEngine(private val context: Context? = null) {

    private val soundPool: SoundPool?
    private val audioFocusManager: AudioFocusManager?
    private val loadedSoundIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val soundMapLock = Any()
    private val noteSoundMap = mutableMapOf<Int, Int>() // Note number (0=Ding, 1..8, 9=Slap) -> SoundPool SoundId
    private val accentSoundMap = mutableMapOf<Int, Int>() // Note number -> Accented SoundId
    private var clickAccentId: Int = 0
    private var clickRegularId: Int = 0

    @Volatile
    private var pitchConfig: NotePitchConfig = NotePitchConfig()
    private var masterVolume: Float = 1.0f
    private var metronomeVolume: Float = 0.8f
    private var isMuted: Boolean = false
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sampleLoadJob: Job? = null
    private val sampleGeneration = AtomicLong(0L)
    @Volatile
    private var released = false

    init {
        audioFocusManager = context?.let { AudioFocusManager(it) }
        if (context != null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(20) // Allow rich acoustic ring and overlap
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && !released) {
                    loadedSoundIds.add(sampleId)
                }
            }
            soundPool = pool
            loadSamples(pitchConfig)
        } else {
            soundPool = null
        }
    }

    open fun getPitchConfig(): NotePitchConfig = pitchConfig

    /**
     * Loads or synthesizes samples for Ding (0), all 8 surrounding notes, Slap (9), and metronome clicks.
     * Checks for custom recorded WAV samples first; if absent, synthesizes high-fidelity tones.
     */
    open fun loadSamples(config: NotePitchConfig) {
        this.pitchConfig = config
        val currentCtx = context ?: return
        val generation = sampleGeneration.incrementAndGet()
        sampleLoadJob?.cancel()
        sampleLoadJob = engineScope.launch {
            try {
                if (!isCurrentGeneration(generation)) return@launch
                // 1. Synthesize clicks
                val clickAccentWav = HandpanSynthesizer.pcmToWav(HandpanSynthesizer.generateClickSample(isAccent = true))
                val clickRegularWav = HandpanSynthesizer.pcmToWav(HandpanSynthesizer.generateClickSample(isAccent = false))
                clickAccentId = loadWavFromBytes(clickAccentWav, "click_accent_$generation.wav")
                clickRegularId = loadWavFromBytes(clickRegularWav, "click_regular_$generation.wav")

                // 2. Load Ding (Note 0) - custom sample or synth
                loadSingleNote(NotePitchConfig.NOTE_DING, config, generation)

                // 3. Load surrounding notes 1 through 8
                for (noteNumber in 1..8) {
                    loadSingleNote(noteNumber, config, generation)
                }

                // 4. Load Slap / Tak strike (Note 9 / S)
                loadSingleNote(NotePitchConfig.NOTE_SLAP, config, generation)

            } catch (e: Exception) {
                Log.e("AudioEngine", "Error initializing audio samples: ${e.message}", e)
            }
        }
    }

    /**
     * Reloads or updates a single note sample dynamically (used after recording a custom strike).
     */
    open fun reloadNoteSample(noteNumber: Int) {
        if (context == null) return
        val generation = sampleGeneration.incrementAndGet()
        val config = pitchConfig
        sampleLoadJob?.cancel()
        sampleLoadJob = engineScope.launch {
            loadSingleNote(noteNumber, config, generation)
        }
    }

    /**
     * Checks if a custom acoustic recorded sample exists for this note number.
     */
    open fun hasCustomSample(noteNumber: Int): Boolean {
        val currentCtx = context ?: return false
        return CustomSampleRecorder.hasCustomSample(currentCtx, noteNumber)
    }

    open fun isCustomSampleLoaded(noteNumber: Int): Boolean = hasCustomSample(noteNumber)

    /**
     * Deletes custom sample for a note and reverts back to the synthetic acoustic tone.
     */
    open fun removeCustomSample(noteNumber: Int) {
        val currentCtx = context ?: return
        CustomSampleRecorder.deleteCustomSample(currentCtx, noteNumber)
        reloadNoteSample(noteNumber)
    }

    private fun loadSingleNote(noteNumber: Int, config: NotePitchConfig, generation: Long) {
        val currentCtx = context ?: return
        val pool = soundPool ?: return
        if (!isCurrentGeneration(generation)) return
        val customFile = CustomSampleRecorder.getCustomSampleFile(currentCtx, noteNumber)
        if (customFile.exists() && customFile.length() > 44) {
            try {
                val soundId = pool.load(customFile.absolutePath, 1)
                replaceNoteSounds(noteNumber, soundId, soundId, generation)
                if (BuildConfig.DEBUG) {
                    Log.d("AudioEngine", "Loaded custom real recorded sample for note $noteNumber")
                }
                return
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed to load custom sample for note $noteNumber, falling back to synth", e)
            }
        }

        // Fallback or default: Synthesize based on pitch config
        if (noteNumber == NotePitchConfig.NOTE_DING) {
            val dingFreq = config.getFrequency(NotePitchConfig.NOTE_DING)
            val dingRegularPcm = HandpanSynthesizer.generateHandpanSample(
                frequency = dingFreq,
                durationSeconds = 2.4f,
                isDing = true,
                velocity = 0.9f
            )
            val dingRegularWav = HandpanSynthesizer.pcmToWav(dingRegularPcm)
            val regularId = loadWavFromBytes(dingRegularWav, "note_ding_regular_$generation.wav")

            val dingAccentPcm = HandpanSynthesizer.generateHandpanSample(
                frequency = dingFreq,
                durationSeconds = 2.6f,
                isDing = true,
                velocity = 1.0f
            )
            val dingAccentWav = HandpanSynthesizer.pcmToWav(dingAccentPcm)
            val accentId = loadWavFromBytes(dingAccentWav, "note_ding_accent_$generation.wav")
            replaceNoteSounds(NotePitchConfig.NOTE_DING, regularId, accentId, generation)
        } else if (noteNumber == NotePitchConfig.NOTE_SLAP) {
            val slapRegularPcm = HandpanSynthesizer.generateSlapSample(velocity = 0.85f)
            val slapRegularWav = HandpanSynthesizer.pcmToWav(slapRegularPcm)
            val regularId = loadWavFromBytes(slapRegularWav, "slap_regular_$generation.wav")

            val slapAccentPcm = HandpanSynthesizer.generateSlapSample(velocity = 1.0f)
            val slapAccentWav = HandpanSynthesizer.pcmToWav(slapAccentPcm)
            val accentId = loadWavFromBytes(slapAccentWav, "slap_accent_$generation.wav")
            replaceNoteSounds(NotePitchConfig.NOTE_SLAP, regularId, accentId, generation)
        } else {
            val freq = config.getFrequency(noteNumber)
            val regularPcm = HandpanSynthesizer.generateHandpanSample(
                frequency = freq,
                durationSeconds = 1.7f,
                isDing = false,
                velocity = 0.85f
            )
            val regularWav = HandpanSynthesizer.pcmToWav(regularPcm)
            val regularId = loadWavFromBytes(regularWav, "note_${noteNumber}_regular_$generation.wav")

            val accentPcm = HandpanSynthesizer.generateHandpanSample(
                frequency = freq,
                durationSeconds = 1.9f,
                isDing = false,
                velocity = 1.0f
            )
            val accentWav = HandpanSynthesizer.pcmToWav(accentPcm)
            val accentId = loadWavFromBytes(accentWav, "note_${noteNumber}_accent_$generation.wav")
            replaceNoteSounds(noteNumber, regularId, accentId, generation)
        }
    }

    private fun isCurrentGeneration(generation: Long): Boolean =
        !released && sampleGeneration.get() == generation

    private fun replaceNoteSounds(noteNumber: Int, regularId: Int, accentId: Int, generation: Long) {
        if (!isCurrentGeneration(generation)) return
        val oldIds = synchronized(soundMapLock) {
            val old = setOfNotNull(noteSoundMap[noteNumber], accentSoundMap[noteNumber])
            noteSoundMap[noteNumber] = regularId
            accentSoundMap[noteNumber] = accentId
            old
        }
        oldIds.forEach { soundId ->
            if (soundId != regularId && soundId != accentId) {
                loadedSoundIds.remove(soundId)
                soundPool?.unload(soundId)
            }
        }
    }

    private fun loadWavFromBytes(wavBytes: ByteArray, tempFileName: String): Int {
        val currentCtx = context ?: return 0
        val pool = soundPool ?: return 0
        val cacheFile = File(currentCtx.cacheDir, tempFileName)
        FileOutputStream(cacheFile).use { fos ->
            fos.write(wavBytes)
            fos.flush()
        }
        return pool.load(cacheFile.absolutePath, 1)
    }

    /**
     * Plays a Handpan note by numeric number (0 for Ding, 1..8 for tonefields, 9 for Slap).
     */
    open fun playNote(noteNumber: Int, accent: Boolean = false, velocity: Float = 0.85f) {
        if (isMuted || (noteNumber !in 0..8 && noteNumber != NotePitchConfig.NOTE_SLAP)) return
        if (audioFocusManager?.request() == false) return

        val soundId = synchronized(soundMapLock) {
            if (accent) accentSoundMap[noteNumber] ?: noteSoundMap[noteNumber]
            else noteSoundMap[noteNumber]
        } ?: return

        if (!loadedSoundIds.contains(soundId)) return

        val vol = (masterVolume * velocity.coerceIn(0.1f, 1.0f)).coerceIn(0.0f, 1.0f)
        try {
            soundPool?.play(soundId, vol, vol, 1, 0, 1.0f)
        } catch (_: Exception) {}
    }

    /**
     * Plays a metronome click.
     */
    open fun playMetronomeClick(isAccent: Boolean) {
        if (isMuted) return
        if (audioFocusManager?.request() == false) return
        val soundId = if (isAccent) clickAccentId else clickRegularId
        if (soundId == 0 || !loadedSoundIds.contains(soundId)) return

        val vol = (metronomeVolume * masterVolume).coerceIn(0.0f, 1.0f)
        try {
            soundPool?.play(soundId, vol, vol, 2, 0, 1.0f)
        } catch (_: Exception) {}
    }

    open fun setMasterVolume(volume: Float) {
        this.masterVolume = volume.coerceIn(0.0f, 1.0f)
    }

    open fun setMetronomeVolume(volume: Float) {
        this.metronomeVolume = volume.coerceIn(0.0f, 1.0f)
    }

    open fun setMuted(muted: Boolean) {
        this.isMuted = muted
    }

    open fun release() {
        if (released) return
        released = true
        sampleGeneration.incrementAndGet()
        sampleLoadJob?.cancel()
        engineScope.cancel()
        synchronized(soundMapLock) {
            (noteSoundMap.values + accentSoundMap.values + listOf(clickAccentId, clickRegularId))
                .distinct()
                .forEach { soundPool?.unload(it) }
            noteSoundMap.clear()
            accentSoundMap.clear()
            clickAccentId = 0
            clickRegularId = 0
        }
        loadedSoundIds.clear()
        audioFocusManager?.abandon()
        try {
            soundPool?.release()
        } catch (_: Exception) {}
    }
}
