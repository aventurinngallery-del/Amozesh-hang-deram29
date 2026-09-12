package com.sharn.handpan.model

/**
 * Advanced, decoupled representation of a musical event in a Handpan pattern.
 * Supports tone fields, Ding dome/shoulder, percussive slaps/taks, ghost notes, palm mutes,
 * playing hands (L/R), playing fingers, and precise velocity dynamics.
 */
data class MusicEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetNumber: Int, // 0 for Ding, 1..8 for tone fields, 9 for Slap
    val beatPosition: Double,
    val duration: Double = 1.0,
    val velocity: Float = 0.85f,
    val accent: Boolean = false,
    val technique: HandpanTechnique = if (targetNumber == 9) HandpanTechnique.SLAP else if (targetNumber == 0) HandpanTechnique.DING else HandpanTechnique.TONE,
    val targetZone: PercussionZone = technique.targetZone,
    val hand: PlayingHand = if (targetNumber % 2 != 0 && targetNumber in 1..8) PlayingHand.RIGHT else if (targetNumber in 1..8) PlayingHand.LEFT else PlayingHand.EITHER,
    val finger: PlayingFinger = PlayingFinger.UNSPECIFIED
) {
    val isRest: Boolean get() = (technique == HandpanTechnique.REST)
    val isDing: Boolean get() = (technique == HandpanTechnique.DING || targetNumber == 0) && !isRest
    val isSlap: Boolean get() = (technique == HandpanTechnique.SLAP || targetNumber == 9) && !isRest

    fun toLegacyNoteEvent(): NoteEvent {
        return NoteEvent(
            noteNumber = targetNumber,
            beatPosition = beatPosition,
            duration = duration,
            velocity = velocity,
            accent = accent,
            isRest = isRest,
            hand = hand.symbol
        )
    }

    companion object {
        fun fromLegacyNoteEvent(noteEvent: NoteEvent): MusicEvent {
            val tech = when {
                noteEvent.isRest -> HandpanTechnique.REST
                noteEvent.noteNumber == 9 -> HandpanTechnique.SLAP
                noteEvent.noteNumber == 0 -> HandpanTechnique.DING
                else -> HandpanTechnique.TONE
            }
            val handEnum = when (noteEvent.hand) {
                "R" -> PlayingHand.RIGHT
                "L" -> PlayingHand.LEFT
                else -> if (noteEvent.noteNumber % 2 != 0 && noteEvent.noteNumber in 1..8) PlayingHand.RIGHT else if (noteEvent.noteNumber in 1..8) PlayingHand.LEFT else PlayingHand.EITHER
            }
            return MusicEvent(
                targetNumber = noteEvent.noteNumber,
                beatPosition = noteEvent.beatPosition,
                duration = noteEvent.duration,
                velocity = noteEvent.velocity,
                accent = noteEvent.accent,
                technique = tech,
                hand = handEnum
            )
        }
    }
}
