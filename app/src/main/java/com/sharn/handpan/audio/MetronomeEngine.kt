package com.sharn.handpan.audio

import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimeSignature
import com.sharn.handpan.util.HapticHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield

data class MetronomeState(
    val isPlaying: Boolean = false,
    val bpm: Int = 70,
    val timeSignature: TimeSignature = TimeSignature.Common44,
    val subdivision: Subdivision = Subdivision.QUARTER,
    val currentBeat: Int = 1,          // 1-indexed (e.g. 1..4)
    val currentSubBeat: Int = 1,       // 1-indexed for subdivision
    val isDownbeat: Boolean = false,
    val accentFirstBeat: Boolean = true,
    val isMuted: Boolean = false,
    val hapticEnabled: Boolean = true,
    val barIndex: Int = 1,
    val tickIndex: Long = 0L,
    val lastTickTimestampNanos: Long = 0L,
    val nextTickTimestampNanos: Long = 0L
)

data class PracticeBeatEvent(
    val beatNumber: Int,
    val barNumber: Int,
    val beatStartNanos: Long,
    val beatProgress: Float,
    val isDownbeat: Boolean,
    val bpm: Int
)

class MetronomeEngine(
    private val audioEngine: AudioEngine,
    private val hapticHelper: HapticHelper? = null,
    private val clock: PracticeClock = PracticeClock.Default
) {
    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    private var metronomeJob: Job? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tapTimes = mutableListOf<Long>()
    private val deadlineScheduler = DeadlineScheduler(clock)

    fun togglePlay() {
        if (_state.value.isPlaying) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (_state.value.isPlaying) return
        _state.update { it.copy(isPlaying = true, currentBeat = 1, currentSubBeat = 1) }

        metronomeJob = engineScope.launch {
            runMetronomeLoop()
        }
    }

    fun stop() {
        metronomeJob?.cancel()
        metronomeJob = null
        _state.update { it.copy(isPlaying = false, currentBeat = 1, currentSubBeat = 1, isDownbeat = false) }
    }

    fun release() {
        stop()
        engineScope.cancel()
    }

    fun consumePracticeBeat(event: PracticeBeatEvent) {
        val beatDuration = MusicalTiming.beatDurationNanos(event.bpm)
        _state.update {
            it.copy(
                isPlaying = true,
                bpm = event.bpm,
                currentBeat = event.beatNumber,
                currentSubBeat = 1,
                isDownbeat = event.isDownbeat,
                barIndex = event.barNumber,
                tickIndex = event.beatNumber.toLong() - 1L,
                lastTickTimestampNanos = event.beatStartNanos,
                nextTickTimestampNanos = event.beatStartNanos + beatDuration
            )
        }
        if (event.beatProgress <= PatternScheduler.BEAT_EPSILON) {
            audioEngine.playMetronomeClick(isAccent = event.isDownbeat)
        }
    }

    private suspend fun runMetronomeLoop() {
        var tickIndex = 0L
        var startTimeNanos = clock.nowNanos()
        var previousBpm = _state.value.bpm
        var previousTimeSignature = _state.value.timeSignature
        var previousSubdivision = _state.value.subdivision

        while (currentCoroutineContext().isActive) {
            val currentState = _state.value
            val beatsPerBar = currentState.timeSignature.beatsPerBar
            val divsPerBeat = currentState.subdivision.divisionsPerBeat

            val totalTicksPerBar = beatsPerBar * divsPerBeat
            val tickInBar = (tickIndex % totalTicksPerBar).toInt()

            val currentBeatIndex = (tickInBar / divsPerBeat) + 1
            val currentSubIndex = (tickInBar % divsPerBeat) + 1
            val isDownbeat = currentState.timeSignature.isGroupedAccent(currentBeatIndex)
            val isMainBeat = (currentSubIndex == 1)
            if (currentState.bpm != previousBpm ||
                currentState.timeSignature != previousTimeSignature ||
                currentState.subdivision != previousSubdivision
            ) {
                startTimeNanos = clock.nowNanos()
                tickIndex = 0L
                previousBpm = currentState.bpm
                previousTimeSignature = currentState.timeSignature
                previousSubdivision = currentState.subdivision
            }
            val intervalNanos = calculateTickIntervalNanos(
                currentState.bpm,
                currentState.timeSignature,
                divsPerBeat
            )
            val tickTimestampNanos = startTimeNanos + (tickIndex * intervalNanos)
            val nextTargetNanos = tickTimestampNanos + intervalNanos

            // Play audio
            val shouldAccent = isDownbeat && currentState.accentFirstBeat
            if (isMainBeat) {
                audioEngine.playMetronomeClick(isAccent = shouldAccent)
            } else {
                // Subtle sub-click
                audioEngine.playMetronomeClick(isAccent = false)
            }

            // Haptic
            if (currentState.hapticEnabled && isMainBeat) {
                hapticHelper?.performClick(isAccent = shouldAccent)
            }

            // Update UI State
            _state.update {
                it.copy(
                    currentBeat = currentBeatIndex,
                    currentSubBeat = currentSubIndex,
                    isDownbeat = isDownbeat,
                    barIndex = (tickIndex / totalTicksPerBar).toInt() + 1,
                    tickIndex = tickIndex,
                    lastTickTimestampNanos = tickTimestampNanos,
                    nextTickTimestampNanos = nextTargetNanos
                )
            }

            tickIndex++

            val currentNanos = clock.nowNanos()
            val nanosToWait = nextTargetNanos - currentNanos

            if (nanosToWait > 0) deadlineScheduler.await(nextTargetNanos)
        }
    }

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(40, 240)
        _state.update { it.copy(bpm = clamped) }
    }

    fun setTimeSignature(timeSignature: TimeSignature) {
        _state.update { it.copy(timeSignature = timeSignature) }
    }

    fun setSubdivision(subdivision: Subdivision) {
        _state.update { it.copy(subdivision = subdivision) }
    }

    fun setAccentFirstBeat(accent: Boolean) {
        _state.update { it.copy(accentFirstBeat = accent) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        _state.update { it.copy(hapticEnabled = enabled) }
    }

    fun nowNanos(): Long = clock.nowNanos()

    fun tapTempo() {
        val now = clock.nowMillis()
        tapTimes.add(now)
        // Keep last 4 taps within 3 seconds
        tapTimes.removeAll { now - it > 3000 }

        if (tapTimes.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimes.size) {
                intervals.add(tapTimes[i] - tapTimes[i - 1])
            }
            val avgIntervalMs = intervals.average()
            if (avgIntervalMs > 0) {
                val calculatedBpm = (60000.0 / avgIntervalMs).toInt().coerceIn(40, 240)
                setBpm(calculatedBpm)
            }
        }
    }

    companion object {
        fun calculateTickIntervalNanos(bpm: Int, subdivisionDivisions: Int): Long {
            return MusicalTiming.beatDurationNanos(bpm) / subdivisionDivisions.coerceAtLeast(1)
        }

        fun calculateTickIntervalNanos(
            bpm: Int,
            timeSignature: TimeSignature,
            subdivisionDivisions: Int
        ): Long {
            return MusicalTiming.signatureBeatDurationNanos(bpm, timeSignature) /
                subdivisionDivisions.coerceAtLeast(1)
        }

        fun calculateBeatIntervalMs(bpm: Int): Long {
            return MusicalTiming.beatDurationNanos(bpm) / 1_000_000L
        }
    }
}
