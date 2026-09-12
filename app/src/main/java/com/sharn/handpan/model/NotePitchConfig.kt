package com.sharn.handpan.model

/**
 * Configuration mapping numeric notes (Ding=0, 1..8 tonefields, Slap=9) to audio frequencies (Hz) and musical pitch names.
 * Represents standard handpan layouts (1 central Ding + 8 surrounding notes + Slap hit).
 */
data class NotePitchConfig(
    val scaleName: String = "D Kurd 9 (استاندارد)",
    val notePitches: Map<Int, NotePitch> = DEFAULT_D_KURD_9_PITCHES,
    val tuningReferenceHz: Float = 440.0f
) {
    val baseFrequencies: Map<Int, Float>
        get() = notePitches.mapValues { it.value.frequencyHz }

    fun getFrequency(noteNumber: Int): Float {
        val base = notePitches[noteNumber]?.frequencyHz ?: 261.63f
        return if (tuningReferenceHz != 440.0f && noteNumber != NOTE_SLAP) {
            base * (tuningReferenceHz / 440.0f)
        } else {
            base
        }
    }

    fun withTuning(refHz: Float): NotePitchConfig {
        return this.copy(tuningReferenceHz = refHz)
    }

    fun getPitchName(noteNumber: Int): String {
        return notePitches[noteNumber]?.name ?: when (noteNumber) {
            NOTE_DING -> "Ding (دینگ)"
            NOTE_SLAP -> "Slap (اسلپ)"
            else -> "نت $noteNumber"
        }
    }

    fun getSymbol(noteNumber: Int): String {
        return when (noteNumber) {
            NOTE_DING -> "D"
            NOTE_SLAP -> "S"
            in 1..8 -> "$noteNumber"
            else -> "$noteNumber"
        }
    }

    companion object {
        const val NOTE_DING = HandpanNote.DING_NUMBER
        const val NOTE_SLAP = HandpanNote.SLAP_NUMBER

        fun fromProfile(profile: InstrumentProfile): NotePitchConfig {
            val pitches = profile.fields.associate { field ->
                field.displayNumber to NotePitch(
                    number = field.displayNumber,
                    name = "${field.scientificPitch} (${field.solfegeName})",
                    frequencyHz = field.frequencyHz,
                    symbol = if (field.isDing) "D" else field.displayNumber.toString()
                )
            } + (NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S"))
            return NotePitchConfig(
                scaleName = profile.scaleName,
                notePitches = pitches,
                tuningReferenceHz = profile.tuningReferenceHz
            )
        }

        // Standard D Kurd 9: Ding (D3) + 8 surrounding notes (A3, Bb3, C4, D4, E4, F4, G4, A4)
        val DEFAULT_D_KURD_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "D3 (دینگ)", 146.83f, "D"),
            1 to NotePitch(1, "A3 (نت ۱)", 220.00f, "1"),
            2 to NotePitch(2, "Bb3 (نت ۲)", 233.08f, "2"),
            3 to NotePitch(3, "C4 (نت ۳)", 261.63f, "3"),
            4 to NotePitch(4, "D4 (نت ۴)", 293.66f, "4"),
            5 to NotePitch(5, "E4 (نت ۵)", 329.63f, "5"),
            6 to NotePitch(6, "F4 (نت ۶)", 349.23f, "6"),
            7 to NotePitch(7, "G4 (نت ۷)", 392.00f, "7"),
            8 to NotePitch(8, "A4 (نت ۸)", 440.00f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        // D Celtic Minor 9: Ding (D3) + 8 notes
        val D_CELTIC_MINOR_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "D3 (دینگ)", 146.83f, "D"),
            1 to NotePitch(1, "A3 (نت ۱)", 220.00f, "1"),
            2 to NotePitch(2, "C4 (نت ۲)", 261.63f, "2"),
            3 to NotePitch(3, "D4 (نت ۳)", 293.66f, "3"),
            4 to NotePitch(4, "E4 (نت ۴)", 329.63f, "4"),
            5 to NotePitch(5, "F4 (نت ۵)", 349.23f, "5"),
            6 to NotePitch(6, "G4 (نت ۶)", 392.00f, "6"),
            7 to NotePitch(7, "A4 (نت ۷)", 440.00f, "7"),
            8 to NotePitch(8, "C5 (نت ۸)", 523.25f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        // E Pygmy 9: Ding (E3) + 8 notes
        val E_PYGMY_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "E3 (دینگ)", 164.81f, "D"),
            1 to NotePitch(1, "G3 (نت ۱)", 196.00f, "1"),
            2 to NotePitch(2, "A3 (نت ۲)", 220.00f, "2"),
            3 to NotePitch(3, "B3 (نت ۳)", 246.94f, "3"),
            4 to NotePitch(4, "D4 (نت ۴)", 293.66f, "4"),
            5 to NotePitch(5, "E4 (نت ۵)", 329.63f, "5"),
            6 to NotePitch(6, "G4 (نت ۶)", 392.00f, "6"),
            7 to NotePitch(7, "A4 (نت ۷)", 440.00f, "7"),
            8 to NotePitch(8, "B4 (نت ۸)", 493.88f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        // C Minor Pygmy 9: Ding (C3) + 8 notes
        val C_MINOR_PYGMY_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "C3 (دینگ)", 130.81f, "D"),
            1 to NotePitch(1, "Eb3 (نت ۱)", 155.56f, "1"),
            2 to NotePitch(2, "F3 (نت ۲)", 174.61f, "2"),
            3 to NotePitch(3, "G3 (نت ۳)", 196.00f, "3"),
            4 to NotePitch(4, "Bb3 (نت ۴)", 233.08f, "4"),
            5 to NotePitch(5, "C4 (نت ۵)", 261.63f, "5"),
            6 to NotePitch(6, "Eb4 (نت ۶)", 311.13f, "6"),
            7 to NotePitch(7, "F4 (نت ۷)", 349.23f, "7"),
            8 to NotePitch(8, "G4 (نت ۸)", 392.00f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        // D Hijaz 9: Ding (D3) + 8 notes
        val D_HIJAZ_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "D3 (دینگ)", 146.83f, "D"),
            1 to NotePitch(1, "G3 (نت ۱)", 196.00f, "1"),
            2 to NotePitch(2, "A3 (نت ۲)", 220.00f, "2"),
            3 to NotePitch(3, "Bb3 (نت ۳)", 233.08f, "3"),
            4 to NotePitch(4, "C#4 (نت ۴)", 277.18f, "4"),
            5 to NotePitch(5, "D4 (نت ۵)", 293.66f, "5"),
            6 to NotePitch(6, "Eb4 (نت ۶)", 311.13f, "6"),
            7 to NotePitch(7, "F#4 (نت ۷)", 369.99f, "7"),
            8 to NotePitch(8, "G4 (نت ۸)", 392.00f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        val C_MAJOR_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "C4 (دینگ)", 261.63f, "D"),
            1 to NotePitch(1, "G4 (نت ۱)", 392.00f, "1"),
            2 to NotePitch(2, "A4 (نت ۲)", 440.00f, "2"),
            3 to NotePitch(3, "C5 (نت ۳)", 523.25f, "3"),
            4 to NotePitch(4, "D5 (نت ۴)", 587.33f, "4"),
            5 to NotePitch(5, "E5 (نت ۵)", 659.25f, "5"),
            6 to NotePitch(6, "G5 (نت ۶)", 783.99f, "6"),
            7 to NotePitch(7, "A5 (نت ۷)", 880.00f, "7"),
            8 to NotePitch(8, "C6 (نت ۸)", 1046.50f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        // D Integral 9: Ding (D3) + 8 notes
        val D_INTEGRAL_9_PITCHES = mapOf(
            NOTE_DING to NotePitch(NOTE_DING, "D3 (دینگ)", 146.83f, "D"),
            1 to NotePitch(1, "A3 (نت ۱)", 220.00f, "1"),
            2 to NotePitch(2, "Bb3 (نت ۲)", 233.08f, "2"),
            3 to NotePitch(3, "C4 (نت ۳)", 261.63f, "3"),
            4 to NotePitch(4, "D4 (نت ۴)", 293.66f, "4"),
            5 to NotePitch(5, "E4 (نت ۵)", 329.63f, "5"),
            6 to NotePitch(6, "F4 (نت ۶)", 349.23f, "6"),
            7 to NotePitch(7, "A4 (نت ۷)", 440.00f, "7"),
            8 to NotePitch(8, "D5 (نت ۸)", 587.33f, "8"),
            NOTE_SLAP to NotePitch(NOTE_SLAP, "Slap / Tak (ضربه اسلپ)", 0.0f, "S")
        )

        val D_KURD_9 = NotePitchConfig("D Kurd 9 (استاندارد)", DEFAULT_D_KURD_9_PITCHES)
        val D_CELTIC_MINOR_9 = NotePitchConfig("D Celtic Minor 9 (آمارا)", D_CELTIC_MINOR_9_PITCHES)
        val E_PYGMY_9 = NotePitchConfig("E Pygmy 9 (آرامش‌بخش)", E_PYGMY_9_PITCHES)
        val C_MINOR_PYGMY_9 = NotePitchConfig("C Minor Pygmy 9", C_MINOR_PYGMY_9_PITCHES)
        val D_HIJAZ_9 = NotePitchConfig("D Hijaz 9 (شرقی / قره‌داغ)", D_HIJAZ_9_PITCHES)
        val D_INTEGRAL_9 = NotePitchConfig("D Integral 9 (عمیق و ژرف)", D_INTEGRAL_9_PITCHES)
        val C_MAJOR_9 = NotePitchConfig("C Major 9 (ماژور)", C_MAJOR_9_PITCHES)

        val SCALES = listOf(
            D_KURD_9,
            D_CELTIC_MINOR_9,
            E_PYGMY_9,
            C_MINOR_PYGMY_9,
            D_HIJAZ_9,
            D_INTEGRAL_9,
            C_MAJOR_9
        )
    }
}

data class NotePitch(
    val number: Int,
    val name: String,
    val frequencyHz: Float,
    val symbol: String = "$number"
)
