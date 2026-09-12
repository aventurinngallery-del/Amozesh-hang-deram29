package com.sharn.handpan.data.local

import androidx.room.Entity
import com.sharn.handpan.model.CanonicalAssessmentMetrics

@Entity(tableName = "assessment_evidence")
data class EvidenceEntity(
    @androidx.room.PrimaryKey
    val sessionId: String,
    val validity: String,
    val validEvidenceCount: Int,
    val overallPerformance: Float,
    val timingScore: Float,
    val pitchScore: Float,
    val noteAccuracy: Float,
    val completionRate: Float,
    val missRate: Float,
    val falseStrikeRate: Float,
    val consistencyScore: Float,
    val confidenceScore: Float,
    val rhythmScore: Float,
    val dynamicsScore: Float,
    val speedScore: Float,
    val techniqueScore: Float
) {
    companion object {
        fun fromDomain(
            sessionId: String,
            validEvidenceCount: Int,
            validity: String,
            metrics: CanonicalAssessmentMetrics
        ) = EvidenceEntity(
            sessionId = sessionId,
            validity = validity,
            validEvidenceCount = validEvidenceCount,
            overallPerformance = metrics.overallPerformance,
            timingScore = metrics.timingScore,
            pitchScore = metrics.pitchScore,
            noteAccuracy = metrics.noteAccuracy,
            completionRate = metrics.completionRate,
            missRate = metrics.missRate,
            falseStrikeRate = metrics.falseStrikeRate,
            consistencyScore = metrics.consistencyScore,
            confidenceScore = metrics.confidenceScore,
            rhythmScore = metrics.rhythmScore,
            dynamicsScore = metrics.dynamicsScore,
            speedScore = metrics.speedScore,
            techniqueScore = metrics.techniqueScore
        )
    }
}