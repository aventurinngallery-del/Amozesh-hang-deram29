package com.sharn.handpan.data.repository

import com.sharn.handpan.data.builtin.BuiltinExercises
import com.sharn.handpan.data.local.PatternDao
import com.sharn.handpan.data.local.PatternEntity
import com.sharn.handpan.data.local.PracticeProgressDao
import com.sharn.handpan.data.local.PracticeProgressEntity
import com.sharn.handpan.data.local.toEntity
import com.sharn.handpan.data.local.toDomain
import com.sharn.handpan.data.local.toDomainOrNull
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.PatternCategory
import com.sharn.handpan.model.PracticeProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import com.sharn.handpan.data.local.AppDatabase
import com.sharn.handpan.data.local.AssessmentEntity
import com.sharn.handpan.data.local.EvidenceEntity
import com.sharn.handpan.data.local.ProcessedAssessmentEntity
import com.sharn.handpan.model.FinalizedAssessment
import com.sharn.handpan.data.local.MasteredSkillEntity
import com.sharn.handpan.model.LearningSkill
import com.sharn.handpan.model.MasteredSkillState
import com.sharn.handpan.model.MasteredSkillUpdater
import com.sharn.handpan.model.PersonalizationEngine
import com.sharn.handpan.model.LearningRecommendation
import com.sharn.handpan.model.ImportedScoreRecord
import com.sharn.handpan.model.ScoreIngestionStore
import com.sharn.handpan.data.local.ImportedScoreEntity
import com.sharn.handpan.data.local.AssessmentSessionEntity
import com.sharn.handpan.model.AssessmentTimeline
import com.sharn.handpan.model.AssessmentTimelineCodec
import com.sharn.handpan.model.PracticeInputMode
import com.sharn.handpan.model.PracticeSessionContext
import com.sharn.handpan.model.PracticeSessionLifecycle

data class RecoveredAssessment(
    val session: AssessmentSessionEntity,
    val timeline: AssessmentTimeline,
    val context: PracticeSessionContext,
    val inputMode: PracticeInputMode
)

