package com.sharn.handpan.model

/**
 * Technique definition detailing performance mechanics and physical interaction on the handpan.
 */
enum class HandpanTechnique(
    val id: String,
    val persianName: String,
    val englishName: String,
    val notationSymbol: String,
    val description: String,
    val targetZone: PercussionZone
) {
    DING("ding", "ضربه دینگ مرکزی", "Ding Strike", "D", "ضربه پرطنین به گنبد یا ناحیه مرکزی", PercussionZone.DING_DOME),
    TONE("tone", "ضربه نت ملودیک", "Tone Field Strike", "T", "ضربه دقیق به فیلدهای صوتی اطراف ساز", PercussionZone.TONE_FIELD),
    TAK("tak", "ضربه تق / تاک", "Tak / Shoulder Strike", "Tak", "ضربه پرکاسیو روی شانه یا بدنه فوقانی", PercussionZone.DING_SHOULDER),
    SLAP("slap", "ضربه اسلپ بدنه", "Slap Strike", "S", "ضربه کوبه‌ای دست روی فضای بین نت‌ها (Interstitial)", PercussionZone.INTERSTITIAL),
    GHOST_NOTE("ghost", "نت شبح / ضربه نرم", "Ghost Note", "(•)", "ضربه بسیار آرام و بدون طنین برای پر کردن ریتم", PercussionZone.TONE_FIELD),
    PALM_MUTE("mute", "ضربه خفه / میوت", "Palm Mute", "x", "ضربه با فرود آمدن نرم کف دست جهت قطع طنین", PercussionZone.TONE_FIELD),
    KNOCK("knock", "ضربه بند انگشت", "Knuckle Knock", "K", "ضربه کوبه‌ای خشک با بند انگشت روی بدنه فلزی", PercussionZone.OUTER_BODY),
    FIST_STROKE("fist", "ضربه مشت نرم", "Fist Tap", "F", "ضربه نرم با بخش نرم مشت برای ایجاد صدای بم کوبه‌ای", PercussionZone.OUTER_BODY),
    SINGING_DING("singing", "مالش دوار دینگ", "Singing Ding", "~", "سایش دورانی انگشت روی دینگ جهت ایجاد رزونانس ممتد", PercussionZone.DING_DOME),
    REST("rest", "سکوت ریتمیک", "Rest", "𝄽", "سکوت به ارزش زمانی مشخص بدون تولید صدا", PercussionZone.NONE)
}

/**
 * Percussion and acoustic impact zones across the handpan physical body.
 */
enum class PercussionZone(val persianName: String) {
    DING_DOME("گنبد مرکزی دینگ (Dome)"),
    DING_SHOULDER("شانه دینگ (Ding Shoulder)"),
    DING_FLAT("حاشیه مسطح دینگ"),
    TONE_FIELD("فیلد صوتی نت"),
    INTERSTITIAL("فضای بین نت‌ها (Interstitial)"),
    OUTER_BODY("بدنه جانبی فلزی"),
    RIM("لبه پیرامونی ساز (Rim)"),
    NONE("نامشخص / سکوت")
}

/**
 * Playing hand articulation.
 */
enum class PlayingHand(val symbol: String, val persianName: String) {
    RIGHT("R", "دست راست"),
    LEFT("L", "دست چپ"),
    EITHER("E", "هر دو یا اختیاری")
}

/**
 * Playing finger designation for advanced technique mapping.
 */
enum class PlayingFinger(val symbol: String, val persianName: String) {
    THUMB("T", "انگشت شست"),
    INDEX("I", "انگشت اشاره"),
    MIDDLE("M", "انگشت وسط"),
    RING("R", "انگشت حلقه"),
    LITTLE("P", "انگشت کوچک"),
    PALM("Palm", "کف دست"),
    KNUCKLE("Knuckle", "بند انگشت"),
    UNSPECIFIED("-", "نامشخص")
}
