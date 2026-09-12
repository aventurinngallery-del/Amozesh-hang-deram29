package com.sharn.handpan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved recorded live performance / loop track.
 */
@Entity(tableName = "recording_tracks")
data class RecordingTrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val date: String,
    val scaleId: String,
    val durationMs: Long,
    val eventsJson: String,
    val bpm: Int = 70,
    val timeSignature: String = "4/4",
    val timelineEventsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)
