package com.sharn.handpan

import com.sharn.handpan.model.HandpanNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

class HandpanNotePositioningAndHandTest {

    @Test
    fun testOddNotesAreOnTheRightSide() {
        val oddNotes = listOf(1, 3, 5, 7)
        for (noteNum in oddNotes) {
            val angleDeg = HandpanNote.TONE_FIELD_ANGLES_DEG[noteNum - 1]
            val rad = Math.toRadians(angleDeg)
            val xOffset = cos(rad)

            // xOffset > 0 means the note is positioned on the right hemisphere
            assertTrue("Note $noteNum with angle $angleDeg° must be on the right (x > 0)", xOffset > 0.0)
            assertEquals("Note $noteNum must recommend right hand (R)", "R", HandpanNote.getRecommendedHand(noteNum))
        }
    }

    @Test
    fun testEvenNotesAreOnTheLeftSide() {
        val evenNotes = listOf(2, 4, 6, 8)
        for (noteNum in evenNotes) {
            val angleDeg = HandpanNote.TONE_FIELD_ANGLES_DEG[noteNum - 1]
            val rad = Math.toRadians(angleDeg)
            val xOffset = cos(rad)

            // xOffset < 0 means the note is positioned on the left hemisphere
            assertTrue("Note $noteNum with angle $angleDeg° must be on the left (x < 0)", xOffset < 0.0)
            assertEquals("Note $noteNum must recommend left hand (L)", "L", HandpanNote.getRecommendedHand(noteNum))
        }
    }
}
