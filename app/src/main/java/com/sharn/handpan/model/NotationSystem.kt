package com.sharn.handpan.model

/**
 * Supported notation rendering paradigms for musical events.
 */
enum class NotationSystem(val persianTitle: String, val englishTitle: String) {
    NUMERIC("نت‌نویسی عددی (D, 1..8)", "Numeric (D, 1..8)"),
    LETTER("نت‌نویسی الفبایی غربی (D3, A3..)", "Letter Pitch (D3, A3..)"),
    LETTER_SIMPLE("نام ساده نت بدون اکتاو (D, A, Bb..)", "Simple Letter (D, A..)"),
    SOLFEGE("سلفژ کلاسیک (ر، لا، سی‌بمل..)", "Solfege (Re, La, Sib..)")
}

/**
 * Strategy for rendering a MusicEvent into a target notation system string.
 */
object NotationRenderer {

    fun render(
        noteNumber: Int,
        technique: HandpanTechnique = HandpanTechnique.TONE,
        instrumentProfile: InstrumentProfile = InstrumentProfile.DEFAULT_D_KURD_9,
        system: NotationSystem = NotationSystem.NUMERIC
    ): String {
        if (technique == HandpanTechnique.REST) return "𝄽"
        if (technique == HandpanTechnique.SLAP) return "S"
        if (technique == HandpanTechnique.TAK) return "Tak"

        val field = instrumentProfile.getFieldByNumber(noteNumber)
            ?: return if (noteNumber == 0) "D" else "$noteNumber"

        return when (system) {
            NotationSystem.NUMERIC -> {
                if (field.isDing) "D" else field.displayNumber.toString()
            }
            NotationSystem.LETTER -> field.scientificPitch
            NotationSystem.LETTER_SIMPLE -> field.pitchName
            NotationSystem.SOLFEGE -> field.solfegeName
        }
    }
}