class HandpanRepository(
    private val patternDao: PatternDao,
    private val practiceProgressDao: PracticeProgressDao,
    private val lessonProgressDao: com.sharn.handpan.data.local.LessonProgressDao,
    private val recordingTrackDao: com.sharn.handpan.data.local.RecordingTrackDao,
    private val database: AppDatabase? = null
) : ScoreIngestionStore {
    companion object {
        const val ASSESSMENT_SCHEMA_VERSION = 1
        const val ASSESSMENT_EVENT_SCHEMA_VERSION = 2
        const val EVALUATION_ALGORITHM_VERSION = 1
        const val AUDIO_CONTINUITY_UNKNOWN = "UNKNOWN"
    }
    /**
     * Flow of all available patterns: Built-in + User custom patterns.
     */
    val allPatterns: Flow<List<HandpanPattern>> = patternDao.getCustomPatterns().map { customEntities ->
        val customPatterns = customEntities.map { it.toDomain() }
        BuiltinExercises.ALL_BUILTIN_PATTERNS + customPatterns
    }

    /**
     * Get custom patterns created by user.
     */
    val customPatterns: Flow<List<HandpanPattern>> = patternDao.getCustomPatterns().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * Get all practice progress records.
     */
    val allProgress: Flow<Map<String, PracticeProgress>> = practiceProgressDao.getAllProgress().map { list ->
        list.associate { it.patternId to it.toDomain() }
    }

    /**
     * Flow of all lesson progress mapped by lessonId.
     */
    val allLessonProgress: Flow<Map<String, com.sharn.handpan.data.local.LessonProgressEntity>> = 
        lessonProgressDao.getAllLessonProgress().map { list ->
            list.associateBy { it.lessonId }
        }

    /**
     * Flow of all recorded tracks sorted chronologically.
     */
    val allRecordedTracks: Flow<List<com.sharn.handpan.audio.RecordedTrack>> = 
        recordingTrackDao.getAllRecordingTracks().map { list ->
            list.mapNotNull { it.toDomainOrNull() }
        }

    val allMasteredSkills: Flow<List<MasteredSkillState>> =
        database?.masteredSkillDao()?.observeAll()?.map { states -> states.map { it.toDomain() } }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getNextRecommendation(recentPatternIds: Set<String> = emptySet()): LearningRecommendation {
        val states = database?.masteredSkillDao()?.getAll()?.map { it.toDomain() } ?: emptyList()
        return PersonalizationEngine.recommend(states, recentPatternIds)
    }

    suspend fun getPatternById(id: String): HandpanPattern? {
        val builtin = BuiltinExercises.ALL_BUILTIN_PATTERNS.find { it.id == id }
        if (builtin != null) return builtin

        val custom = patternDao.getPatternById(id)
        return custom?.toDomain()
    }

    suspend fun saveCustomPattern(pattern: HandpanPattern) {
        val customPattern = pattern.copy(isCustom = true, category = PatternCategory.CUSTOM)
        patternDao.insertPattern(PatternEntity.fromDomain(customPattern))
    }

    suspend fun deleteCustomPattern(id: String) {
        patternDao.deletePatternById(id)
    }

    override suspend fun saveImportedScore(record: ImportedScoreRecord) {
        checkNotNull(database) { "Imported score persistence requires a database" }
            .importedScoreDao().insert(ImportedScoreEntity.fromRecord(record))
    }

    override suspend fun saveImportedExercise(record: ImportedScoreRecord, pattern: HandpanPattern) {
        val db = checkNotNull(database) { "Imported exercise persistence requires a database" }
        db.withTransaction {
            patternDao.insertPattern(PatternEntity.fromDomain(pattern.copy(isCustom = true, category = PatternCategory.CUSTOM)))
            db.importedScoreDao().insert(ImportedScoreEntity.fromRecord(record))
        }
    }

    suspend fun getImportedScore(sourceId: String): ImportedScoreRecord? =
        database?.importedScoreDao()?.getBySourceId(sourceId)?.toRecord()

    suspend fun recordPracticeSession(patternId: String, currentBpm: Int, elapsedSeconds: Int) {
        val existing = practiceProgressDao.getProgressForPattern(patternId)?.toDomain()
        val updated = if (existing != null) {
            existing.copy(
                practiceCount = existing.practiceCount + 1,
                lastPracticedTimestamp = System.currentTimeMillis(),
                highestBpmAchieved = maxOf(existing.highestBpmAchieved, currentBpm),
                lastUsedBpm = currentBpm,
                totalTimeSeconds = existing.totalTimeSeconds + elapsedSeconds,
                completedRounds = existing.completedRounds + 1
            )
        } else {
            PracticeProgress(
                patternId = patternId,
                practiceCount = 1,
                lastPracticedTimestamp = System.currentTimeMillis(),
                highestBpmAchieved = currentBpm,
                lastUsedBpm = currentBpm,
                totalTimeSeconds = elapsedSeconds,
                completedRounds = 1
            )
        }
        practiceProgressDao.saveProgress(PracticeProgressEntity.fromDomain(updated))
    }

    suspend fun getLessonProgress(lessonId: String): com.sharn.handpan.data.local.LessonProgressEntity? {
        return lessonProgressDao.getProgressForLesson(lessonId)
    }

    fun observeLessonProgress(lessonId: String): Flow<com.sharn.handpan.data.local.LessonProgressEntity?> {
        return lessonProgressDao.observeProgressForLesson(lessonId)
    }

    suspend fun saveLessonProgress(lessonId: String, score: Int, stars: Int, isCompleted: Boolean = true) {
        val existing = lessonProgressDao.getProgressForLesson(lessonId)
        val updated = if (existing != null) {
            existing.copy(
                isCompleted = isCompleted || existing.isCompleted,
                stars = maxOf(existing.stars, stars),
                bestScore = maxOf(existing.bestScore, score),
                attempts = existing.attempts + 1,
                lastPracticedAt = System.currentTimeMillis()
            )
        } else {
            com.sharn.handpan.data.local.LessonProgressEntity(
                lessonId = lessonId,
                isCompleted = isCompleted,
                stars = stars,
                bestScore = score,
                attempts = 1,
                lastPracticedAt = System.currentTimeMillis()
            )
        }
        lessonProgressDao.saveLessonProgress(updated)
    }

    suspend fun deleteLessonProgress(lessonId: String) {
        lessonProgressDao.deleteLessonProgress(lessonId)
    }

    suspend fun saveRecordingTrack(track: com.sharn.handpan.audio.RecordedTrack) {
        recordingTrackDao.insertRecordingTrack(track.toEntity())
    }

    suspend fun deleteRecordingTrack(id: String) {
        recordingTrackDao.deleteRecordingTrackById(id)
    }

    suspend fun getRecordingTrackById(id: String): com.sharn.handpan.audio.RecordedTrack? {
        return recordingTrackDao.getRecordingTrackById(id)?.toDomain()
    }

    suspend fun persistFinalizedAssessment(
        assessment: FinalizedAssessment,
        evidence: EvidenceEntity,
        finalTimelinePayload: String? = null
    ): Boolean {
        require(assessment.sessionId == evidence.sessionId)
        require(assessment.quality.validity == com.sharn.handpan.model.AssessmentSessionValidity.VALID)
        val db = database ?: return false
        return db.withTransaction {
            val active = db.assessmentSessionDao().getBySessionId(assessment.sessionId)
            if (active != null) {
                validateSessionMetadata(active)
                check(active.lifecycle in setOf(
                    PracticeSessionLifecycle.ACTIVE.name,
                    PracticeSessionLifecycle.PAUSED.name,
                    PracticeSessionLifecycle.FINALIZING.name,
                    PracticeSessionLifecycle.FINALIZED.name
                )) { "Assessment session cannot be finalized from ${active.lifecycle}" }
            }
            finalTimelinePayload?.let { payload ->
                val decoded = AssessmentTimelineCodec.decode(payload)
                require(decoded is com.sharn.handpan.model.AssessmentTimelineDecodeResult.Success) {
                    "Cannot finalize assessment with invalid timeline payload"
                }
                active?.let { current ->
                    val currentTimeline = when (val currentDecoded = AssessmentTimelineCodec.decode(current.timelinePayload)) {
                        is com.sharn.handpan.model.AssessmentTimelineDecodeResult.Success -> currentDecoded.timeline
                        is com.sharn.handpan.model.AssessmentTimelineDecodeResult.Failure ->
                            error("Active assessment timeline is corrupt")
                    }
                    require(
                        AssessmentTimelineCodec.encode(
                            (decoded as com.sharn.handpan.model.AssessmentTimelineDecodeResult.Success).timeline
                        ) == AssessmentTimelineCodec.encode(currentTimeline)
                    ) {
                        "Final assessment timeline does not match active session"
                    }
                }
            }
            val inserted = db.assessmentDao().insertIgnore(AssessmentEntity.fromDomain(assessment))
            if (inserted == -1L) {
                val existingEvidence = db.evidenceDao().getBySessionId(assessment.sessionId)
                check(existingEvidence == evidence) { "Finalized assessment evidence is inconsistent" }
                active?.let {
                    db.assessmentSessionDao().markFinalized(
                        sessionId = assessment.sessionId,
                        finalizedAtEpochMs = assessment.completedAtEpochMs,
                        audioContinuityState = AUDIO_CONTINUITY_UNKNOWN
                    )
                }
                false
            } else {
                db.evidenceDao().insertIgnore(evidence)
                db.processedAssessmentDao().insertIgnore(ProcessedAssessmentEntity(assessment.sessionId))
                val masteredSkillDao = db.masteredSkillDao()
                val practicedAt = assessment.completedAtEpochMs
                MasteredSkillUpdater.evidenceFrom(assessment.metrics).forEach { (skill, evidence) ->
                    val existing = masteredSkillDao.get(skill.name)?.toDomain()
                        ?: MasteredSkillState(skill = skill)
                    masteredSkillDao.save(
                        MasteredSkillEntity.fromDomain(
                            MasteredSkillUpdater.update(existing, evidence, practicedEpochMs = practicedAt)
                        )
                    )
                }
                recordPracticeSession(
                    patternId = assessment.patternId,
                    currentBpm = assessment.bpm,
                    elapsedSeconds = (assessment.quality.activeDurationMs / 1_000L).toInt()
                )
                active?.let {
                    db.assessmentSessionDao().markFinalized(
                        sessionId = assessment.sessionId,
                        finalizedAtEpochMs = assessment.completedAtEpochMs,
                        audioContinuityState = AUDIO_CONTINUITY_UNKNOWN
                    )
                }
                true
            }
        }
    }

    suspend fun startActiveAssessment(
        session: PracticeSessionContext,
        patternId: String,
        bpm: Int,
        inputMode: PracticeInputMode,
        timeline: AssessmentTimeline,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        val db = checkNotNull(database) { "Active assessment persistence requires a database" }
        timeline.bindToSession(session.sessionId)
                val payload = AssessmentTimelineCodec.encode(timeline, timelineRevision = 0L)
        db.assessmentSessionDao().insert(
            AssessmentSessionEntity(
                sessionId = session.sessionId,
                patternId = patternId,
                lifecycle = session.lifecycle.name,
                startedAtEpochMs = nowEpochMs,
                lastUpdatedAtEpochMs = nowEpochMs,
                elapsedDurationMs = 0L,
                activeDurationMs = 0L,
                accumulatedPausedDurationMs = 0L,
                restartCount = session.restartCount,
                bpm = bpm,
                inputMode = inputMode.name,
                assessmentSchemaVersion = ASSESSMENT_SCHEMA_VERSION,
                eventSchemaVersion = ASSESSMENT_EVENT_SCHEMA_VERSION,
                evaluationAlgorithmVersion = EVALUATION_ALGORITHM_VERSION,
                timelinePayload = payload,
                timelineRevision = 0L,
                timelineChecksum = AssessmentTimelineCodec.checksum(payload),
                eventCount = 0,
                lastEventOrdinal = -1L,
                finalizationState = "NOT_FINALIZED",
                audioContinuityState = AUDIO_CONTINUITY_UNKNOWN
            )
        )
    }

    suspend fun persistActiveTimeline(
        sessionId: String,
        timeline: AssessmentTimeline,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        val db = checkNotNull(database) { "Active assessment persistence requires a database" }
        val events = timeline.snapshot()
        val current = db.assessmentSessionDao().getBySessionId(sessionId)
            ?: error("Active assessment session does not exist: $sessionId")
        validateSessionMetadata(current)
        val payload = AssessmentTimelineCodec.encode(timeline, timelineRevision = current.timelineRevision + 1L)
        check(current.lifecycle == PracticeSessionLifecycle.ACTIVE.name || current.lifecycle == PracticeSessionLifecycle.PAUSED.name) {
            "Cannot append to assessment session in ${current.lifecycle} state"
        }
        check(db.assessmentSessionDao().updateTimeline(
            sessionId = sessionId,
            timelinePayload = payload,
            timelineRevision = current.timelineRevision + 1L,
            timelineChecksum = AssessmentTimelineCodec.checksum(payload),
            eventCount = events.size,
            lastEventOrdinal = events.maxOfOrNull { it.eventOrdinal } ?: -1L,
            elapsedDurationMs = (nowEpochMs - current.startedAtEpochMs).coerceAtLeast(current.elapsedDurationMs),
            activeDurationMs = (nowEpochMs - current.startedAtEpochMs - current.accumulatedPausedDurationMs).coerceAtLeast(current.activeDurationMs),
            lastUpdatedAtEpochMs = nowEpochMs
        ) == 1)
    }

    suspend fun updateActiveAssessmentLifecycle(
        sessionId: String,
        lifecycle: PracticeSessionLifecycle,
        pauseStartedAtEpochMs: Long?,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        val db = checkNotNull(database) { "Active assessment persistence requires a database" }
        val current = db.assessmentSessionDao().getBySessionId(sessionId)
            ?: error("Active assessment session does not exist: $sessionId")
        require(isValidTransition(current.lifecycle, lifecycle.name)) {
            "Invalid assessment lifecycle transition ${current.lifecycle} -> ${lifecycle.name}"
        }
        val elapsed = (nowEpochMs - current.startedAtEpochMs).coerceAtLeast(current.elapsedDurationMs)
        val accumulatedPaused = if (lifecycle == PracticeSessionLifecycle.ACTIVE &&
            current.lifecycle == PracticeSessionLifecycle.PAUSED.name
        ) {
            current.accumulatedPausedDurationMs +
                (nowEpochMs - (current.pauseStartedAtEpochMs ?: nowEpochMs)).coerceAtLeast(0L)
        } else if (lifecycle == PracticeSessionLifecycle.PAUSED) {
            (elapsed - current.activeDurationMs).coerceAtLeast(current.accumulatedPausedDurationMs)
        } else {
            current.accumulatedPausedDurationMs
        }
        val activeDuration = (elapsed - accumulatedPaused).coerceAtLeast(current.activeDurationMs)
        check(db.assessmentSessionDao().updateLifecycle(
            sessionId = sessionId,
            expectedLifecycle = current.lifecycle,
            lifecycle = lifecycle.name,
            pauseStartedAtEpochMs = pauseStartedAtEpochMs,
            accumulatedPausedDurationMs = accumulatedPaused,
            elapsedDurationMs = elapsed,
            activeDurationMs = activeDuration,
            lastUpdatedAtEpochMs = nowEpochMs,
            audioContinuityState = AUDIO_CONTINUITY_UNKNOWN
        ) == 1)
    }

    suspend fun beginFinalization(sessionId: String, nowEpochMs: Long = System.currentTimeMillis()) {
        val current = database?.assessmentSessionDao()?.getBySessionId(sessionId)
            ?: error("Active assessment session does not exist: $sessionId")
        if (current.lifecycle == PracticeSessionLifecycle.FINALIZING.name ||
            current.lifecycle == PracticeSessionLifecycle.FINALIZED.name
        ) return
        updateActiveAssessmentLifecycle(
            sessionId = sessionId,
            lifecycle = PracticeSessionLifecycle.FINALIZING,
            pauseStartedAtEpochMs = null,
            nowEpochMs = nowEpochMs
        )
    }

    suspend fun invalidateAssessment(sessionId: String, nowEpochMs: Long = System.currentTimeMillis()) {
        val db = checkNotNull(database) { "Active assessment persistence requires a database" }
        check(db.assessmentSessionDao().markInvalidated(sessionId, nowEpochMs) == 1)
    }

    suspend fun loadRecoverableAssessments(): List<AssessmentSessionEntity> =
        database?.assessmentSessionDao()?.getRecoverable() ?: emptyList()

    suspend fun recoverAssessment(sessionId: String, nowTimestampNanos: Long): RecoveredAssessment? {
        val session = database?.assessmentSessionDao()?.getBySessionId(sessionId) ?: return null
        if (session.lifecycle == PracticeSessionLifecycle.FINALIZED.name ||
            session.lifecycle == PracticeSessionLifecycle.INVALIDATED.name
        ) return null
        require(session.assessmentSchemaVersion == ASSESSMENT_SCHEMA_VERSION) {
            "Unsupported assessment schema version: ${session.assessmentSchemaVersion}"
        }
        require(session.eventSchemaVersion == ASSESSMENT_EVENT_SCHEMA_VERSION) {
            "Unsupported assessment event schema version: ${session.eventSchemaVersion}"
        }
        require(session.evaluationAlgorithmVersion == EVALUATION_ALGORITHM_VERSION) {
            "Unsupported assessment evaluation algorithm version: ${session.evaluationAlgorithmVersion}"
        }
        validateSessionMetadata(session)
        val timeline = loadAssessmentTimeline(sessionId) ?: return null
        require(timeline.sessionId() == sessionId) {
            "Recovered timeline session identity mismatch"
        }
        val lifecycle = PracticeSessionLifecycle.valueOf(session.lifecycle)
        val context = PracticeSessionContext.restore(
            sessionId = session.sessionId,
            patternId = session.patternId,
            nowTimestampNanos = nowTimestampNanos,
            elapsedDurationNanos = session.elapsedDurationMs * 1_000_000L,
            activeDurationNanos = session.activeDurationMs * 1_000_000L,
            restartCount = session.restartCount,
            lifecycle = lifecycle
        )
        return RecoveredAssessment(
            session = session,
            timeline = timeline,
            context = context,
            inputMode = PracticeInputMode.valueOf(session.inputMode)
        )
    }

    suspend fun loadAssessmentTimeline(sessionId: String): AssessmentTimeline? {
        val session = database?.assessmentSessionDao()?.getBySessionId(sessionId) ?: return null
        validateSessionMetadata(session)
        require(AssessmentTimelineCodec.checksum(session.timelinePayload) == session.timelineChecksum) {
            "Assessment timeline checksum mismatch for $sessionId"
        }
        return when (val decoded = AssessmentTimelineCodec.decode(session.timelinePayload)) {
            is com.sharn.handpan.model.AssessmentTimelineDecodeResult.Success -> decoded.timeline
            is com.sharn.handpan.model.AssessmentTimelineDecodeResult.Failure ->
                error("Assessment timeline decode failed: ${decoded.reason}")
        }
    }

    private fun validateSessionMetadata(session: AssessmentSessionEntity) {
        require(session.assessmentSchemaVersion == ASSESSMENT_SCHEMA_VERSION) {
            "Unsupported assessment schema version: ${session.assessmentSchemaVersion}"
        }
        require(session.eventSchemaVersion == ASSESSMENT_EVENT_SCHEMA_VERSION) {
            "Unsupported assessment event schema version: ${session.eventSchemaVersion}"
        }
        require(session.timelineRevision >= 0L) { "Invalid assessment timeline revision" }
        val payloadRevision = org.json.JSONObject(session.timelinePayload).optLong("timelineRevision", -1L)
        require(payloadRevision == session.timelineRevision) { "Assessment timeline revision mismatch" }
        require(AssessmentTimelineCodec.checksum(session.timelinePayload) == session.timelineChecksum) {
            "Assessment timeline checksum mismatch for ${session.sessionId}"
        }
        val decoded = AssessmentTimelineCodec.decode(session.timelinePayload)
        require(decoded is com.sharn.handpan.model.AssessmentTimelineDecodeResult.Success) {
            "Assessment timeline decode failed for ${session.sessionId}"
        }
        val events = decoded.timeline.snapshot()
        require(decoded.timeline.sessionId() == session.sessionId) {
            "Assessment session identity mismatch"
        }
        require(events.size == session.eventCount) { "Assessment event count mismatch" }
        require((events.maxOfOrNull { it.eventOrdinal } ?: -1L) == session.lastEventOrdinal) {
            "Assessment last event ordinal mismatch"
        }
    }

    private fun isValidTransition(current: String, next: String): Boolean = when (current) {
        PracticeSessionLifecycle.ACTIVE.name -> next in setOf(
            PracticeSessionLifecycle.PAUSED.name,
            PracticeSessionLifecycle.FINALIZING.name,
            PracticeSessionLifecycle.INVALIDATED.name
        )
        PracticeSessionLifecycle.PAUSED.name -> next in setOf(
            PracticeSessionLifecycle.ACTIVE.name,
            PracticeSessionLifecycle.FINALIZING.name,
            PracticeSessionLifecycle.INVALIDATED.name
        )
        PracticeSessionLifecycle.FINALIZING.name -> next in setOf(
            PracticeSessionLifecycle.FINALIZED.name,
            PracticeSessionLifecycle.INVALIDATED.name
        )
        else -> false
    }

    suspend fun getAssessment(sessionId: String): AssessmentEntity? =
        database?.assessmentDao()?.getBySessionId(sessionId)

    suspend fun getEvidence(sessionId: String): EvidenceEntity? =
        database?.evidenceDao()?.getBySessionId(sessionId)
}
