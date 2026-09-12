package com.sharn.handpan.model

data class ScoreCounters(
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val unknownCount: Int = 0,
    val missedCount: Int = 0,
    val extraCount: Int = 0,
    val perfectCount: Int = 0,
    val excellentCount: Int = 0,
    val goodCount: Int = 0,
    val earlyCount: Int = 0,
    val lateCount: Int = 0,
    val nonCorrectTimingPoints: Int = 0
)

data class AssessmentWeightProfile(
    val timing: Float = 0.30f,
    val noteAccuracy: Float = 0.30f,
    val completion: Float = 0.20f,
    val consistency: Float = 0.10f,
    val confidence: Float = 0.10f
) {
    init {
        require(timing >= 0f && noteAccuracy >= 0f && completion >= 0f && consistency >= 0f && confidence >= 0f)
        require((timing + noteAccuracy + completion + consistency + confidence) in 0.999f..1.001f)
    }
}

data class CanonicalAssessmentMetrics(
    val overallPerformance: Float,
    val timingScore: Float,
    val pitchScore: Float,
    val noteAccuracy: Float,
    val completionRate: Float,
    val missRate: Float,
    val falseStrikeRate: Float,
    val consistencyScore: Float,
    val confidenceScore: Float,
    val rhythmScore: Float = 0f,
    val dynamicsScore: Float = 0f,
    val speedScore: Float = 0f,
    val techniqueScore: Float = 0f
) {
    companion object {
        val EMPTY = CanonicalAssessmentMetrics(
            overallPerformance = 0f,
            timingScore = 0f,
            pitchScore = 0f,
            noteAccuracy = 0f,
            completionRate = 0f,
            missRate = 0f,
            falseStrikeRate = 0f,
            consistencyScore = 100f,
            confidenceScore = 0f
        )
    }
}

object PracticeScoreCalculator {
    fun calculateMetrics(
        events: List<AssessmentTimelineEvent>,
        weights: AssessmentWeightProfile = AssessmentWeightProfile()
    ): CanonicalAssessmentMetrics {
        val expected = events.filter { it.eventType == AssessmentEventType.EXPECTED }
        val results = events.filter { it.eventType != AssessmentEventType.EXPECTED }
        val scored = results.filter { it.eventType != AssessmentEventType.EXTRA && it.eventType != AssessmentEventType.MISSED }
        val correct = results.count { it.eventType == AssessmentEventType.CORRECT }
        val noteDenominator = results.count {
            it.eventType == AssessmentEventType.CORRECT ||
                it.eventType == AssessmentEventType.WRONG ||
                it.eventType == AssessmentEventType.UNKNOWN ||
                it.eventType == AssessmentEventType.MISSED
        }
        val timingValues = scored.map { event ->
            when (event.timingResult?.status) {
                TimingStatus.PERFECT -> 100f
                TimingStatus.EXCELLENT -> 90f
                TimingStatus.GOOD -> 80f
                TimingStatus.EARLY, TimingStatus.LATE -> 50f
                else -> 0f
            }
        }
        val timingScore = timingValues.averageOrZero()
        val deviations = scored.mapNotNull { it.deviationNanos?.toDouble() }
        val consistencyScore = if (deviations.size < 2) 100f else {
            val mean = deviations.average()
            (100.0 - kotlin.math.sqrt(deviations.map { (it - mean) * (it - mean) }.average()) / 1_000_000.0)
                .coerceIn(0.0, 100.0).toFloat()
        }
        val detected = results.filter { it.eventType != AssessmentEventType.MISSED }
        val confidenceScore = (detected.map { it.confidence }.averageOrZero() * 100f)
            .coerceIn(0f, 100f)
        val completionRate = percentage(correct, expected.size)
        val missRate = percentage(results.count { it.eventType == AssessmentEventType.MISSED }, expected.size)
        val falseStrikeRate = percentage(results.count { it.eventType == AssessmentEventType.EXTRA }, results.size)
        val noteAccuracy = percentage(correct, noteDenominator)
        val pitchDenominator = results.count {
            it.eventType == AssessmentEventType.CORRECT ||
                it.eventType == AssessmentEventType.WRONG ||
                it.eventType == AssessmentEventType.UNKNOWN
        }
        val pitchScore = percentage(correct, pitchDenominator)
        val overall = (
            timingScore * weights.timing +
                confidenceScore * weights.confidence +
                noteAccuracy * weights.noteAccuracy +
                completionRate * weights.completion +
                consistencyScore * weights.consistency
            ).coerceIn(0f, 100f)
        return CanonicalAssessmentMetrics(
            overallPerformance = overall,
            timingScore = timingScore,
            pitchScore = pitchScore,
            noteAccuracy = noteAccuracy,
            completionRate = completionRate,
            missRate = missRate,
            falseStrikeRate = falseStrikeRate,
            consistencyScore = consistencyScore,
            confidenceScore = confidenceScore
        )
    }

    fun calculate(timeline: AssessmentTimeline): PracticeScore =
        calculate(timeline.snapshot())

