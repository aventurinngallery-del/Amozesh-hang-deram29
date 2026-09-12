package com.sharn.handpan.audio

import android.content.Context
import android.util.Log
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.model.DetectedStrikeEvent
import com.sharn.handpan.model.StrikeClassification
import com.sharn.handpan.model.AssessmentTimeline
import com.sharn.handpan.model.AssessmentTimelineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data structure representing a recorded live performance session.
 */
data class RecordedTrack(
    val id: String,
    val title: String,
    val date: String,
    val scaleId: String,
    val durationMs: Long,
    val events: List<RecordedStrikeEvent>,
    val timelineEvents: List<AssessmentTimelineEvent> = emptyList(),
    val bpm: Int = 70,
    val timeSignature: String = "4/4"
)

data class RecordedStrikeEvent(
    val noteNumber: Int,
    val timestampMs: Long,
    val velocity: Float = 0.85f,
    val isAccent: Boolean = false,
    val durationMs: Long? = null,
    val hand: String? = null,
    val classification: StrikeClassification = StrikeClassification.CORRECT_NOTE,
    val confidence: Float = 1.0f
)

data class RecorderState(
    val isRecording: Boolean = false,
    val recordingStartTime: Long = 0L,
    val recordingEventsCount: Int = 0,
    val tracks: List<RecordedTrack> = emptyList(),
    val playingTrackId: String? = null,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false
)

/**
 * High-precision Performance Recorder and Looper Engine for live Handpan strikes.
 */
