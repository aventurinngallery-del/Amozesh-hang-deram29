package com.sharn.handpan.model

/**
 * Modes of practicing a Handpan pattern.
 */
enum class PracticeMode(
    val title: String,
    val persianTitle: String,
    val description: String,
    val iconName: String
) {
    FOLLOW(
        title = "Follow Mode",
        persianTitle = "حالت هم‌نوازی (دنبال کردن)",
        description = "تمام نت‌ها با شماره‌های بزرگ و روشن نمایش داده می‌شوند تا همزمان با ساز اجرا کنید.",
        iconName = "visibility"
    ),
    RHYTHM(
        title = "Rhythm Focus",
        persianTitle = "تمرکز ریتم و ضرب‌آهنگ",
        description = "تاکید بر زمان‌بندی مترونوم و دقت ضربات. بدون حواس‌پرتی روی ریتم تمرکز کنید.",
        iconName = "timer"
    ),
    CHALLENGE(
        title = "Blind Challenge",
        persianTitle = "چالش حافظه و شنوایی",
        description = "نت بعدی تا لحظه اجرا مخفی است تا حافظه عضلانی و شنوایی شما تقویت شود.",
        iconName = "psychology"
    )
}
