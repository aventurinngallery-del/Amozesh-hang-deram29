package com.sharn.handpan

import com.sharn.handpan.model.CanonicalAssessmentMetrics
import com.sharn.handpan.model.LearningSkill
import com.sharn.handpan.data.builtin.BuiltinExercises
import com.sharn.handpan.model.LearningExerciseMapping
import com.sharn.handpan.model.MasteredSkillState
import com.sharn.handpan.model.MasteredSkillUpdater
import com.sharn.handpan.model.PersonalizationEngine
import com.sharn.handpan.model.PracticeRecommendation
import com.sharn.handpan.model.SkillEvidenceMapper
import com.sharn.handpan.model.AssessmentEventType
import com.sharn.handpan.model.AssessmentTimeline
import com.sharn.handpan.model.AssessmentTimelineEvent
import com.sharn.handpan.model.DynamicsEvidenceSample
import com.sharn.handpan.model.HandpanTechnique
import com.sharn.handpan.model.SkillEvidenceCalculator
import com.sharn.handpan.model.SkillEvidenceRawMetrics
import com.sharn.handpan.model.TechniqueEvidenceEvent
import com.sharn.handpan.model.TimingResult
import com.sharn.handpan.model.TimingStatus
import com.sharn.handpan.model.AssessmentSessionValidity
import com.sharn.handpan.model.AssessmentSessionValidator
import com.sharn.handpan.model.SessionHistoryProvider
import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimingToleranceProfile
import com.sharn.handpan.model.DetectedStrikeEvent
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.model.PracticeSession
import com.sharn.handpan.audio.PracticeClock
import com.sharn.handpan.model.PracticeSessionContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningEngineTest {
    @Test
    fun noMultipleSessionIdentityCreators() {
        val session = PracticeSessionContext.start("pattern", 1_000L)
        val evaluator = com.sharn.handpan.audio.AcousticPracticeEvaluator(clock = PracticeClock.Default)
        val pattern = com.sharn.handpan.model.HandpanPattern("pattern", "title", "description", events = emptyList())

        evaluator.startAssessment(session, pattern, NotePitchConfig.D_KURD_9)

        assertEquals(session.sessionId, evaluator.assessmentSessionIdForTesting)
    }

    @Test
    fun audioSessionMustUseContextId() {
        val session = com.sharn.handpan.audio.AudioAnalysisSession()
        val field = session::class.java.getDeclaredField("activeSessionId")
        field.isAccessible = true
        session.bindSessionId("canonical")

        assertEquals("canonical", field.get(session))
    }

    @Test
    fun evaluatorMustNotGenerateSessionId() {
        val session = PracticeSessionContext.start("pattern", 1_000L)
        val evaluator = com.sharn.handpan.audio.AcousticPracticeEvaluator(clock = PracticeClock.Default)
        val pattern = com.sharn.handpan.model.HandpanPattern("pattern", "title", "description", events = emptyList())

        evaluator.startAssessment(session, pattern, NotePitchConfig.D_KURD_9)

        assertEquals(session.sessionId, evaluator.assessmentSessionIdForTesting)
    }

    @Test
    fun deprecatedAssessmentApisHaveNoRuntimeCallers() {
        val method = com.sharn.handpan.audio.AcousticPracticeEvaluator::class.java.getDeclaredMethod(
            "startAssessment",
            PracticeSessionContext::class.java,
            com.sharn.handpan.model.HandpanPattern::class.java,
            NotePitchConfig::class.java,
            Int::class.javaPrimitiveType
        )

        assertFalse(method.isAnnotationPresent(Deprecated::class.java))
    }

    @Test
    fun previewMustNotEnterAssessmentPipeline() {
        val session = PracticeSessionContext.start("preview-pattern", 1_000L)
        val target = com.sharn.handpan.audio.PatternScheduler.buildSchedule(
            events = listOf(com.sharn.handpan.model.NoteEvent(0, 0.0)),
            beatsPerBar = 4,
            totalBars = 1,
            assessmentSessionId = session.sessionId,
            patternId = session.patternId
        ).first { it.target != null }.target!!

        assertEquals(session.sessionId, target.identity.sessionId)
    }

    @Test
    fun canonicalSessionIdMustRemainStableAcrossAssessmentPipeline() {
        val evaluator = com.sharn.handpan.audio.AcousticPracticeEvaluator(clock = PracticeClock.Default)
        val pattern = com.sharn.handpan.model.HandpanPattern(
            id = "canonical-pattern",
            title = "canonical",
            description = "canonical",
            events = listOf(com.sharn.handpan.model.NoteEvent(0, 0.0))
        )

        evaluator.startAssessment(pattern, NotePitchConfig.D_KURD_9)
        val evaluatorSessionId = evaluator.assessmentSessionIdForTesting
        val target = com.sharn.handpan.audio.PatternScheduler.buildSchedule(
            events = pattern.events,
            beatsPerBar = 4,
            totalBars = 1,
            assessmentSessionId = evaluatorSessionId,
            patternId = pattern.id
        ).first { it.target != null }.target!!
        evaluator.notifyExpectedTarget(target)

        assertEquals(evaluatorSessionId, target.identity.sessionId)
    }

    @Test
    fun mixedSessionIdsMustInvalidateAssessment() {
        val timeline = AssessmentTimeline()
        timeline.append(contextualEvent(eventId = "session-a", sessionId = "session-A"))
        timeline.append(contextualEvent(eventId = "session-b", sessionId = "session-B"))

        val quality = AssessmentSessionValidator.validate(timeline, 2_000L, 0.9f, 0)

        assertNull(SkillEvidenceCalculator.calculateValidEvidence(timeline, quality))
    }

    @Test
    fun assessmentSessionMustProduceDeterministicDuration() {
        val session = PracticeSessionContext.start("pattern", 1_000_000_000L)
        session.finalize(4_500_000_000L)

        assertEquals(3_500L, session.activeDurationMs)
    }

    @Test
    fun pausedTimeMustNotCountTowardActiveAssessmentDuration() {
        val session = PracticeSessionContext.start("pattern", 0L)
        session.pause(2_000_000_000L)
        session.resume(5_000_000_000L)
        session.finalize(6_500_000_000L)

        assertEquals(3_500L, session.activeDurationMs)
    }

    @Test
    fun naturalCompletionAndManualStopMustProduceEquivalentFinalization() {
        val session = PracticeSessionContext.start("pattern", 0L)
        session.finalize(2_000_000_000L)

        assertTrue(session.finalized)
    }

    @Test
    fun restartMustCreateNewAssessmentSession() {
        val sessionA = PracticeSessionContext.start("pattern", 1_000_000_000L)
        val sessionB = PracticeSessionContext.start("pattern", 1_000_000_001L, restartCount = 1)

        assertNotEquals(sessionA.sessionId, sessionB.sessionId)
        assertEquals(1, sessionB.restartCount)
    }

    @Test
    fun restartMustBeTrackedForSessionValidity() {
        val session = PracticeSessionContext.start("pattern", 0L, restartCount = 1)
        session.finalize(2_000_000_000L)
        val quality = AssessmentSessionValidator.derive(session, contextualTimeline(2, sessionId = session.sessionId))

        assertEquals(AssessmentSessionValidity.INVALID_RESTART, quality.validity)
    }

    @Test
    fun emptyAssessmentSessionMustNotProduceValidSkillEvidence() {
        val timeline = AssessmentTimeline()
        val quality = AssessmentSessionValidator.validate(timeline, 2_000L, 0.9f, 0)

        assertNull(SkillEvidenceCalculator.calculateValidEvidence(timeline, quality))
    }

    @Test
    fun sessionSignalQualityMustAggregateDetectedEvents() {
        val session = PracticeSessionContext.start("pattern-1", 0L)
        session.finalize(2_000_000_000L)
        val timeline = contextualTimeline(3, sessionId = session.sessionId, signalQualities = listOf(0.9f, 0.8f, 0.7f))
        val quality = AssessmentSessionValidator.derive(session, timeline)

        assertEquals(0.8f, quality.signalQuality, 0.001f)
    }

    @Test
    fun lowSignalQualityMustInvalidateSession() {
        val timeline = contextualTimeline(eventCount = 2)
        val quality = AssessmentSessionValidator.validate(timeline, 2_000L, 0.4f, 0)

        assertEquals(AssessmentSessionValidity.INVALID_SIGNAL, quality.validity)
    }

    @Test
    fun insufficientEvidenceMustInvalidateSession() {
        val timeline = AssessmentTimeline()
        timeline.append(contextualEvent(eventId = "only-event"))
        val quality = AssessmentSessionValidator.validate(timeline, 2_000L, 0.9f, 0)

        assertEquals(AssessmentSessionValidity.INVALID_TARGET_CONTEXT, quality.validity)
        assertNull(SkillEvidenceCalculator.calculateValidEvidence(timeline, quality))
    }

    @Test
    fun allAssessmentEventsMustShareCanonicalSessionId() {
        val timeline = contextualTimeline(eventCount = 2)
        val canonicalId = timeline.snapshot().first().sessionId

        assertTrue(timeline.snapshot().all { it.sessionId == canonicalId })
    }

    @Test
    fun skillEvidenceMustAcceptOnlyFinalizedValidSession() {
        val session = PracticeSessionContext.start("pattern-1", 0L)
        val timeline = contextualTimeline(eventCount = 2, sessionId = session.sessionId)

        assertNull(SkillEvidenceCalculator.calculateValidEvidence(session, timeline))
        session.finalize(2_000_000_000L)
        val finalizedQuality = AssessmentSessionValidator.derive(session, timeline)

        assertTrue(finalizedQuality.validity == AssessmentSessionValidity.VALID)
        assertNotNull(SkillEvidenceCalculator.calculateValidEvidence(timeline, finalizedQuality))
    }

    @Test
    fun sessionDurationBoundariesAreDeterministic() {
        val zero = PracticeSessionContext.start("pattern", 1_000L)
        zero.finalize(1_000L)
        val exact = PracticeSessionContext.start("pattern", 0L)
        exact.finalize(1_000_000_000L)
        val short = PracticeSessionContext.start("pattern", 0L)
        short.finalize(999_000_000L)

        assertEquals(0L, zero.activeDurationMs)
        assertEquals(1_000L, exact.activeDurationMs)
        assertEquals(999L, short.activeDurationMs)
    }

    @Test
    fun multiplePauseIntervalsAndRepeatedPauseAreIdempotent() {
        val session = PracticeSessionContext.start("pattern", 0L)
        session.pause(1_000_000_000L)
        session.pause(2_000_000_000L)
        session.resume(3_000_000_000L)
        session.pause(4_000_000_000L)
        session.resume(5_000_000_000L)
        session.finalize(6_000_000_000L)

        assertEquals(3_000L, session.activeDurationMs)
    }

    @Test
    fun stopWhilePausedAndRepeatedFinalizeAreDeterministic() {
        val session = PracticeSessionContext.start("pattern", 0L)
        session.pause(2_000_000_000L)
        session.finalize(5_000_000_000L)
        val end = session.endTimestampNanos
        session.finalize(9_000_000_000L)

        assertEquals(2_000L, session.activeDurationMs)
        assertEquals(end, session.endTimestampNanos)
        assertTrue(session.finalized)
    }

    @Test
    fun masteryMovesGraduallyAndConfidenceReflectsEvidence() {
        val initial = MasteredSkillState(skill = LearningSkill.TIMING)
        val evidence = MasteredSkillUpdater.evidenceFrom(
            CanonicalAssessmentMetrics(
                overallPerformance = 90f,
                timingScore = 92f,
                pitchScore = 80f,
                noteAccuracy = 85f,
                completionRate = 90f,
                missRate = 10f,
                falseStrikeRate = 0f,
                consistencyScore = 88f,
                confidenceScore = 95f
            )
        )

        val updated = MasteredSkillUpdater.update(
            initial,
            evidence[LearningSkill.TIMING]!!
        )

        assertTrue(updated.masteryScore in 0f..100f)
        assertTrue(updated.masteryScore < 90f)
        assertTrue(updated.attemptCount == 1)
        assertTrue(updated.confidence < 1f)
    }

    @Test
    fun personalizationSelectsWeakestSkillDeterministically() {
        val states = listOf(
            MasteredSkillState(LearningSkill.TIMING, masteryScore = 62f, attemptCount = 4),
            MasteredSkillState(LearningSkill.PITCH_ACCURACY, masteryScore = 48f, attemptCount = 4),
            MasteredSkillState(LearningSkill.CONSISTENCY, masteryScore = 75f, attemptCount = 4)
        )

        val recommendation = PersonalizationEngine.recommend(states, recentPatternIds = setOf("pattern-a"))

        assertEquals(LearningSkill.PITCH_ACCURACY, recommendation.skill)
        assertEquals(PracticeRecommendation.FOCUS_WEAKNESS, recommendation.reason)
        assertEquals("beg_05_scale_eight_notes", recommendation.patternId)
    }

    @Test
    fun everyMappedRecommendationResolvesToABuiltinPattern() {
        val builtinIds = BuiltinExercises.ALL_BUILTIN_PATTERNS.map { it.id }.toSet()

        assertTrue(LearningExerciseMapping.allMappedPatternIds.all { it in builtinIds })
        LearningSkill.values().forEach { skill ->
            val recommendation = PersonalizationEngine.recommend(
                states = listOf(MasteredSkillState(skill, masteryScore = 20f)),
                availablePatternIds = builtinIds
            )

            assertTrue(recommendation.patternId in builtinIds)
            assertTrue(recommendation.patternId !in setOf("pattern-note-accuracy", "pattern-timing", "review-timing"))
        }
    }

    @Test
    fun unmappedSkillUsesResolvableFallback() {
        val builtinIds = BuiltinExercises.ALL_BUILTIN_PATTERNS.map { it.id }.toSet()
        val recommendation = PersonalizationEngine.recommend(
            states = listOf(MasteredSkillState(LearningSkill.DYNAMICS, masteryScore = 20f)),
            availablePatternIds = builtinIds
        )

        assertEquals(LearningExerciseMapping.FALLBACK_PATTERN_ID, recommendation.patternId)
        assertTrue(recommendation.patternId in builtinIds)
    }

    @Test
    fun emptyAvailableSetUsesBuiltinCatalogFallback() {
        val recommendation = PersonalizationEngine.recommend(
            states = listOf(MasteredSkillState(LearningSkill.DYNAMICS, masteryScore = 20f)),
            availablePatternIds = emptySet()
        )

        assertEquals(LearningExerciseMapping.FALLBACK_PATTERN_ID, recommendation.patternId)
    }

    @Test
    fun skillEvidenceMustBeSkillSpecific() {
        val timeline = AssessmentTimeline()
        timeline.append(
            AssessmentTimelineEvent(
                eventId = "timing-1",
                sessionId = "session",
                loopId = null,
                sequenceIndex = 0,
                expectedNote = 0,
                detectedNote = 0,
                eventType = AssessmentEventType.CORRECT,
                expectedTimestampNanos = 1_000_000_000L,
                detectedTimestampNanos = 1_020_000_000L,
                deviationNanos = 20_000_000L,
                timingResult = TimingResult(TimingStatus.EXCELLENT, 20_000_000L),
                confidence = 0.9f,
                targetId = "target",
                source = "test",
                durationNanos = null,
                isConsumed = true,
                measuredAmplitude = 0.7f,
                measuredVelocity = 0.75f,
                accentStrength = 0.8f,
                expectedTechnique = HandpanTechnique.DING,
                detectedTechnique = HandpanTechnique.DING,
                targetBpm = 100
            )
        )
        val event = timeline.snapshot().single()
        assertEquals(0.7f, event.measuredAmplitude!!, 0.001f)
        assertEquals(HandpanTechnique.DING, event.detectedTechnique)
        assertEquals(100, event.targetBpm)
        val metrics = SkillEvidenceCalculator.calculate(
            timeline = timeline,
            rawMetrics = SkillEvidenceRawMetrics(
                subdivisionAccuracy = 61f,
                beatStability = 71f,
                targetBpm = 100,
                successfulBpm = 80,
                performanceAccuracy = 90f,
                dynamicsSamples = listOf(
                    DynamicsEvidenceSample(0.8f, 0.7f, 0.8f, 0.75f, true, 0.9f)
                ),
                techniqueEvents = listOf(
                    TechniqueEvidenceEvent(HandpanTechnique.DING, HandpanTechnique.DING, 0.8f)
                )
            )
        )

        val evidence = SkillEvidenceMapper.from(metrics)

        assertTrue(evidence[LearningSkill.RHYTHM]!!.performanceScore > 0f)
        assertTrue(evidence[LearningSkill.RHYTHM]!!.performanceScore != metrics.completionRate)
        assertTrue(evidence[LearningSkill.DYNAMICS]!!.performanceScore > 0f)
        assertTrue(evidence[LearningSkill.SPEED]!!.performanceScore > 0f)
        assertTrue(evidence[LearningSkill.TECHNIQUE]!!.performanceScore > 0f)
        assertEquals(90f, evidence[LearningSkill.RHYTHM]!!.confidence, 0.001f)
    }

    @Test
    fun assessmentEventMustContainTargetContext() {
        val event = contextualEvent()

        assertEquals("pattern-1", event.patternId)
        assertEquals("target-1", event.targetNoteId)
        assertEquals(90, event.targetBpm)
        assertEquals(Subdivision.SIXTEENTH, event.subdivision)
        assertEquals(1.25, event.beatPosition!!, 0.001)
        assertEquals(HandpanTechnique.TONE, event.expectedTechnique)
        assertEquals(AssessmentSessionValidity.VALID, event.sessionValidity)
    }

    @Test
    fun invalidSessionMustNotCreateSkillEvidence() {
        val timeline = AssessmentTimeline()
        timeline.append(contextualEvent(sessionValidity = AssessmentSessionValidity.INVALID_SIGNAL))

        val quality = AssessmentSessionValidator.validate(timeline, 2_000L, 0.9f, 0)
        assertEquals(null, SkillEvidenceCalculator.calculateValidEvidence(timeline, quality))
    }

    @Test
    fun rhythmEvidenceRequiresBeatContext() {
        val complete = contextualTimeline(eventCount = 2)
        val incomplete = AssessmentTimeline()
        incomplete.append(
            contextualEvent(
                subdivision = null,
                beatPosition = null,
                expectedTimingWindow = null
            )
        )

        val completeQuality = AssessmentSessionValidator.validate(complete, 2_000L, 0.9f, 0)
        val incompleteQuality = AssessmentSessionValidator.validate(incomplete, 2_000L, 0.9f, 0)
        val completeRhythm = SkillEvidenceCalculator.calculateValidEvidence(complete, completeQuality)!!.rhythmScore
        val incompleteEvidence = SkillEvidenceCalculator.calculateValidEvidence(incomplete, incompleteQuality)

        assertTrue(completeRhythm > 0f)
        assertEquals(null, incompleteEvidence)
    }

    @Test
    fun invalidAssessmentSessionMustNotAffectMastery() {
        val quality = AssessmentSessionValidator.validate(
            timeline = contextualTimeline(eventCount = 2),
            durationMs = 100L,
            signalQuality = 0.9f,
            restartCount = 0
        )

        assertEquals(AssessmentSessionValidity.INVALID_DURATION, quality.validity)
        assertEquals(null, SkillEvidenceCalculator.calculateValidEvidence(
            contextualTimeline(eventCount = 2),
            quality
        ))
    }

    @Test
    fun masteryMustUseOnlyValidSessions() {
        val validTimeline = contextualTimeline(eventCount = 2)
        val invalidTimeline = contextualTimeline(
            eventCount = 2,
            sessionValidity = AssessmentSessionValidity.INVALID_SIGNAL
        )
        val validQuality = AssessmentSessionValidator.validate(validTimeline, 2_000L, 0.9f, 0)
        val invalidQuality = AssessmentSessionValidator.validate(invalidTimeline, 2_000L, 0.9f, 0)

        assertTrue(SkillEvidenceCalculator.calculateValidEvidence(validTimeline, validQuality) != null)
        assertEquals(null, SkillEvidenceCalculator.calculateValidEvidence(invalidTimeline, invalidQuality))
    }

    @Test
    fun consistencyRequiresMultipleValidSessions() {
        val provider = object : SessionHistoryProvider {
            override fun recentSessions(): List<AssessmentTimeline> =
                listOf(contextualTimeline(eventCount = 2))
        }

        assertEquals(null, SkillEvidenceCalculator.calculateConsistency(provider))
    }

    private fun contextualTimeline(
        eventCount: Int,
        sessionId: String = "session",
        signalQualities: List<Float> = List(eventCount) { 0.8f },
        sessionValidity: AssessmentSessionValidity = AssessmentSessionValidity.VALID
    ): AssessmentTimeline {
        val timeline = AssessmentTimeline()
        repeat(eventCount) { index ->
            timeline.append(
                contextualEvent(
                    eventId = "context-event-$index",
                    sessionId = sessionId,
                    signalQuality = signalQualities[index],
                    sessionValidity = sessionValidity
                )
            )
        }
        return timeline
    }

    private fun contextualEvent(
        eventId: String = "context-event",
        sessionId: String = "session",
        signalQuality: Float = 0.8f,
        sessionValidity: AssessmentSessionValidity = AssessmentSessionValidity.VALID,
        subdivision: Subdivision? = Subdivision.SIXTEENTH,
        beatPosition: Double? = 1.25,
        expectedTimingWindow: TimingToleranceProfile? = TimingToleranceProfile(
            perfectWindowNanos = 20_000_000L,
            excellentWindowNanos = 40_000_000L,
            goodWindowNanos = 80_000_000L,
            missWindowNanos = 160_000_000L
        )
    ) = AssessmentTimelineEvent(
        eventId = eventId,
        sessionId = sessionId,
        loopId = "loop-1",
        sequenceIndex = 0,
        expectedNote = 1,
        detectedNote = 1,
        eventType = AssessmentEventType.CORRECT,
        expectedTimestampNanos = 1_000_000_000L,
        detectedTimestampNanos = 1_020_000_000L,
        deviationNanos = 20_000_000L,
        timingResult = TimingResult(TimingStatus.EXCELLENT, 20_000_000L),
        confidence = 0.9f,
        targetId = "target-1",
        source = "microphone",
        durationNanos = null,
        isConsumed = true,
        patternId = "pattern-1",
        targetNoteId = "target-1",
        targetBpm = 90,
        subdivision = subdivision,
        beatPosition = beatPosition,
        expectedTimingWindow = expectedTimingWindow,
        expectedTechnique = HandpanTechnique.TONE,
        detectedTechnique = HandpanTechnique.TONE,
        signalQuality = signalQuality,
        sessionValidity = sessionValidity
    )
}