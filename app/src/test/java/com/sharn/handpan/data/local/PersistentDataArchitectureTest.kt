package com.sharn.handpan.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sharn.handpan.audio.RecordedStrikeEvent
import com.sharn.handpan.audio.RecordedTrack
import com.sharn.handpan.model.AssessmentEventType
import com.sharn.handpan.model.AssessmentTimelineEvent
import com.sharn.handpan.model.TimingResult
import com.sharn.handpan.model.TimingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistentDataArchitectureTest {

    private lateinit var db: AppDatabase
    private lateinit var lessonDao: LessonProgressDao
    private lateinit var recordingDao: RecordingTrackDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        lessonDao = db.lessonProgressDao()
        recordingDao = db.recordingTrackDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testLessonProgressDao_insertAndQuery() = runBlocking {
        val lesson = LessonProgressEntity(
            lessonId = "lesson_1_ding",
            isCompleted = true,
            stars = 3,
            bestScore = 95,
            attempts = 2,
            lastPracticedAt = 1700000000000L
        )

        lessonDao.saveLessonProgress(lesson)

        val retrieved = lessonDao.getProgressForLesson("lesson_1_ding")
        assertNotNull(retrieved)
        assertEquals(3, retrieved?.stars)
        assertEquals(95, retrieved?.bestScore)
        assertTrue(retrieved?.isCompleted == true)
        assertEquals(2, retrieved?.attempts)

        val allList = lessonDao.getAllLessonProgress().first()
        assertEquals(1, allList.size)
        assertEquals("lesson_1_ding", allList[0].lessonId)
    }

    @Test
    fun testRecordingTrackDao_insertAndSerialization() = runBlocking {
        val events = listOf(
            RecordedStrikeEvent(noteNumber = 0, timestampMs = 100L, velocity = 0.9f, isAccent = true),
            RecordedStrikeEvent(noteNumber = 1, timestampMs = 350L, velocity = 0.8f, isAccent = false),
            RecordedStrikeEvent(noteNumber = 9, timestampMs = 600L, velocity = 1.0f, isAccent = true)
        )

        val track = RecordedTrack(
            id = "track_test_123",
            title = "بداهه‌نوازی آزمایشی D Kurd",
            date = "2026/08/21 12:00",
            scaleId = "D Kurd",
            durationMs = 1200L,
            bpm = 88,
            timeSignature = "6/8",
            events = events
        )

        val entity = track.toEntity()
        recordingDao.insertRecordingTrack(entity)

        val retrievedEntity = recordingDao.getRecordingTrackById("track_test_123")
        assertNotNull(retrievedEntity)
        val domainTrack = retrievedEntity!!.toDomain()

        assertEquals("track_test_123", domainTrack.id)
        assertEquals("بداهه‌نوازی آزمایشی D Kurd", domainTrack.title)
        assertEquals(3, domainTrack.events.size)
        assertEquals(0, domainTrack.events[0].noteNumber)
        assertTrue(domainTrack.events[0].isAccent)
        assertEquals(9, domainTrack.events[2].noteNumber)
        assertEquals(88, domainTrack.bpm)
        assertEquals("6/8", domainTrack.timeSignature)
    }

    @Test
    fun testRecordingTrackDao_persistsCanonicalTimelineChordMetadata() = runBlocking {
        val timelineEvent = AssessmentTimelineEvent(
            eventId = "assessment-event-1",
            sessionId = "session-1",
            loopId = "loop-2",
            sequenceIndex = 4,
            expectedNote = 0,
            detectedNote = 0,
            eventType = AssessmentEventType.CORRECT,
            expectedTimestampNanos = 2_000_000_000L,
            detectedTimestampNanos = 2_010_000_000L,
            deviationNanos = 10_000_000L,
            timingResult = TimingResult(TimingStatus.GOOD, 10_000_000L),
            confidence = 0.94f,
            targetId = "target-4",
            source = "microphone",
            durationNanos = 30_000_000L,
            isConsumed = true,
            assessmentSessionId = "session-1",
            patternId = "pattern-1",
            obligationId = "target-4-0",
            expectedNotes = setOf(0, 1)
        )
        val track = RecordedTrack(
            id = "track-timeline",
            title = "timeline",
            date = "2026/08/21",
            scaleId = "D Kurd",
            durationMs = 500L,
            events = emptyList(),
            timelineEvents = listOf(timelineEvent)
        )

        recordingDao.insertRecordingTrack(track.toEntity())

        val persisted = recordingDao.getRecordingTrackById(track.id)!!.toDomain()
        assertEquals(setOf(0, 1), persisted.timelineEvents.single().expectedNotes)
        assertEquals("target-4-0", persisted.timelineEvents.single().obligationId)
        assertEquals(TimingStatus.GOOD, persisted.timelineEvents.single().timingResult?.status)
    }
}
