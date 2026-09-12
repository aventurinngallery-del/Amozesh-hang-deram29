package com.sharn.handpan.data.builtin

import com.sharn.handpan.model.DifficultyLevel
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.model.PatternCategory
import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimeSignature

/**
 * Built-in curriculum of authentic Handpan exercises (9-Note D Kurd scale).
 * Note 0 = Center Ding (D)
 * Right Side (R): Notes 1, 3, 5, 7
 * Left Side (L): Notes 2, 4, 6, 8
 * Note 9 = Slap / Tak strike (S)
 */
object BuiltinExercises {

    val ALL_BUILTIN_PATTERNS: List<HandpanPattern> = listOf(
        // ==================== BEGINNER (مبتدی و پایه) ====================
        HandpanPattern(
            id = "beg_01_single_ding",
            title = "تمرین ۱: ضربه به نت مرکز (دینگ)",
            description = "آشنایی با صدای طنین‌انداز دینگ (D). ۴ ضربه آرام و یکنواخت با انگشت شست یا بند انگشت وسط.",
            bpm = 60,
            timeSignature = TimeSignature.Common44,
            bars = 1,
            difficulty = DifficultyLevel.BEGINNER,
            category = PatternCategory.BEGINNER,
            recommendedSubdivision = Subdivision.QUARTER,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 1.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 2.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 3.0, accent = false, hand = "L")
            )
        ),
        HandpanPattern(
            id = "beg_02_ding_and_one",
            title = "تمرین ۲: دینگ و اولین نت (D - 1)",
            description = "حرکت دست بین دینگ مرکزی (D) و نت بم اول در سمت راست (نت ۱). ایجاد پیوستگی و تعادل بین دست‌ها.",
            bpm = 60,
            timeSignature = TimeSignature.Common44,
            bars = 1,
            difficulty = DifficultyLevel.BEGINNER,
            category = PatternCategory.BEGINNER,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 2.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 3.0, accent = false, hand = "R")
            )
        ),
        HandpanPattern(
            id = "beg_03_triad_arpeggio",
            title = "تمرین ۳: آرپژ سه‌تایی هارمونیک (D - 1 - 2 - 1)",
            description = "الگوی بنیادین و گوش‌نواز هنگ‌درام با حرکت متناوب دست راست روی نت ۱ و دست چپ روی نت ۲.",
            bpm = 65,
            timeSignature = TimeSignature.Common44,
            bars = 1,
            difficulty = DifficultyLevel.BEGINNER,
            category = PatternCategory.BEGINNER,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 2.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 3.0, accent = false, hand = "R")
            )
        ),
        HandpanPattern(
            id = "beg_04_intro_to_slap",
            title = "تمرین ۴: آشنایی با ضربه اسلپ (D - 1 - S - 2)",
            description = "اجرای تکنیک اسلپ (S) روی بدنه برای خلق ریتم کوبه‌ای و پرانرژی هندپن.",
            bpm = 70,
            timeSignature = TimeSignature.Common44,
            bars = 1,
            difficulty = DifficultyLevel.BEGINNER,
            category = PatternCategory.BEGINNER,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 2.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 3.0, accent = false, hand = "L")
            )
        ),
        HandpanPattern(
            id = "beg_05_scale_eight_notes",
            title = "تمرین ۵: پیمایش کامل گام ۸ نت (D تا ۸)",
            description = "نواختن متوالی دینگ و تمام ۸ نت دور ساز به صورت زیگزاگی متناوب (راست: ۱، چپ: ۲، راست: ۳، چپ: ۴ و...).",
            bpm = 60,
            timeSignature = TimeSignature(9, 8),
            bars = 1,
            difficulty = DifficultyLevel.INTERMEDIATE,
            category = PatternCategory.BEGINNER,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 2.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 3, beatPosition = 3.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 4, beatPosition = 4.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 5, beatPosition = 5.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 6, beatPosition = 6.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 7, beatPosition = 7.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 8, beatPosition = 8.0, accent = false, hand = "L")
            )
        ),
        HandpanPattern(
            id = "beg_06_ping_pong",
            title = "تمرین ۶: پینگ‌پنگ دینگ به نت‌های کناری",
            description = "بازگشت مداوم به دینگ پس از هر نت کناری (D-1-D-2-D-3-D-4).",
            bpm = 72,
            timeSignature = TimeSignature.Common44,
            bars = 2,
            difficulty = DifficultyLevel.INTERMEDIATE,
            category = PatternCategory.BEGINNER,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 2.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 3.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 4.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 3, beatPosition = 5.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 6.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 4, beatPosition = 7.0, accent = false, hand = "L")
            )
        ),

        // ==================== RHYTHM (ریتم و تکنیک اسلپ) ====================
        HandpanPattern(
            id = "rhy_01_funk_slap_groove",
            title = "ریتم ۱: شیار اسلپ و دینگ (D - 1 - S - 2)",
            description = "ریتم کلاسیک و پرطرفدار هنگدرام: دینگ پایه، نت ملودی، اسلپ محکم و نت فرود.",
            bpm = 75,
            timeSignature = TimeSignature.Common44,
            bars = 2,
            difficulty = DifficultyLevel.INTERMEDIATE,
            category = PatternCategory.RHYTHM,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 2.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 3.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 4.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 3, beatPosition = 5.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 6.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 4, beatPosition = 7.0, accent = false, hand = "L")
            )
        ),
        HandpanPattern(
            id = "rhy_02_waltz_3_4",
            title = "ریتم ۲: والس آرام سه‌ضربی (۳/۴)",
            description = "ریتم دلنشین سه‌ضربی: ضرب اول قوی دینگ و دو ضرب سبک کناری (D - 1 - 2).",
            bpm = 85,
            timeSignature = TimeSignature.Waltz34,
            bars = 2,
            difficulty = DifficultyLevel.BEGINNER,
            category = PatternCategory.RHYTHM,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 2.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 3.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 3, beatPosition = 4.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 4, beatPosition = 5.0, accent = false, hand = "L")
            )
        ),
        HandpanPattern(
            id = "rhy_03_six_eight_groove",
            title = "ریتم ۳: ریتم شاد ۶/۸ ایرانی",
            description = "ریتم آشنا و پرانرژی ۶/۸ محلی با چیدمان ضربات متوالی راست و چپ و اسلپ میانی.",
            bpm = 100,
            timeSignature = TimeSignature.SixEight68,
            bars = 2,
            difficulty = DifficultyLevel.ADVANCED,
            category = PatternCategory.RHYTHM,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 2.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 3.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 3, beatPosition = 4.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 4, beatPosition = 5.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 6.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 7.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 8.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 9.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 5, beatPosition = 10.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 6, beatPosition = 11.0, accent = false, hand = "L")
            )
        ),

        // ==================== INDEPENDENCE (استقلال دست‌ها) ====================
        HandpanPattern(
            id = "ind_01_hand_alternation",
            title = "استقلال ۱: تمرین متناوب دست‌ها",
            description = "ملودی آرامش‌بخش با تمپوی ۵۰ BPM برای تنفس عمیق و تسلط بر دست‌ها با هارمونی دینگ و نت‌های زوج و فرد.",
            bpm = 50,
            timeSignature = TimeSignature.Common44,
            bars = 2,
            difficulty = DifficultyLevel.BEGINNER,
            category = PatternCategory.INDEPENDENCE,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 3, beatPosition = 2.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 5, beatPosition = 3.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 4.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 5.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 4, beatPosition = 6.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 6, beatPosition = 7.0, accent = false, hand = "L")
            )
        ),
        HandpanPattern(
            id = "ind_02_paradiddle",
            title = "استقلال ۲: الگوی پارادیدل (R-L-R-R / L-R-L-L)",
            description = "حرکت معلق بین نت‌های ۵، ۳، ۱ و دینگ در فضایی ژرف و متقارن.",
            bpm = 55,
            timeSignature = TimeSignature.Common44,
            bars = 2,
            difficulty = DifficultyLevel.INTERMEDIATE,
            category = PatternCategory.INDEPENDENCE,
            events = listOf(
                NoteEvent(noteNumber = 5, beatPosition = 0.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 3, beatPosition = 1.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = 1, beatPosition = 2.0, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 3.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 6, beatPosition = 4.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 4, beatPosition = 5.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 2, beatPosition = 6.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 7.0, accent = true, hand = "R")
            )
        ),

        // ==================== MELODY (ملودی و قطعات پیشرفته) ====================
        HandpanPattern(
            id = "mel_01_syncopated_speed",
            title = "ملودی ۱: سنکوپ سریع و پاساژ دوطرفه",
            description = "ترکیب ضربات ضدضرب و انتقال سریع میان فیلدهای صوتی سمت راست و چپ.",
            bpm = 95,
            timeSignature = TimeSignature.Common44,
            bars = 2,
            difficulty = DifficultyLevel.ADVANCED,
            category = PatternCategory.MELODY,
            events = listOf(
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 1, beatPosition = 0.5, accent = false, hand = "R"),
                NoteEvent(noteNumber = 2, beatPosition = 1.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 2.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 3, beatPosition = 2.5, accent = false, hand = "R"),
                NoteEvent(noteNumber = 4, beatPosition = 3.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = 5, beatPosition = 3.5, accent = false, hand = "R"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 4.0, accent = true, hand = "L"),
                NoteEvent(noteNumber = 6, beatPosition = 5.0, accent = false, hand = "L"),
                NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 6.0, accent = true, hand = "R"),
                NoteEvent(noteNumber = 7, beatPosition = 7.0, accent = false, hand = "R")
            )
        )
    )
}
