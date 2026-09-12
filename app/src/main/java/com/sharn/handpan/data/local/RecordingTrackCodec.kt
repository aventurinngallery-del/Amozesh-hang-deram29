package com.sharn.handpan.data.local

import com.sharn.handpan.audio.RecordedStrikeEvent
import com.sharn.handpan.audio.RecordedTrack
import com.sharn.handpan.model.AssessmentEventType
import com.sharn.handpan.model.AssessmentTimelineEvent
import com.sharn.handpan.model.StrikeClassification
import com.sharn.handpan.model.TimingResult
import com.sharn.handpan.model.TimingStatus
import org.json.JSONArray
import org.json.JSONObject

fun RecordingTrackEntity.toDomainOrNull(): RecordedTrack? = runCatching {
    toDomain()
}.getOrNull()

fun RecordingTrackEntity.toDomain(): RecordedTrack = RecordedTrack(
    id = id,
    title = title,
    date = date,
    scaleId = scaleId,
    durationMs = durationMs,
    events = parseEventsJson(eventsJson),
    timelineEvents = parseTimelineEventsJson(timelineEventsJson),
    bpm = bpm,
    timeSignature = timeSignature
)

fun RecordedTrack.toEntity(): RecordingTrackEntity = RecordingTrackEntity(
    id = id,
    title = title,
    date = date,
    scaleId = scaleId,
    durationMs = durationMs,
    eventsJson = encodeEventsJson(events),
    bpm = bpm,
    timeSignature = timeSignature,
    timelineEventsJson = encodeTimelineEventsJson(timelineEvents)
)

private fun encodeEventsJson(events: List<RecordedStrikeEvent>): String = JSONArray().apply {
    events.forEach { event ->
        put(JSONObject().apply {
            put("noteNumber", event.noteNumber)
            put("timestampMs", event.timestampMs)
            put("velocity", event.velocity.toDouble())
            put("isAccent", event.isAccent)
            put("classification", event.classification.name)
            put("confidence", event.confidence)
            event.durationMs?.let { put("durationMs", it) }
            event.hand?.let { put("hand", it) }
        })
    }
}.toString()

private fun parseEventsJson(json: String): List<RecordedStrikeEvent> {
    val array = JSONArray(json)
    return buildList {
        for (index in 0 until array.length()) {
            val obj = array.getJSONObject(index)
            add(RecordedStrikeEvent(
                noteNumber = obj.getInt("noteNumber"),
                timestampMs = obj.getLong("timestampMs"),
                velocity = obj.optDouble("velocity", 0.85).toFloat(),
                isAccent = obj.optBoolean("isAccent", false),
                durationMs = if (obj.has("durationMs")) obj.optLong("durationMs") else null,
                hand = obj.optString("hand").takeIf { it.isNotBlank() },
                classification = StrikeClassification.valueOf(
                    obj.optString("classification", StrikeClassification.CORRECT_NOTE.name)
                ),
                confidence = obj.optDouble("confidence", 1.0).toFloat()
            ))
        }
    }
}

private fun encodeTimelineEventsJson(events: List<AssessmentTimelineEvent>): String = JSONArray().apply {
    events.forEach { event ->
        put(JSONObject().apply {
            put("eventId", event.eventId)
            put("sessionId", event.sessionId)
            put("assessmentSessionId", event.assessmentSessionId)
            event.loopId?.let { put("loopId", it) }
            event.patternId?.let { put("patternId", it) }
            event.targetId?.let { put("targetId", it) }
            put("sequenceIndex", event.sequenceIndex)
            event.obligationId?.let { put("obligationId", it) }
            put("expectedNotes", JSONArray(event.expectedNotes.toList()))
            event.expectedNote?.let { put("expectedNote", it) }
            event.detectedNote?.let { put("detectedNote", it) }
            put("eventType", event.eventType.name)
            event.expectedTimestampNanos?.let { put("expectedTimestampNanos", it) }
            event.detectedTimestampNanos?.let { put("detectedTimestampNanos", it) }
            event.deviationNanos?.let { put("deviationNanos", it) }
            event.timingResult?.let {
                put("timingStatus", it.status.name)
                put("timingDeviationNanos", it.deviationNanos)
            }
            put("confidence", event.confidence)
            put("source", event.source)
            event.durationNanos?.let { put("durationNanos", it) }
            put("isConsumed", event.isConsumed)
        })
    }
}.toString()

private fun parseTimelineEventsJson(json: String): List<AssessmentTimelineEvent> {
    val array = JSONArray(json)
    return buildList {
        for (index in 0 until array.length()) {
            val obj = array.getJSONObject(index)
            val expectedNote = obj.optIntOrNull("expectedNote")
            val timingStatus = obj.optString("timingStatus").takeIf { it.isNotBlank() }
            add(AssessmentTimelineEvent(
                eventId = obj.getString("eventId"),
                sessionId = obj.getString("sessionId"),
                loopId = obj.optString("loopId").takeIf { it.isNotBlank() },
                sequenceIndex = obj.getInt("sequenceIndex"),
                expectedNote = expectedNote,
                detectedNote = obj.optIntOrNull("detectedNote"),
                eventType = AssessmentEventType.valueOf(obj.getString("eventType")),
                expectedTimestampNanos = obj.optLongOrNull("expectedTimestampNanos"),
                detectedTimestampNanos = obj.optLongOrNull("detectedTimestampNanos"),
                deviationNanos = obj.optLongOrNull("deviationNanos"),
                timingResult = timingStatus?.let {
                    TimingResult(TimingStatus.valueOf(it), obj.optLong("timingDeviationNanos"))
                },
                confidence = obj.getDouble("confidence").toFloat(),
                targetId = obj.optString("targetId").takeIf { it.isNotBlank() },
                source = obj.getString("source"),
                durationNanos = obj.optLongOrNull("durationNanos"),
                isConsumed = obj.getBoolean("isConsumed"),
                assessmentSessionId = obj.optString("assessmentSessionId", obj.getString("sessionId")),
                patternId = obj.optString("patternId").takeIf { it.isNotBlank() },
                obligationId = obj.optString("obligationId").takeIf { it.isNotBlank() },
                expectedNotes = obj.optJSONArray("expectedNotes")?.let { notes ->
                    buildSet { for (noteIndex in 0 until notes.length()) add(notes.getInt(noteIndex)) }
                } ?: expectedNote?.let { setOf(it) }.orEmpty()
            ))
        }
    }
}

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null
