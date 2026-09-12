package com.sharn.handpan.ui.theme

import androidx.compose.ui.graphics.Color

// Primary & Brand Palette (Bronze, Gold, Wood, Charcoal)
val HandpanGold = Color(0xFFE5A93C)
val HandpanGoldLight = Color(0xFFFFD166)
val HandpanBronze = Color(0xFFC59B6D)
val HandpanBronzeDark = Color(0xFF8C5E3C)
val HandpanTerracotta = Color(0xFFD96B43)
val HandpanCopper = Color(0xFFB85D36)

// Dark Theme Surfaces
val CharcoalBlack = Color(0xFF100E0C)
val CharcoalDark = Color(0xFF181411)
val CharcoalSurface = Color(0xFF221C17)
val CharcoalSurfaceVariant = Color(0xFF2D2620)
val CharcoalBorder = Color(0xFF3E342B)

// Light Theme Surfaces
val WoodWarmLight = Color(0xFFFFFBF7)
val WoodSurfaceLight = Color(0xFFF7EFE6)
val WoodSurfaceVariantLight = Color(0xFFECE1D4)
val WoodBorderLight = Color(0xFFD8C7B5)

// Note Colors (0 for Ding, 1 to 8 for surrounding tonefields, 9 for Slap/Tak)
val NoteDingColor = Color(0xFFFFB703)  // Center Ding (Gold/Amber)
val Note1Color = Color(0xFFFB8500)      // Amber Orange (Note 1 - A3)
val Note2Color = Color(0xFFE76F51)      // Coral Red (Note 2 - Bb3)
val Note3Color = Color(0xFFF4A261)      // Sandy Ochre (Note 3 - C4)
val Note4Color = Color(0xFFE9C46A)      // Golden Sand (Note 4 - D4)
val Note5Color = Color(0xFF2A9D8F)      // Patina Teal (Note 5 - E4)
val Note6Color = Color(0xFF457B9D)      // Steel Slate (Note 6 - F4)
val Note7Color = Color(0xFF8338EC)      // Velvet Violet (Note 7 - G4)
val Note8Color = Color(0xFF3A86FF)      // Deep Sky Blue (Note 8 - A4)
val NoteSlapColor = Color(0xFFFF4081)   // Percussive Slap/Tak (Rose Pink)

// Functional Metronome & Indicator Colors
val BeatDownbeatColor = Color(0xFFFF5252) // Downbeat red/amber
val BeatRegularColor = Color(0xFFFFD166)  // Regular beat gold
val RestColor = Color(0xFF757575)        // Rest gray
val AccentGlowColor = Color(0xFFFFE066)  // Accent glow

fun getNoteColor(noteNumber: Int): Color {
    return when (noteNumber) {
        0 -> NoteDingColor      // Ding (D)
        1 -> Note1Color          // Note 1
        2 -> Note2Color          // Note 2
        3 -> Note3Color          // Note 3
        4 -> Note4Color          // Note 4
        5 -> Note5Color          // Note 5
        6 -> Note6Color          // Note 6
        7 -> Note7Color          // Note 7
        8 -> Note8Color          // Note 8
        9 -> NoteSlapColor       // Slap (S)
        else -> HandpanGold
    }
}
