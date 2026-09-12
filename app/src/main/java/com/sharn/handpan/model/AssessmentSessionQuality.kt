package com.sharn.handpan.model

data class AssessmentSessionQuality(
    val sessionId: String,
    val durationMs: Long,
    val eventCount: Int,
    val validEventCount: Int,
    val signalQuality: Float,
    val restartCount: Int,
    val contextCompleteness: Float,
    val qualityScore: Float,
    val validity: AssessmentSessionValidity,
    val finalized: Boolean = false,
    val activeDurationMs: Long = durationMs,
    val validStrikeRatio: Float = if (eventCount == 0) 0f else validEventCount.toFloat() / eventCount
) {
    init {
        require(sessionId.isNotBlank())
        require(durationMs >= 0L)
        require(eventCount >= 0)
        require(validEventCount in 0..eventCount)
        require(signalQuality in 0f..1f)
        require(restartCount >= 0)
        require(contextCompleteness in 0f..1f)
        require(qualityScore in 0f..100f)
        require(activeDurationMs >= 0L)
        require(validStrikeRatio in 0f..1f)
    }
}

object AssessmentSessionValidator {
    const val MINIMUM_DURATION_MS = 1_000L
    const val MINIMUM_EVIDENCE_EVENTS = 2
    const val MINIMUM_SIGNAL_QUALITY = 0.5f

    @Deprecated("Use derive(session, timeline) so quality cannot be fabricated by a caller.")
    fun validate(
        timeline: AssessmentTimeline,
        durationMs: Long,
        signalQuality: Float,
        restartCount: Int
    ): AssessmentSessionQuality {
        val events = timeline.snapshot()
        val sessionIds = events.map { it.sessionId }.toSet()
        val validEventCount = events.count { it.hasCompleteTargetContext() }
        val contextCompleteness = if (events.isEmpty()) 0f else validEventCount.toFloat() / events.size
        val validity = when {
            restartCount > 0 -> AssessmentSessionValidity.INVALID_RESTART
            durationMs < MINIMUM_DURATION_MS -> AssessmentSessionValidity.INVALID_DURATION
            signalQuality < MINIMUM_SIGNAL_QUALITY -> AssessmentSessionValidity.INVALID_SIGNAL
            sessionIds.size != 1 || events.any { it.sessionValidity != AssessmentSessionValidity.VALID } ->
                AssessmentSessionValidity.INVALID_TARGET_CONTEXT
            validEventCount < MINIMUM_EVIDENCE_EVENTS -> AssessmentSessionValidity.INVALID_TARGET_CONTEXT
            else -> AssessmentSessionValidity.VALID
        }
        val qualityScore = (
            (durationMs.toFloat() / MINIMUM_DURATION_MS).coerceIn(0f, 1f) * 25f +
                (validEventCount.toFloat() / MINIMUM_EVIDENCE_EVENTS).coerceIn(0f, 1f) * 25f +
                signalQuality.coerceIn(0f, 1f) * 25f +
                contextCompleteness * 25f
            ).coerceIn(0f, 100f)
        return AssessmentSessionQuality(
            sessionId = events.firstOrNull()?.sessionId ?: "empty-session",
            durationMs = durationMs,
            eventCount = events.size,
            validEventCount = validEventCount,
            signalQuality = signalQuality.coerceIn(0f, 1f),
            restartCount = restartCount,
            contextCompleteness = contextCompleteness,
            qualityScore = qualityScore,
            validity = validity
        )
    }

    fun derive(
        session: PracticeSessionContext,
        timeline: AssessmentTimeline
    ): AssessmentSessionQuality {
        val events = timeline.snapshot()
        val sessionIds = events.map { it.sessionId }.toSet()
        val evidenceEvents = events.filter {
            it.eventType != AssessmentEventType.EXPECTED && it.eventType != AssessmentEventType.EXTRA
        }
        val validEventCount = evidenceEvents.count {
            it.eventType != AssessmentEventType.MISSED && it.hasCompleteTargetContext()
        }
        val contextCompleteness = if (evidenceEvents.isEmpty()) 0f
        else validEventCount.toFloat() / evidenceEvents.size
        val signalValues = evidenceEvents.mapNotNull { it.signalQuality }
        val signalQuality = if (signalValues.isEmpty()) 0f else signalValues.average().toFloat()
        val validity = when {
            !session.finalized -> AssessmentSessionValidity.INVALID_NOT_FINALIZED
            session.restartCount > 0 -> AssessmentSessionValidity.INVALID_RESTART
            session.activeDurationMs < MINIMUM_DURATION_MS -> AssessmentSessionValidity.INVALID_DURATION
            signalQuality < MINIMUM_SIGNAL_QUALITY -> AssessmentSessionValidity.INVALID_SIGNAL
            sessionIds.size != 1 || sessionIds.firstOrNull() != session.sessionId ||
                evidenceEvents.any { it.sessionValidity != AssessmentSessionValidity.VALID } ->
                AssessmentSessionValidity.INVALID_TARGET_CONTEXT
            validEventCount < MINIMUM_EVIDENCE_EVENTS -> AssessmentSessionValidity.INVALID_TARGET_CONTEXT
            else -> AssessmentSessionValidity.VALID
        }
        val qualityScore = (
            (session.activeDurationMs.toFloat() / MINIMUM_DURATION_MS).coerceIn(0f, 1f) * 25f +
                (validEventCount.toFloat() / MINIMUM_EVIDENCE_EVENTS).coerceIn(0f, 1f) * 25f +
                signalQuality * 25f + contextCompleteness * 25f
            ).coerceIn(0f, 100f)
        return AssessmentSessionQuality(
            sessionId = session.sessionId,
            durationMs = session.elapsedDurationNanos / 1_000_000L,
            eventCount = evidenceEvents.size,
            validEventCount = validEventCount,
            signalQuality = signalQuality.coerceIn(0f, 1f),
            restartCount = session.restartCount,
            contextCompleteness = contextCompleteness,
            qualityScore = qualityScore,
            validity = validity,
            finalized = session.finalized,
            activeDurationMs = session.activeDurationMs,
            validStrikeRatio = if (evidenceEvents.isEmpty()) 0f else validEventCount.toFloat() / evidenceEvents.size
        )
    }

    private fun AssessmentTimelineEvent.hasCompleteTargetContext(): Boolean =
        sessionValidity == AssessmentSessionValidity.VALID &&
            sessionId.isNotBlank() &&
            !patternId.isNullOrBlank() &&
            !targetNoteId.isNullOrBlank() &&
            targetBpm != null &&
            subdivision != null &&
            beatPosition != null &&
            expectedTimingWindow != null &&
            expectedTechnique != null
}