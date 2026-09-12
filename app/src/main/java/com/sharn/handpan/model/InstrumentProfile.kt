package com.sharn.handpan.model

/**
 * Geometric and acoustic definition of a single Tone Field on a Handpan.
 */
data class ToneFieldDefinition(
    val internalId: Int,
    val displayNumber: Int, // 0 for Ding, 1..N for tone fields
    val pitchName: String, // e.g. "A", "Bb", "C"
    val scientificPitch: String, // e.g. "A3", "Bb3", "D3"
    val solfegeName: String, // e.g. "لا", "سی‌بمل", "ر"
    val frequencyHz: Float,
    val polarAngleDeg: Double, // Angular position relative to player (0° = 3 o'clock / East)
    val radialDistanceFraction: Float, // Distance from center (0.0 = center, 0.72 = outer ring)
    val defaultHand: PlayingHand,
    val isDing: Boolean = false,
    val sizeRelative: Float = 1.0f // Size factor (lower notes are physically larger tone fields)
)

/**
 * Complete Instrument Profile specification for extensible scale & handpan models.
 */
data class InstrumentProfile(
    val id: String,
    val name: String,
    val scaleName: String,
    val rootPitch: String,
    val tuningReferenceHz: Float = 440.0f,
    val fields: List<ToneFieldDefinition>,
    val description: String
) {
    val dingField: ToneFieldDefinition
        get() = fields.firstOrNull { it.isDing } ?: fields.first()

    val toneFields: List<ToneFieldDefinition>
        get() = fields.filter { !it.isDing }

    fun getFieldByNumber(num: Int): ToneFieldDefinition? {
        return fields.firstOrNull { it.displayNumber == num }
    }

    companion object {
        /**
         * The standard 9-Note D Kurd scale:
         * D3 Ding (Center)
         * 1: A3 (Bottom Right - 67.5°) - R
         * 2: Bb3 (Bottom Left - 112.5°) - L
         * 3: C4 (Mid Right - 22.5°) - R
         * 4: D4 (Mid Left - 157.5°) - L
         * 5: E4 (Upper Right - 337.5°) - R
         * 6: F4 (Upper Left - 202.5°) - L
         * 7: G4 (Top Right - 292.5°) - R
         * 8: A4 (Top Left - 247.5°) - L
         */
        val DEFAULT_D_KURD_9 = InstrumentProfile(
            id = "d_kurd_9",
            name = "هنگ‌درام ۹ نت دی کورد (D Kurd 9)",
            scaleName = "D Kurd",
            rootPitch = "D3",
            tuningReferenceHz = 440.0f,
            description = "ساز مرجع استاندارد ۹ نت شامل دینگ مرکزی D3 و ۸ فیلد صوتی پیرامونی در گام مینور دی کورد",
            fields = listOf(
                ToneFieldDefinition(
                    internalId = 0,
                    displayNumber = 0,
                    pitchName = "D",
                    scientificPitch = "D3",
                    solfegeName = "ر",
                    frequencyHz = 146.83f,
                    polarAngleDeg = 0.0,
                    radialDistanceFraction = 0.0f,
                    defaultHand = PlayingHand.EITHER,
                    isDing = true,
                    sizeRelative = 1.35f
                ),
                ToneFieldDefinition(
                    internalId = 1,
                    displayNumber = 1,
                    pitchName = "A",
                    scientificPitch = "A3",
                    solfegeName = "لا",
                    frequencyHz = 220.00f,
                    polarAngleDeg = 67.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.RIGHT,
                    sizeRelative = 1.15f
                ),
                ToneFieldDefinition(
                    internalId = 2,
                    displayNumber = 2,
                    pitchName = "Bb",
                    scientificPitch = "Bb3",
                    solfegeName = "سی‌بمل",
                    frequencyHz = 233.08f,
                    polarAngleDeg = 112.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.LEFT,
                    sizeRelative = 1.12f
                ),
                ToneFieldDefinition(
                    internalId = 3,
                    displayNumber = 3,
                    pitchName = "C",
                    scientificPitch = "C4",
                    solfegeName = "دو",
                    frequencyHz = 261.63f,
                    polarAngleDeg = 22.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.RIGHT,
                    sizeRelative = 1.05f
                ),
                ToneFieldDefinition(
                    internalId = 4,
                    displayNumber = 4,
                    pitchName = "D",
                    scientificPitch = "D4",
                    solfegeName = "ر",
                    frequencyHz = 293.66f,
                    polarAngleDeg = 157.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.LEFT,
                    sizeRelative = 1.00f
                ),
                ToneFieldDefinition(
                    internalId = 5,
                    displayNumber = 5,
                    pitchName = "E",
                    scientificPitch = "E4",
                    solfegeName = "می",
                    frequencyHz = 329.63f,
                    polarAngleDeg = 337.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.RIGHT,
                    sizeRelative = 0.95f
                ),
                ToneFieldDefinition(
                    internalId = 6,
                    displayNumber = 6,
                    pitchName = "F",
                    scientificPitch = "F4",
                    solfegeName = "فا",
                    frequencyHz = 349.23f,
                    polarAngleDeg = 202.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.LEFT,
                    sizeRelative = 0.90f
                ),
                ToneFieldDefinition(
                    internalId = 7,
                    displayNumber = 7,
                    pitchName = "G",
                    scientificPitch = "G4",
                    solfegeName = "سل",
                    frequencyHz = 392.00f,
                    polarAngleDeg = 292.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.RIGHT,
                    sizeRelative = 0.85f
                ),
                ToneFieldDefinition(
                    internalId = 8,
                    displayNumber = 8,
                    pitchName = "A",
                    scientificPitch = "A4",
                    solfegeName = "لا",
                    frequencyHz = 440.00f,
                    polarAngleDeg = 247.5,
                    radialDistanceFraction = 0.72f,
                    defaultHand = PlayingHand.LEFT,
                    sizeRelative = 0.80f
                )
            )
        )

        val D_CELTIC_MINOR = InstrumentProfile(
            id = "d_celtic_9",
            name = "هنگ‌درام دی سلتیک مینور (D Celtic Minor / Amara)",
            scaleName = "D Celtic Minor",
            rootPitch = "D3",
            tuningReferenceHz = 440.0f,
            description = "گام شش صدایی ملودیک و رویایی بدون درجات دیسونانس، فوق‌العاده برای بداهه‌نوازی آزاد (D3, A3, C4, D4, E4, F4, G4, A4, C5)",
            fields = listOf(
                ToneFieldDefinition(0, 0, "D", "D3", "ر", 146.83f, 0.0, 0.0f, PlayingHand.EITHER, isDing = true, sizeRelative = 1.35f),
                ToneFieldDefinition(1, 1, "A", "A3", "لا", 220.00f, 67.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.15f),
                ToneFieldDefinition(2, 2, "C", "C4", "دو", 261.63f, 112.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.10f),
                ToneFieldDefinition(3, 3, "D", "D4", "ر", 293.66f, 22.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.05f),
                ToneFieldDefinition(4, 4, "E", "E4", "می", 329.63f, 157.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.00f),
                ToneFieldDefinition(5, 5, "F", "F4", "فا", 349.23f, 337.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.95f),
                ToneFieldDefinition(6, 6, "G", "G4", "سل", 392.00f, 202.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.90f),
                ToneFieldDefinition(7, 7, "A", "A4", "لا", 440.00f, 292.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.85f),
                ToneFieldDefinition(8, 8, "C", "C5", "دو", 523.25f, 247.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.80f)
            )
        )

        val D_HIJAZ = InstrumentProfile(
            id = "d_hijaz_9",
            name = "هنگ‌درام دی حجاز (D Hijaz / Karadag)",
            scaleName = "D Hijaz",
            rootPitch = "D3",
            tuningReferenceHz = 440.0f,
            description = "مقام شرقی و کویری پرحرارت با پرش فاصله دوم افزوده (C# و Bb) برای فضای شرقی و عرفانی (D3, A3, Bb3, C#4, D4, E4, F4, G4, A4)",
            fields = listOf(
                ToneFieldDefinition(0, 0, "D", "D3", "ر", 146.83f, 0.0, 0.0f, PlayingHand.EITHER, isDing = true, sizeRelative = 1.35f),
                ToneFieldDefinition(1, 1, "A", "A3", "لا", 220.00f, 67.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.15f),
                ToneFieldDefinition(2, 2, "Bb", "Bb3", "سی‌بمل", 233.08f, 112.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.12f),
                ToneFieldDefinition(3, 3, "C#", "C#4", "دو دیز", 277.18f, 22.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.05f),
                ToneFieldDefinition(4, 4, "D", "D4", "ر", 293.66f, 157.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.00f),
                ToneFieldDefinition(5, 5, "E", "E4", "می", 329.63f, 337.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.95f),
                ToneFieldDefinition(6, 6, "F", "F4", "فا", 349.23f, 202.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.90f),
                ToneFieldDefinition(7, 7, "G", "G4", "سل", 392.00f, 292.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.85f),
                ToneFieldDefinition(8, 8, "A", "A4", "لا", 440.00f, 247.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.80f)
            )
        )

        val E_PYGMY = InstrumentProfile(
            id = "e_pygmy_9",
            name = "هنگ‌درام ای پیگمی (E Pygmy)",
            scaleName = "E Pygmy",
            rootPitch = "E3",
            tuningReferenceHz = 440.0f,
            description = "گام پنج‌صدایی هیپنوتیزمی و عمیق الهام‌گرفته از موسیقی چندصدایی آفریقای مرکزی (E3, B3, C4, E4, G4, A4, B4, D5, E5)",
            fields = listOf(
                ToneFieldDefinition(0, 0, "E", "E3", "می", 164.81f, 0.0, 0.0f, PlayingHand.EITHER, isDing = true, sizeRelative = 1.30f),
                ToneFieldDefinition(1, 1, "B", "B3", "سی", 246.94f, 67.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.15f),
                ToneFieldDefinition(2, 2, "C", "C4", "دو", 261.63f, 112.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.10f),
                ToneFieldDefinition(3, 3, "E", "E4", "می", 329.63f, 22.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.05f),
                ToneFieldDefinition(4, 4, "G", "G4", "سل", 392.00f, 157.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.00f),
                ToneFieldDefinition(5, 5, "A", "A4", "لا", 440.00f, 337.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.95f),
                ToneFieldDefinition(6, 6, "B", "B4", "سی", 493.88f, 202.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.90f),
                ToneFieldDefinition(7, 7, "D", "D5", "ر", 587.33f, 292.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.85f),
                ToneFieldDefinition(8, 8, "E", "E5", "می", 659.25f, 247.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.80f)
            )
        )

        val D_MAJOR = InstrumentProfile(
            id = "d_major_9",
            name = "هنگ‌درام دی ماژور (D Major / Sabye)",
            scaleName = "D Major",
            rootPitch = "D3",
            tuningReferenceHz = 440.0f,
            description = "گام ماژور درخشان، سرشار از امید، شادی و حس طلوع آفتاب (D3, G3, A3, B3, C#4, D4, E4, F#4, A4)",
            fields = listOf(
                ToneFieldDefinition(0, 0, "D", "D3", "ر", 146.83f, 0.0, 0.0f, PlayingHand.EITHER, isDing = true, sizeRelative = 1.35f),
                ToneFieldDefinition(1, 1, "G", "G3", "سل", 196.00f, 67.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.20f),
                ToneFieldDefinition(2, 2, "A", "A3", "لا", 220.00f, 112.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.15f),
                ToneFieldDefinition(3, 3, "B", "B3", "سی", 246.94f, 22.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.10f),
                ToneFieldDefinition(4, 4, "C#", "C#4", "دو دیز", 277.18f, 157.5, 0.72f, PlayingHand.LEFT, sizeRelative = 1.05f),
                ToneFieldDefinition(5, 5, "D", "D4", "ر", 293.66f, 337.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 1.00f),
                ToneFieldDefinition(6, 6, "E", "E4", "می", 329.63f, 202.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.95f),
                ToneFieldDefinition(7, 7, "F#", "F#4", "فا دیز", 369.99f, 292.5, 0.72f, PlayingHand.RIGHT, sizeRelative = 0.90f),
                ToneFieldDefinition(8, 8, "A", "A4", "لا", 440.00f, 247.5, 0.72f, PlayingHand.LEFT, sizeRelative = 0.85f)
            )
        )

        val STANDARD_PROFILES = listOf(
            DEFAULT_D_KURD_9,
            D_CELTIC_MINOR,
            D_HIJAZ,
            E_PYGMY,
            D_MAJOR
        )
        val ALL_PROFILES = STANDARD_PROFILES
    }
}
