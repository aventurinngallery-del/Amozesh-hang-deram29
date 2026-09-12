package com.sharn.handpan.model

/**
 * Subdivision represents the division of each beat into smaller parts.
 * (تقسیم هر ضرب به قسمت‌های مساوی)
 */
enum class Subdivision(
    val divisionsPerBeat: Int,
    val persianName: String,
    val description: String
) {
    QUARTER(1, "سیاه (۱ ضرب)", "بدون تقسیم‌بندی - ۱ ضربه در هر ضرب"),
    EIGHTH(2, "چنگ (۲ ضربه)", "تقسیم هر ضرب به ۲ قسمت مساوی (۱ و ۲ و...)"),
    TRIPLET(3, "تریوله (۳ ضربه)", "تقسیم هر ضرب به ۳ قسمت مساوی"),
    SIXTEENTH(4, "دولاصنگ (۴ ضربه)", "تقسیم هر ضرب به ۴ قسمت مساوی")
}
