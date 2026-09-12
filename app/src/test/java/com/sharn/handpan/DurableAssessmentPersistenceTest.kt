package com.sharn.handpan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sharn.handpan.data.local.AppDatabase
import com.sharn.handpan.data.local.EvidenceEntity
import com.sharn.handpan.data.repository.HandpanRepository
import com.sharn.handpan.model.AssessmentSessionValidity
import com.sharn.handpan.model.AssessmentSessionQuality
import com.sharn.handpan.model.CanonicalAssessmentMetrics
import com.sharn.handpan.model.FinalizedAssessment
import com.sharn.handpan.model.PracticeScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DurableAssessmentPersistenceTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: HandpanRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HandpanRepository(
            database.patternDao(),
            database.practiceProgressDao(),
            database.lessonProgressDao(),
            database.recordingTrackDao(),
            database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun finalizedAssessmentEvidenceAndProgressAreSessionIdempotent() = runBlocking {
        val first = assessment("session-a")
        val firstEvidence = evidence(first)

        assertTrue(repository.persistFinalizedAssessment(first, firstEvidence))
        assertFalse(repository.persistFinalizedAssessment(first, firstEvidence))

        val persistedAssessment = repository.getAssessment(first.sessionId)
        val persistedEvidence = repository.getEvidence(first.sessionId)
        assertNotNull(persistedAssessment)
        assertNotNull(persistedEvidence)
        assertEquals(first.sessionId, persistedAssessment?.sessionId)
        assertEquals(first.patternId, persistedAssessment?.patternId)
        assertEquals(first.metrics.timingScore, persistedAssessment?.timingScore)
        assertEquals(first.metrics.pitchScore, persistedAssessment?.pitchScore)
        assertEquals(first.metrics.consistencyScore, persistedEvidence?.consistencyScore)

        val second = assessment("session-b")
        assertTrue(repository.persistFinalizedAssessment(second, evidence(second)))
        val progress = repository.allProgress.firstValue()[first.patternId]
        assertNotNull(progress)
        assertEquals(2, progress?.practiceCount)
        assertEquals(2, progress?.completedRounds)
    }

    @Test
    fun concurrentPersistenceForOneSessionUpdatesProgressOnce() = runBlocking {
        val assessment = assessment("concurrent-session")
        val evidence = evidence(assessment)

        val results = coroutineScope {
            listOf(
                async(Dispatchers.Default) {
                    repository.persistFinalizedAssessment(assessment, evidence)
                },
                async(Dispatchers.Default) {
                    repository.persistFinalizedAssessment(assessment, evidence)
                }
            ).map { it.await() }
        }

        assertEquals(listOf(true, false).toSet(), results.toSet())
        assertEquals(1, database.assessmentDao().observeAll().first().count { it.sessionId == assessment.sessionId })
        assertNotNull(repository.getEvidence(assessment.sessionId))
        assertEquals(assessment.sessionId, database.processedAssessmentDao().find(assessment.sessionId))
        assertEquals(1, repository.allProgress.first()[assessment.patternId]?.practiceCount)
        assertEquals(1, repository.allProgress.first()[assessment.patternId]?.completedRounds)
    }

    private fun assessment(sessionId: String) = FinalizedAssessment(
        sessionId = sessionId,
        patternId = "pattern-1",
        bpm = 80,
        completedAtEpochMs = 1_700_000_000_000L,
        quality = AssessmentSessionQuality(
            sessionId = sessionId,
            durationMs = 5_000L,
            activeDurationMs = 4_000L,
            eventCount = 2,
            validEventCount = 2,
            signalQuality = 0.9f,
            restartCount = 0,
            contextCompleteness = 1f,
            qualityScore = 90f,
            validity = AssessmentSessionValidity.VALID,
            finalized = true
        ),
        metrics = CanonicalAssessmentMetrics(
            overallPerformance = 88f,
            timingScore = 92f,
            pitchScore = 94f,
            noteAccuracy = 95f,
            completionRate = 100f,
            missRate = 0f,
            falseStrikeRate = 0f,
            consistencyScore = 91f,
            confidenceScore = 90f
        ),
        score = PracticeScore(
            correctCount = 2,
            wrongCount = 0,
            unknownCount = 0,
            missedCount = 0,
            extraCount = 0,
            noteAccuracyPercentage = 100f,
            timingAccuracyPercentage = 92f,
            overallAccuracyPercentage = 96f
        )
    )

    private fun evidence(assessment: FinalizedAssessment) = EvidenceEntity.fromDomain(
        sessionId = assessment.sessionId,
        validEvidenceCount = assessment.quality.validEventCount,
        validity = assessment.quality.validity.name,
        metrics = assessment.metrics
    )
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = first()
