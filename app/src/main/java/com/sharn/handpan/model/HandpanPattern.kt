package com.sharn.handpan.model

enum class DifficultyLevel(val persianLabel: String) {
    BEGINNER("مقدماتی"),
    INTERMEDIATE("متوسط"),
    ADVANCED("پیشرفته")
}

enum class PatternCategory(val persianTitle: String, val icon: String) {
    BEGINNER("آموزش مبتدی", "school"),
    RHYTHM("تمرین ریتم و اسلپ", "schedule"),
    INDEPENDENCE("استقلال دست‌ها و پارادیدل", "swap_horiz"),
    MELODY("ملودی و قطعات", "music_note"),
    WARM_UP("گرم‌کردن و چابکی", "fitness_center"),
    CUSTOM("الگوهای شخصی من", "edit")
}

/**
 * Standard data model representing a playable Handpan musical pattern / exercise.
 */
data class HandpanPattern(
    val id: String,
    val title: String,
    val description: String,
    val bpm: Int = 70,
    val timeSignature: TimeSignature = TimeSignature.Common44,
    val bars: Int = 1,
    val events: List<NoteEvent>,
    val difficulty: DifficultyLevel = DifficultyLevel.BEGINNER,
    val category: PatternCategory = PatternCategory.BEGINNER,
    val isCustom: Boolean = false,
    val recommendedSubdivision: Subdivision = Subdivision.QUARTER
) {
    init {
        require(bpm in 30..300) { "BPM must be between 30 and 300" }
        require(bars > 0) { "Bars must be positive" }
        require(events.all { it.beatPosition < totalBeats }) {
            "Every event must start inside the pattern duration"
        }
    }

    val orderedEvents: List<NoteEvent>
        get() = events.withIndex()
            .sortedWith(compareBy<IndexedValue<NoteEvent>> { it.value.beatPosition }.thenBy { it.index })
            .map { it.value }

    val totalBeats: Double
        get() = (bars * timeSignature.beatsPerBar).toDouble()

    /**
     * Get all active non-rest note events
     */
    val activeNotes: List<NoteEvent>
        get() = events.filter { !it.isRest }

    /**
     * Formats the sequence of notes as a scannable string (e.g., "D - 1 - S - 1")
     */
    val notesSummary: String
        get() = notesSummary(NotationSystem.NUMERIC)

    fun notesSummary(system: NotationSystem): String {
            if (events.isEmpty()) return "خالی"
            return events.joinToString(" - ") {
                val symbol = NotationRenderer.render(
                    noteNumber = it.noteNumber,
                    technique = it.technique,
                    system = system
                )
                if (it.isRest) "𝄽" else if (it.accent) "[$symbol]" else symbol
            }
        }
}
