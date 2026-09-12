package com.sharn.handpan.model

data class DynamicsEvidenceSample(
    val expectedAmplitude: Float,
    val actualAmplitude: Float,
    val expectedVelocity: Float,
    val actualVelocity: Float,
    val expectedAccent: Boolean,
    val actualAccentStrength: Float
) {
    init {
        require(expectedAmplitude in 0f..1f)
        require(actualAmplitude in 0f..1f)
        require(expectedVelocity in 0f..1f)
        require(actualVelocity in 0f..1f)
        require(actualAccentStrength in 0f..1f)
    }
}

data class TechniqueEvidenceEvent(
    val expectedTechnique: HandpanTechnique,
    val actualTechnique: HandpanTechnique,
    val articulationQuality: Float
) {
    init {
        require(articulationQuality in 0f..1f)
    }
}

data class SkillEvidenceRawMetrics(
    val subdivisionAccuracy: Float? = null,
    val beatStability: Float? = null,
    val targetBpm: Int? = null,
    val successfulBpm: Int? = null,
    val performanceAccuracy: Float? = null,
    val dynamicsSamples: List<DynamicsEvidenceSample> = emptyList(),
    val techniqueEvents: List<TechniqueEvidenceEvent> = emptyList()
) {
    init {
        require(subdivisionAccuracy == null || subdivisionAccuracy in 0f..100f)
        require(beatStability == null || beatStability in 0f..100f)
        require(targetBpm == null || targetBpm > 0)
        require(successfulBpm == null || successfulBpm > 0)
        require(performanceAccuracy == null || performanceAccuracy in 0f..100f)
    }
}

interface SessionHistoryProvider {
    fun recentSessions(): List<AssessmentTimeline>
}

object SkillEvidenceCalculator {
    fun calculateConsistency(provider: SessionHistoryProvider): Float? {
        val sessions = provider.recentSessions()
        if (sessions.size < 2) return null
        val quality = sessions.map {
            AssessmentSessionValidator.validate(it, AssessmentSessionValidator.MINIMUM_DURATION_MS, 1f, 0)
        }
        if (quality.any { it.validity != AssessmentSessionValidity.VALID }) return null
        return sessions.map { calculate(it).consistencyScore }.average().toFloat()
    }

    @Deprecated("Use calculateValidEvidence(session, timeline) to derive quality from lifecycle facts.")
    fun calculateValidEvidence(
        timeline: AssessmentTimeline,
        quality: AssessmentSessionQuality
    ): CanonicalAssessmentMetrics? {
        if (quality.validity != AssessmentSessionValidity.VALID) return null
        if (quality.sessionId != timeline.snapshot().firstOrNull()?.sessionId) return null
        return calculate(timeline)
    }

    fun calculateValidEvidence(
        session: PracticeSessionContext,
        timeline: AssessmentTimeline
    ): CanonicalAssessmentMetrics? {
        val quality = AssessmentSessionValidator.derive(session, timeline)
        if (!session.finalized) return null
        return calculateValidEvidence(timeline, quality)
    }

    fun calculate(
        timeline: AssessmentTimeline,
        rawMetrics: SkillEvidenceRawMetrics = SkillEvidenceRawMetrics(),
        baseMetrics: CanonicalAssessmentMetrics = PracticeScoreCalculator.calculateMetrics(timeline.snapshot())
    ): CanonicalAssessmentMetrics {
        val events = timeline.snapshot()
        return baseMetrics.copy(
            rhythmScore = calculateRhythm(events, rawMetrics),
            dynamicsScore = calculateDynamics(events, rawMetrics.dynamicsSamples),
            speedScore = calculateSpeed(rawMetrics),
            techniqueScore = calculateTechnique(events, rawMetrics.techniqueEvents)
        )
    }

    private fun calculateRhythm(
        events: List<AssessmentTimelineEvent>,
        rawMetrics: SkillEvidenceRawMetrics
    ): Float {
        val contextualEvents = events.filter { it.eventType != AssessmentEventType.EXPECTED && it.eventType != AssessmentEventType.EXTRA }
        val timingScores = contextualEvents
            .filter { it.eventType != AssessmentEventType.EXPECTED && it.eventType != AssessmentEventType.EXTRA }
            .mapNotNull { it.timingResult?.status?.rhythmScore }
        val timingScore = timingScores.averageOrZero()
        val deviationScore = contextualEvents.mapNotNull { it.contextualRhythmScore() }.averageOrZero()
        val components = listOfNotNull(
            rawMetrics.subdivisionAccuracy,
            rawMetrics.beatStability,
            if (timingScores.isNotEmpty()) timingScore else null,
            if (contextualEvents.any { it.contextualRhythmScore() != null }) deviationScore else null
        )
        return components.averageOrZero()
    }

