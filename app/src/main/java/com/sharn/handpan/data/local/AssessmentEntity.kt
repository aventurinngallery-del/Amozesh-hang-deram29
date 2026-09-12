package com.sharn.handpan.data.local

import androidx.room.Entity
import com.sharn.handpan.model.FinalizedAssessment

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @androidx.room.PrimaryKey
    val sessionId: String,
    val patternId: String,
    val bpm: Int,
    val completedAtEpochMs: Long,
    val durationMs: Long,
    val activeDurationMs: Long,
    val validity: String,
    val qualityScore: Float,
    val signalQuality: Float,
    val validEventCount: Int,
    val eventCount: Int,
    val restartCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val missedCount: Int,
    val extraCount: Int,
    val unknownCount: Int,
    val timingScore: Float,
    val pitchScore: Float,
    val noteAccuracy: Float,
    val overallPerformance: Float,
    val consistencyScore: Float
) {
    companion object {
        fun fromDomain(assessment: FinalizedAssessment) = AssessmentEntity(
            sessionId = assessment.sessionId,
            patternId = assessment.patternId,
            bpm = assessment.bpm,
            completedAtEpochMs = assessment.completedAtEpochMs,
            durationMs = assessment.quality.durationMs,
            activeDurationMs = assessment.quality.activeDurationMs,
            validity = assessment.quality.validity.name,
            qualityScore = assessment.quality.qualityScore,
            signalQuality = assessment.quality.signalQuality,
            validEventCount = assessment.quality.validEventCount,
            eventCount = assessment.quality.eventCount,
            restartCount = assessment.quality.restartCount,
            correctCount = assessment.score.correctCount,
            wrongCount = assessment.score.wrongCount,
            missedCount = assessment.score.missedCount,
            extraCount = assessment.score.extraCount,
            unknownCount = assessment.score.unknownCount,
            timingScore = assessment.metrics.timingScore,
            pitchScore = assessment.metrics.pitchScore,
            noteAccuracy = assessment.metrics.noteAccuracy,
            overallPerformance = assessment.metrics.overallPerformance,
            consistencyScore = assessment.metrics.consistencyScore
        )
    }
}
