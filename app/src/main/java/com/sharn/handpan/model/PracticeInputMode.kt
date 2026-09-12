package com.sharn.handpan.model

/**
 * Fundamental input mode for the Handpan learning engine.
 *
 * REAL_HANDPAN: Primary mode. The user strikes their real physical Handpan.
 * Audio is captured via the microphone, analyzed via the YIN pitch & onset engine,
 * evaluated against target notes and timing, and educational feedback is generated.
 * Virtual synthesizer sounds are MUTED to avoid audio feedback loop into the microphone.
 *
 * VIRTUAL_HANDPAN: Secondary fallback mode for practicing on-the-go or without an instrument.
 * Touches on the virtual disc trigger sound synthesis and haptics.
 */
enum class PracticeInputMode(
    val title: String,
    val persianTitle: String,
    val description: String,
    val persianDescription: String
) {
    REAL_HANDPAN(
        title = "Real Handpan",
        persianTitle = "هندپن واقعی (پیشنهادی)",
        description = "Play on your physical Handpan. The microphone detects your pitch, hit timing, and dynamics.",
        persianDescription = "هندپن واقعی خود را بنوازید. میکروفون صدای ساز را می‌شنود و دقت نت و زمان‌بندی ضربات شما را ارزیابی می‌کند."
    ),
    VIRTUAL_HANDPAN(
        title = "Virtual Handpan",
        persianTitle = "هندپن مجازی (حالت کمکی)",
        description = "Practice without an instrument on the screen using touch and audio synthesis.",
        persianDescription = "تمرین بدون ساز روی صفحه لمسی با پخش صدای سنتز شده هندپن."
    )
}