    private fun AssessmentTimelineEvent.contextualRhythmScore(): Float? {
        val actualTimestamp = detectedTimestampNanos ?: return null
        val expectedTimestamp = expectedTimestampNanos ?: return null
        val bpm = targetBpm ?: return null
        val subdivisionValue = subdivision ?: return null
        val beat = beatPosition ?: return null
        val window = expectedTimingWindow ?: return null
        val beatDuration = 60_000_000_000.0 / bpm.toDouble()
        val subdivisionPosition = beat - kotlin.math.floor(beat)
        val expectedSubdivisionPosition = kotlin.math.round(
            subdivisionPosition * subdivisionValue.divisionsPerBeat
        ) / subdivisionValue.divisionsPerBeat
        val subdivisionErrorNanos = kotlin.math.abs(
            (subdivisionPosition - expectedSubdivisionPosition) * beatDuration
        )
        val timingErrorNanos = kotlin.math.abs(actualTimestamp - expectedTimestamp)
        val combinedError = timingErrorNanos + subdivisionErrorNanos
        val normalized = (combinedError.toDouble() / window.missWindowNanos).coerceIn(0.0, 1.0)
        return ((1.0 - normalized) * 100.0).toFloat()
    }

    private fun calculateDynamics(
        events: List<AssessmentTimelineEvent>,
        samples: List<DynamicsEvidenceSample>
    ): Float {
        if (samples.isEmpty()) {
            val measured = events.mapNotNull { event ->
                val amplitude = event.measuredAmplitude ?: return@mapNotNull null
                val velocity = event.measuredVelocity ?: return@mapNotNull null
                val accent = event.accentStrength ?: 0f
                (amplitude + velocity + accent) / 3f * 100f
            }
            return measured.averageOrZero().coerceIn(0f, 100f)
        }
        if (samples.isEmpty()) return 0f
        return samples.map { sample ->
            val amplitudeScore = similarity(sample.expectedAmplitude, sample.actualAmplitude)
            val velocityScore = similarity(sample.expectedVelocity, sample.actualVelocity)
            val accentScore = if (sample.expectedAccent) {
                sample.actualAccentStrength * 100f
            } else {
                (1f - sample.actualAccentStrength).coerceIn(0f, 1f) * 100f
            }
            (amplitudeScore + velocityScore + accentScore) / 3f
        }.average().toFloat().coerceIn(0f, 100f)
    }

    private fun calculateSpeed(rawMetrics: SkillEvidenceRawMetrics): Float {
        val targetBpm = rawMetrics.targetBpm ?: return 0f
        val successfulBpm = rawMetrics.successfulBpm ?: return 0f
        val accuracy = rawMetrics.performanceAccuracy ?: return 0f
        val tempoPressure = (successfulBpm.toFloat() / targetBpm.toFloat() * 100f).coerceIn(0f, 100f)
        return (accuracy * 0.7f + tempoPressure * 0.3f).coerceIn(0f, 100f)
    }

    private fun calculateTechnique(
        timelineEvents: List<AssessmentTimelineEvent>,
        events: List<TechniqueEvidenceEvent>
    ): Float {
        val techniqueEvents = if (events.isEmpty()) {
            timelineEvents.mapNotNull { event ->
                val expected = event.expectedTechnique ?: return@mapNotNull null
                val actual = event.detectedTechnique ?: return@mapNotNull null
                TechniqueEvidenceEvent(expected, actual, event.confidence)
            }
        } else {
            events
        }
        if (techniqueEvents.isEmpty()) return 0f
        return techniqueEvents.map { event ->
            val techniqueMatch = if (event.expectedTechnique == event.actualTechnique) 100f else 0f
            techniqueMatch * 0.7f + event.articulationQuality * 100f * 0.3f
        }.average().toFloat().coerceIn(0f, 100f)
    }

    private fun deviationStability(events: List<AssessmentTimelineEvent>): Float {
        val deviations = events.mapNotNull { it.deviationNanos?.toDouble() }
        if (deviations.isEmpty()) return 0f
        val mean = deviations.average()
        val standardDeviationMs = kotlin.math.sqrt(
            deviations.map { (it - mean) * (it - mean) }.average()
        ) / 1_000_000.0
        return (100.0 - standardDeviationMs).coerceIn(0.0, 100.0).toFloat()
    }

    private fun similarity(expected: Float, actual: Float): Float =
        (100f - kotlin.math.abs(expected - actual) * 100f).coerceIn(0f, 100f)

    private val TimingStatus.rhythmScore: Float
        get() = when (this) {
            TimingStatus.PERFECT -> 100f
            TimingStatus.EXCELLENT -> 90f
            TimingStatus.GOOD -> 80f
            TimingStatus.EARLY, TimingStatus.LATE -> 50f
            TimingStatus.OUTSIDE_WINDOW -> 0f
        }

    private fun List<Float>.averageOrZero(): Float = if (isEmpty()) 0f else average().toFloat()
}