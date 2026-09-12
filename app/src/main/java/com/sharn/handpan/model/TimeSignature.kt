package com.sharn.handpan.model

/**
 * Represents a musical time signature (میزان‌نما).
 */
data class TimeSignature(
    val numerator: Int = 4,   // Number of beats per bar (تعداد ضرب در هر میزان)
    val denominator: Int = 4, // Note value that represents one beat (ارزش زمانی هر ضرب)
    val grouping: List<Int> = defaultGrouping(numerator, denominator)
) {
    init {
        require(numerator > 0) { "Numerator must be positive" }
        require(denominator > 0 && denominator and (denominator - 1) == 0) {
            "Denominator must be a positive power of two"
        }
        require(grouping.isNotEmpty() && grouping.sum() == numerator) {
            "Grouping must contain positive values that sum to the numerator"
        }
        require(grouping.all { it > 0 }) { "Grouping values must be positive" }
    }

    val displayName: String
        get() = "$numerator/$denominator"

    val persianDisplayName: String
        get() = when ("$numerator/$denominator") {
            "4/4" -> "۴/۴ (چهار چهارم)"
            "3/4" -> "۳/۴ (سه چهارم - والس)"
            "2/4" -> "۲/۴ (دو چهارم - مارش/ساده)"
            "6/8" -> "۶/۸ (شش و هشت ریتمیک)"
            else -> "$numerator/$denominator"
        }

    val beatsPerBar: Int
        get() = numerator

    val pulseCount: Int
        get() = grouping.size

    fun isGroupedAccent(beatNumber: Int): Boolean {
        if (beatNumber !in 1..numerator) return false
        var start = 1
        return grouping.any { groupSize ->
            val isStart = beatNumber == start
            start += groupSize
            isStart
        }
    }

    companion object {
        private fun defaultGrouping(numerator: Int, denominator: Int): List<Int> = when {
            numerator == 6 && denominator == 8 -> listOf(3, 3)
            numerator == 9 && denominator == 8 -> listOf(3, 3, 3)
            numerator == 12 && denominator == 8 -> listOf(3, 3, 3, 3)
            else -> listOf(numerator)
        }

        val Common44 = TimeSignature(4, 4)
        val Waltz34 = TimeSignature(3, 4)
        val March24 = TimeSignature(2, 4)
        val SixEight68 = TimeSignature(6, 8, listOf(3, 3))
        val SevenEight272 = TimeSignature(7, 8, listOf(2, 2, 3))
        val SevenEight322 = TimeSignature(7, 8, listOf(3, 2, 2))

        val ALL_PRESETS = listOf(Common44, Waltz34, March24, SixEight68, SevenEight272, SevenEight322)
    }
}
