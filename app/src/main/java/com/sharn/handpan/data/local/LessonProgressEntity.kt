package com.sharn.handpan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing persistent progress and masterclass scores for a Handpan lesson.
 */
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey
    val lessonId: String,
    val isCompleted: Boolean = false,
    val stars: Int = 0,
    val bestScore: Int = 0,
    val attempts: Int = 0,
    val lastPracticedAt: Long = System.currentTimeMillis()
)