    fun calculate(events: List<AssessmentTimelineEvent>): PracticeScore {
        val results = events.filter { it.eventType != AssessmentEventType.EXPECTED }
        val correct = results.count { it.eventType == AssessmentEventType.CORRECT }
        val wrong = results.count { it.eventType == AssessmentEventType.WRONG }
        val unknown = results.count { it.eventType == AssessmentEventType.UNKNOWN }
        val missed = results.count { it.eventType == AssessmentEventType.MISSED }
        val extra = results.count { it.eventType == AssessmentEventType.EXTRA }
        fun timingPoints(event: AssessmentTimelineEvent): Int = when (event.timingResult?.status) {
            TimingStatus.PERFECT -> 100
            TimingStatus.EXCELLENT -> 90
            TimingStatus.GOOD -> 80
            TimingStatus.EARLY, TimingStatus.LATE -> 50
            TimingStatus.OUTSIDE_WINDOW, null -> 0
        }
        val nonCorrectTimingPoints = results
            .filter { it.eventType == AssessmentEventType.WRONG || it.eventType == AssessmentEventType.UNKNOWN }
            .sumOf(::timingPoints)
        return calculate(
            ScoreCounters(
                correctCount = correct,
                wrongCount = wrong,
                unknownCount = unknown,
                missedCount = missed,
                extraCount = extra,
                perfectCount = results.count { it.timingResult?.status == TimingStatus.PERFECT },
                excellentCount = results.count { it.timingResult?.status == TimingStatus.EXCELLENT },
                goodCount = results.count { it.timingResult?.status == TimingStatus.GOOD },
                earlyCount = results.count { it.timingResult?.status == TimingStatus.EARLY },
                lateCount = results.count { it.timingResult?.status == TimingStatus.LATE },
                nonCorrectTimingPoints = nonCorrectTimingPoints
            )
        ).copy(maxCombo = maxCombo(events), score = score(events))
    }

    fun calculate(counters: ScoreCounters): PracticeScore {
        val noteDenominator = counters.correctCount + counters.wrongCount +
            counters.unknownCount + counters.missedCount
        val timingDenominator = counters.correctCount + counters.wrongCount + counters.unknownCount
        val timingPoints = counters.perfectCount * 100 +
            counters.excellentCount * 90 +
            counters.goodCount * 80 +
            counters.earlyCount * 50 +
            counters.lateCount * 50 +
            counters.nonCorrectTimingPoints

        val noteAccuracy = percentage(counters.correctCount, noteDenominator)
        val timingAccuracy = percentage(timingPoints, timingDenominator * 100)
        val overall = if (noteDenominator == 0 && timingDenominator == 0) {
            0f
        } else {
            ((noteAccuracy + timingAccuracy) / 2f).coerceIn(0f, 100f)
        }

        return PracticeScore(
            correctCount = counters.correctCount,
            wrongCount = counters.wrongCount,
            unknownCount = counters.unknownCount,
            missedCount = counters.missedCount,
            extraCount = counters.extraCount,
            noteAccuracyPercentage = noteAccuracy,
            timingAccuracyPercentage = timingAccuracy,
            overallAccuracyPercentage = overall,
            maxCombo = maxCombo(counters),
            score = score(counters)
        )
    }

    fun calculateResult(events: List<AssessmentTimelineEvent>, durationMs: Long): PracticeResult {
        val score = calculate(events)
        val results = events.filter { it.eventType != AssessmentEventType.EXPECTED }
        val perfect = results.count { it.timingResult?.status == TimingStatus.PERFECT }
        val good = results.count { it.timingResult?.status == TimingStatus.GOOD }
        val excellent = results.count { it.timingResult?.status == TimingStatus.EXCELLENT }
        val early = results.count { it.timingResult?.status == TimingStatus.EARLY }
        val late = results.count { it.timingResult?.status == TimingStatus.LATE }
        val completed = results
            .filter { it.eventType == AssessmentEventType.CORRECT }
            .mapNotNull { it.targetId }
            .distinct()
            .count()
        return PracticeResult(
            score = score.score,
            accuracy = score.overallAccuracyPercentage,
            timingAccuracy = score.timingAccuracyPercentage,
            noteAccuracy = score.noteAccuracyPercentage,
            perfectCount = perfect,
            greatCount = excellent,
            goodCount = good,
            earlyCount = early,
            lateCount = late,
            missCount = score.missedCount,
            wrongNoteCount = score.wrongCount + score.unknownCount,
            maxCombo = score.maxCombo,
            totalTargets = events.count { it.eventType == AssessmentEventType.EXPECTED },
            completedTargets = completed,
            durationMs = durationMs.coerceAtLeast(0L)
        )
    }

    fun maxCombo(events: List<AssessmentTimelineEvent>): Int {
        var current = 0
        var maximum = 0
        events.filter { it.eventType != AssessmentEventType.EXPECTED }.forEach { event ->
            if (event.eventType == AssessmentEventType.CORRECT) {
                current++
                maximum = maxOf(maximum, current)
            } else {
                current = 0
            }
        }
        return maximum
    }

    private fun maxCombo(counters: ScoreCounters): Int = counters.correctCount

    private fun score(events: List<AssessmentTimelineEvent>): Int {
        var combo = 0
        return events.filter { it.eventType != AssessmentEventType.EXPECTED }.sumOf { event ->
            if (event.eventType != AssessmentEventType.CORRECT) {
                combo = 0
                return@sumOf if (event.eventType == AssessmentEventType.MISSED) -25 else -10
            }
            combo++
            val base = when (event.timingResult?.status) {
                TimingStatus.PERFECT -> 100
                TimingStatus.GOOD -> 80
                TimingStatus.EARLY, TimingStatus.LATE -> 50
                else -> 0
            }
            base + (combo / 5)
        }.coerceAtLeast(0)
    }

    private fun score(counters: ScoreCounters): Int = (
        counters.perfectCount * 100 +
            counters.goodCount * 80 +
            (counters.earlyCount + counters.lateCount) * 50 -
            counters.missedCount * 25 -
            (counters.wrongCount + counters.unknownCount) * 10
        ).coerceAtLeast(0)

    private fun percentage(numerator: Int, denominator: Int): Float {
        if (denominator <= 0) return 0f
        return (numerator.toFloat() / denominator.toFloat() * 100f).coerceIn(0f, 100f)
    }

    private fun List<Float>.averageOrZero(): Float = if (isEmpty()) 0f else average().toFloat()
}