class PerformanceRecorder(
    private val context: Context,
    private val audioEngine: AudioEngine,
    private val repository: com.sharn.handpan.data.repository.HandpanRepository? = null,
    private val clock: PracticeClock = PracticeClock.Default,
    private val analysisSession: AudioAnalysisSession = AudioAnalysisSession(),
    private val ownsAnalysisSession: Boolean = true,
    val timeline: AssessmentTimeline = AssessmentTimeline()
) {
    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val liveEvents = mutableListOf<RecordedStrikeEvent>()
    private var recordStartMs: Long = 0L
    private var recordStartNanos: Long = 0L
    private var playbackJob: Job? = null
    private var analysisSubscription: AudioAnalysisSession.Subscription? = null
    private var timelineSubscription: AssessmentTimeline.Subscription? = null
    private val liveTimelineEvents = mutableListOf<AssessmentTimelineEvent>()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val deadlineScheduler = DeadlineScheduler(clock)

    val isCapturingAcousticInput: Boolean
        get() = _state.value.isRecording

    init {
        loadTracksFromStorage()
    }

    fun startRecording(scaleConfig: NotePitchConfig = NotePitchConfig.D_KURD_9) {
        if (_state.value.isRecording) return
        stopPlayback()
        liveEvents.clear()
        liveTimelineEvents.clear()
        recordStartMs = clock.nowMillis()
        recordStartNanos = clock.nowNanos()
        _state.update {
            it.copy(
                isRecording = true,
                recordingStartTime = recordStartMs,
                recordingEventsCount = 0
            )
        }
        timelineSubscription?.close()
        timelineSubscription = timeline.subscribe { event ->
            if (_state.value.isRecording && event.sessionId.isNotBlank()) {
                liveTimelineEvents += event
            }
        }
        analysisSubscription = analysisSession.acquire(
            scaleConfig = scaleConfig,
            onStrike = { event ->
                recordDetectedStrike(event)
            }
        )
    }

    private fun recordDetectedStrike(event: DetectedStrikeEvent) {
        if (!_state.value.isRecording) return
        val offset = ((event.monotonicTimestampNanos - recordStartNanos) / 1_000_000L).coerceAtLeast(0L)
        liveEvents.add(
            RecordedStrikeEvent(
                noteNumber = event.detectedNote ?: -1,
                timestampMs = offset,
                velocity = event.energy,
                classification = if (event.pitchValid) StrikeClassification.CORRECT_NOTE else StrikeClassification.UNKNOWN_NOTE,
                confidence = event.pitchConfidence
            )
        )
        _state.update { it.copy(recordingEventsCount = liveEvents.size) }
    }

    fun recordStrikeAtMonotonicTime(
        noteNumber: Int,
        velocity: Float,
        timestampNanos: Long
    ) {
        if (!_state.value.isRecording) return
        val offset = ((timestampNanos - recordStartNanos) / 1_000_000L).coerceAtLeast(0L)
        liveEvents.add(
            RecordedStrikeEvent(
                noteNumber = noteNumber,
                timestampMs = offset,
                velocity = velocity
            )
        )
        _state.update { it.copy(recordingEventsCount = liveEvents.size) }
    }

    fun recordStrike(
        noteNumber: Int,
        isAccent: Boolean = false,
        velocity: Float = 0.85f,
        timestampMs: Long = clock.nowMillis(),
        durationMs: Long? = null,
        hand: String? = null,
        classification: StrikeClassification = StrikeClassification.CORRECT_NOTE,
        confidence: Float = 1.0f
    ) {
        if (!_state.value.isRecording) return
        liveEvents += RecordedStrikeEvent(
            noteNumber = noteNumber,
            timestampMs = (timestampMs - recordStartMs).coerceAtLeast(0L),
            velocity = velocity,
            isAccent = isAccent,
            durationMs = durationMs,
            hand = hand,
            classification = classification,
            confidence = confidence
        )
        _state.update { it.copy(recordingEventsCount = liveEvents.size) }
    }

    fun stopRecording(
        scaleName: String = "D Kurd",
        customTitle: String? = null,
        bpm: Int = 70,
        timeSignature: String = "4/4"
    ): RecordedTrack? {
        if (!_state.value.isRecording) return null
        analysisSubscription?.close()
        analysisSubscription = null
        timelineSubscription?.close()
        timelineSubscription = null
        val duration = clock.nowMillis() - recordStartMs
        _state.update { it.copy(isRecording = false) }

        if (liveEvents.isEmpty()) return null

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val defaultTitle = "بداهه‌نوازی $scaleName (${_state.value.tracks.size + 1})"
        val track = RecordedTrack(
            id = "track_${System.currentTimeMillis()}",
            title = if (!customTitle.isNullOrBlank()) customTitle else defaultTitle,
            date = dateFormat.format(Date()),
            scaleId = scaleName,
            durationMs = duration.coerceAtLeast(500L),
            events = ArrayList(liveEvents),
            timelineEvents = liveTimelineEvents.toList(),
            bpm = bpm,
            timeSignature = timeSignature
        )

        val updatedTracks = listOf(track) + _state.value.tracks
        _state.update { it.copy(tracks = updatedTracks) }
        saveTrackToStorage(track)
        return track
    }

    fun playTrack(track: RecordedTrack) {
        stopPlayback()
        _state.update { it.copy(playingTrackId = track.id) }

        playbackJob = scope.launch {
            val speed = _state.value.playbackSpeed
            val isLoop = _state.value.isLooping

            do {
                val playbackStartNanos = clock.nowNanos()
                var lastTime = 0L
                for (evt in track.events) {
                    if (!isActive) break
                    val eventOffsetNanos = (evt.timestampMs / speed * 1_000_000L).toLong()
                    deadlineScheduler.await(playbackStartNanos + eventOffsetNanos)
                    if (evt.noteNumber >= 0) {
                        audioEngine.playNote(evt.noteNumber, evt.isAccent, evt.velocity)
                    }
                    lastTime = evt.timestampMs
                }

                val remainingTail = ((track.durationMs - lastTime).toFloat() / speed).toLong().coerceAtLeast(0L)
                if (remainingTail > 0 && isActive) {
                    deadlineScheduler.await(playbackStartNanos + (track.durationMs / speed * 1_000_000L).toLong())
                }
            } while (isLoop && isActive)

            if (isActive) {
                _state.update { it.copy(playingTrackId = null) }
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _state.update { it.copy(playingTrackId = null) }
    }

    fun release() {
        if (_state.value.isRecording) {
            analysisSubscription?.close()
            analysisSubscription = null
            timelineSubscription?.close()
            timelineSubscription = null
            _state.update { it.copy(isRecording = false) }
        }
        stopPlayback()
        if (ownsAnalysisSession) analysisSession.close()
        scope.cancel()
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed.coerceIn(0.5f, 2.0f)) }
    }

    fun toggleLoop() {
        _state.update { it.copy(isLooping = !it.isLooping) }
    }

    fun deleteTrack(trackId: String) {
        if (_state.value.playingTrackId == trackId) {
            stopPlayback()
        }
        val updated = _state.value.tracks.filter { it.id != trackId }
        _state.update { it.copy(tracks = updated) }
        if (repository != null) {
            scope.launch {
                try {
                    repository.deleteRecordingTrack(trackId)
                } catch (e: Exception) {
                    Log.e("PerformanceRecorder", "Error deleting track from DB: ${e.message}", e)
                }
            }
        }
    }

    fun exportTrackAsJSON(track: RecordedTrack): String {
        val root = JSONObject()
        root.put("id", track.id)
        root.put("title", track.title)
        root.put("date", track.date)
        root.put("scaleId", track.scaleId)
        root.put("durationMs", track.durationMs)
        root.put("bpm", track.bpm)
        root.put("timeSignature", track.timeSignature)

        val eventsArray = JSONArray()
        for (e in track.events) {
            val evObj = JSONObject()
            evObj.put("noteNumber", e.noteNumber)
            evObj.put("timestampMs", e.timestampMs)
            evObj.put("velocity", e.velocity)
            evObj.put("isAccent", e.isAccent)
            evObj.put("classification", e.classification.name)
            evObj.put("confidence", e.confidence)
            e.durationMs?.let { evObj.put("durationMs", it) }
            e.hand?.let { evObj.put("hand", it) }
            eventsArray.put(evObj)
        }
        root.put("events", eventsArray)
        return root.toString(2)
    }

    private fun saveTrackToStorage(track: RecordedTrack) {
        if (repository != null) {
            scope.launch {
                try {
                    repository.saveRecordingTrack(track)
                } catch (e: Exception) {
                    Log.e("PerformanceRecorder", "Error saving track to Room DB: ${e.message}", e)
                }
            }
        }
    }

    private fun loadTracksFromStorage() {
        if (repository != null) {
            scope.launch {
                // One-time legacy migration from SharedPreferences to Room if needed
                migrateLegacyPreferencesIfNeeded()

                try {
                    repository.allRecordedTracks.collect { tracksFromDb ->
                        _state.update { it.copy(tracks = tracksFromDb) }
                    }
                } catch (e: Exception) {
                    Log.e("PerformanceRecorder", "Error collecting tracks from Room DB: ${e.message}", e)
                }
            }
        } else {
            // Fallback for isolated unit tests without repository
            migrateLegacyPreferencesIfNeeded()
        }
    }

    private fun migrateLegacyPreferencesIfNeeded() {
        try {
            val prefs = context.getSharedPreferences("handpan_recorded_tracks", Context.MODE_PRIVATE)
            val isMigrated = prefs.getBoolean("room_migration_completed", false)
            if (isMigrated) return

            val raw = prefs.getString("saved_tracks_json", null)
            if (!raw.isNullOrBlank()) {
                val jsonArray = JSONArray(raw)
                val list = mutableListOf<RecordedTrack>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val evArray = obj.optJSONArray("events") ?: JSONArray()
                    val evList = mutableListOf<RecordedStrikeEvent>()
                    for (j in 0 until evArray.length()) {
                        val evObj = evArray.getJSONObject(j)
                        evList.add(
                            RecordedStrikeEvent(
                                noteNumber = evObj.getInt("noteNumber"),
                                timestampMs = evObj.getLong("timestampMs"),
                                velocity = evObj.optDouble("velocity", 0.85).toFloat(),
                                isAccent = evObj.optBoolean("isAccent", false)
                            )
                        )
                    }
                    list.add(
                        RecordedTrack(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            date = obj.getString("date"),
                            scaleId = obj.optString("scaleId", "D Kurd"),
                            durationMs = obj.getLong("durationMs"),
                            events = evList
                        )
                    )
                }

                if (repository != null && list.isNotEmpty()) {
                    scope.launch {
                        for (track in list) {
                            repository.saveRecordingTrack(track)
                        }
                    }
                } else if (list.isNotEmpty()) {
                    _state.update { it.copy(tracks = list) }
                }
            }
            prefs.edit()
                .putBoolean("room_migration_completed", true)
                .remove("saved_tracks_json")
                .apply()
        } catch (e: Exception) {
            Log.e("PerformanceRecorder", "Error in legacy tracks migration: ${e.message}", e)
        }
    }
}
