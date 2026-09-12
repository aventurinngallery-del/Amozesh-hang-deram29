package com.sharn.handpan.model

/**
 * Type of Handpan sound or articulation.
 */
enum class HandpanNoteType {
    /** Central deep bass root note (Ding) - usually D3 on D Kurd */
    DING,

    /** Melodic perimeter tone fields (numbered 1 through 8 in ascending pitch) */
    TONE_FIELD,

    /** Percussive slap or tak hit on the interstitial space / shoulder */
    SLAP,

    /** Rhythmic silence */
    REST
}

/**
 * Core domain representation of a Handpan note/tonefield definition.
 * Backed dynamically by InstrumentProfile.
 */
data class HandpanNote(
    val number: Int,
    val type: HandpanNoteType,
    val symbol: String,
    val pitchName: String,
    val frequencyHz: Float,
    val persianName: String,
    val hand: String? = null,
    val polarAngleDeg: Double = 0.0,
    val technique: HandpanTechnique = HandpanTechnique.TONE,
    val scientificPitch: String = pitchName
) {
    val isDing: Boolean get() = (type == HandpanNoteType.DING || number == DING_NUMBER)
    val isSlap: Boolean get() = (type == HandpanNoteType.SLAP || number == SLAP_NUMBER)
    val isToneField: Boolean get() = (type == HandpanNoteType.TONE_FIELD && number in 1..8)

    companion object {
        const val DING_NUMBER = 0
        const val SLAP_NUMBER = 9

        /**
         * Standard polar angles (degrees) for the 8 surrounding tone fields:
         * Derived directly from InstrumentProfile.DEFAULT_D_KURD_9.
         */
        val TONE_FIELD_ANGLES_DEG = InstrumentProfile.DEFAULT_D_KURD_9.toneFields.map { it.polarAngleDeg }

        /**
         * Default hand mapping:
         * Right hand (R) = odd notes (1, 3, 5, 7) on the Right side
         * Left hand (L) = even notes (2, 4, 6, 8) on the Left side
         */
        fun getRecommendedHand(noteNumber: Int): String? {
            return when {
                noteNumber == DING_NUMBER -> null
                noteNumber == SLAP_NUMBER -> null
                noteNumber % 2 != 0 -> "R"
                else -> "L"
            }
        }

        /**
         * Returns standard display symbol for any note index.
         */
        fun getDisplaySymbol(
            noteNumber: Int,
            isRest: Boolean = false,
            system: NotationSystem = NotationSystem.NUMERIC
        ): String {
            if (isRest) return "𝄽"
            return NotationRenderer.render(
                noteNumber = noteNumber,
                technique = if (noteNumber == SLAP_NUMBER) HandpanTechnique.SLAP else HandpanTechnique.TONE,
                system = system
            )
        }

        /**
         * Returns Persian descriptive title for note index.
         */
        fun getPersianLabel(noteNumber: Int, isRest: Boolean = false): String {
            return when {
                isRest -> "سکوت"
                noteNumber == DING_NUMBER -> "دینگ مرکز (D)"
                noteNumber == SLAP_NUMBER -> "ضربه اسلپ (S)"
                noteNumber in 1..8 -> "نت $noteNumber"
                else -> "نت $noteNumber"
            }
        }
    }
}
