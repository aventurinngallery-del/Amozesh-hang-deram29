package com.sharn.handpan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.sharn.handpan.model.HandpanNote
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGoldLight
import com.sharn.handpan.ui.theme.NoteDingColor
import com.sharn.handpan.ui.theme.NoteSlapColor
import com.sharn.handpan.ui.theme.getNoteColor
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Interactive, high-precision visual representation of a 9-Note Handpan instrument (1 Ding + 8 notes + Slap).
 */
@Composable
fun HandpanDiscView(
    activeNoteNumber: Int = -1, // -1 means none, 0 is Ding, 1..8 are notes, 9 is Slap
    onNoteTapped: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    showHandsGuide: Boolean = true,
    customSamplesMap: Map<Int, Boolean> = emptyMap()
) {
    var localTappedNote by remember { mutableIntStateOf(-1) }
    val effectiveActiveNote = if (activeNoteNumber >= 0) activeNoteNumber else localTappedNote

    val tapAnim = remember { Animatable(1f) }

    LaunchedEffect(effectiveActiveNote) {
        if (effectiveActiveNote >= 0) {
            tapAnim.snapTo(1.18f)
            tapAnim.animateTo(1.0f, tween(200))
        }
    }

    LaunchedEffect(localTappedNote) {
        if (localTappedNote >= 0) {
            delay(180)
            localTappedNote = -1
        }
    }

    Box(
        modifier = modifier
            .widthIn(max = 400.dp)
            .aspectRatio(1f)
            .padding(6.dp)
            .semantics {
                role = Role.Image
                contentDescription = "هنگ درام مجازی با دینگ، نت‌های ۱ تا ۸ و اسلپ"
                stateDescription = if (effectiveActiveNote >= 0) {
                    "نت فعال: ${if (effectiveActiveNote == 0) "دینگ" else effectiveActiveNote}"
                } else {
                    "بدون نت فعال"
                }
                customActions = listOf(
                    CustomAccessibilityAction("نواختن دینگ") {
                        onNoteTapped(NotePitchConfig.NOTE_DING)
                        true
                    },
                    *((1..8).map { noteNumber ->
                        CustomAccessibilityAction("نواختن نت $noteNumber") {
                            onNoteTapped(noteNumber)
                            true
                        }
                    }.toTypedArray()),
                    CustomAccessibilityAction("نواختن اسلپ") {
                        onNoteTapped(NotePitchConfig.NOTE_SLAP)
                        true
                    }
                )
            }
            .testTag("handpan_disc_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isInteractive) {
                    if (isInteractive) {
                        detectTapGestures { tapOffset ->
                            val tapped = findTappedNote(
                                tapOffset = tapOffset,
                                canvasWidth = size.width.toFloat(),
                                canvasHeight = size.height.toFloat()
                            )
                            if (tapped >= 0) {
                                localTappedNote = tapped
                                onNoteTapped(tapped)
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f * 0.94f

            // 1. Draw Main Metallic Shell & Dome Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3D3025),
                        Color(0xFF261D17),
                        Color(0xFF140F0C)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer Rim Metallic Highlight & Rubber Ring Edge
            drawCircle(
                color = HandpanBronze.copy(alpha = 0.55f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            drawCircle(
                color = Color(0xFF0D0A08),
                radius = outerRadius + 2.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Ding Ring / Shoulder Area (Slap zone between center and notes)
            val isSlapActive = (effectiveActiveNote == NotePitchConfig.NOTE_SLAP)
            val shoulderRadius = outerRadius * 0.44f

            drawCircle(
                color = if (isSlapActive) NoteSlapColor.copy(alpha = 0.45f) else Color(0xFF2B211A).copy(alpha = 0.35f),
                radius = shoulderRadius,
                center = center,
                style = Stroke(width = if (isSlapActive) 3.dp.toPx() else 1.5.dp.toPx())
            )

            if (isSlapActive) {
                drawCircle(
                    color = NoteSlapColor.copy(alpha = 0.25f),
                    radius = outerRadius * 0.88f,
                    center = center,
                    style = Stroke(width = 8.dp.toPx())
                )
            }

            // Draw subtle Slap indicator labels on shoulder
            drawSlapIndicators(center, shoulderRadius, isSlapActive)

            // 2. Draw Center Ding (Note 0 / D)
            val dingRadius = outerRadius * 0.26f
            val isDingActive = (effectiveActiveNote == NotePitchConfig.NOTE_DING)
            drawToneField(
                center = center,
                radius = dingRadius,
                noteNumber = 0,
                displayText = "D",
                isActive = isDingActive,
                isDing = true,
                handLabel = null,
                showHandsGuide = showHandsGuide,
                hasCustomSample = customSamplesMap[0] == true
            )

            // 3. Draw 8 Surrounding Tone Fields (Notes 1 through 8)
            val baseRimRadius = outerRadius * 0.175f
            val orbitRadius = outerRadius * 0.67f

            for (i in 0 until 8) {
                val noteNum = i + 1
                val angleDeg = HandpanNote.TONE_FIELD_ANGLES_DEG[i]
                val angleRad = Math.toRadians(angleDeg)
                val noteCenter = Offset(
                    x = center.x + (orbitRadius * cos(angleRad)).toFloat(),
                    y = center.y + (orbitRadius * sin(angleRad)).toFloat()
                )
                val isActive = (effectiveActiveNote == noteNum)
                val hand = HandpanNote.getRecommendedHand(noteNum)

                // Subtle proportional sizing: lower notes are slightly larger
                val sizeFactor = 1.05f - (i * 0.025f)
                val noteRadius = baseRimRadius * sizeFactor

                drawToneField(
                    center = noteCenter,
                    radius = noteRadius,
                    noteNumber = noteNum,
                    displayText = "$noteNum",
                    isActive = isActive,
                    isDing = false,
                    handLabel = hand,
                    showHandsGuide = showHandsGuide,
                    hasCustomSample = customSamplesMap[noteNum] == true
                )
            }
        }
    }
}

private val cachedSlapPaint = android.graphics.Paint().apply {
    isAntiAlias = true
    textSize = 18f
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
}

private val cachedTonePaint = android.graphics.Paint().apply {
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
}

private val cachedHandPaint = android.graphics.Paint().apply {
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
}

private fun DrawScope.drawSlapIndicators(center: Offset, shoulderRadius: Float, isSlapActive: Boolean) {
    cachedSlapPaint.color = if (isSlapActive) NoteSlapColor.toArgb() else Color(0x77E5A93C).toArgb()

    val slapAngles = listOf(0.0, 180.0) // Left & right shoulder markers
    for (deg in slapAngles) {
        val rad = Math.toRadians(deg)
        val pos = Offset(
            x = center.x + (shoulderRadius * cos(rad)).toFloat(),
            y = center.y + (shoulderRadius * sin(rad)).toFloat()
        )
        drawContext.canvas.nativeCanvas.drawText("S", pos.x, pos.y + 6f, cachedSlapPaint)
    }
}

private fun DrawScope.drawToneField(
    center: Offset,
    radius: Float,
    noteNumber: Int,
    displayText: String,
    isActive: Boolean,
    isDing: Boolean,
    handLabel: String?,
    showHandsGuide: Boolean = true,
    hasCustomSample: Boolean = false
) {
    val noteBaseColor = getNoteColor(noteNumber)

    if (isActive) {
        // Glowing Halo
        drawCircle(
            color = noteBaseColor.copy(alpha = 0.50f),
            radius = radius * 1.38f,
            center = center
        )
        drawCircle(
            color = HandpanGoldLight.copy(alpha = 0.35f),
            radius = radius * 1.65f,
            center = center
        )
    }

    // Tone Field Base Dimple / Surface
    val fillColors = if (isActive) {
        listOf(
            HandpanGoldLight,
            noteBaseColor,
            noteBaseColor.copy(alpha = 0.85f)
        )
    } else {
        listOf(
            if (isDing) Color(0xFF4A3A2C) else Color(0xFF2E241D),
            if (isDing) Color(0xFF2A1F16) else Color(0xFF1B140F)
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = fillColors,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Border
    drawCircle(
        color = if (isActive) Color.White else if (hasCustomSample) Color(0xFF4CAF50) else noteBaseColor.copy(alpha = 0.70f),
        radius = radius,
        center = center,
        style = Stroke(width = if (isActive) 3.5.dp.toPx() else if (hasCustomSample) 2.2.dp.toPx() else 1.8.dp.toPx())
    )

    // Authentic center dimple impression
    drawCircle(
        color = if (isActive) Color.White.copy(alpha = 0.75f) else Color(0xFF100C09),
        radius = radius * 0.30f,
        center = center
    )

    // Custom acoustic sample indicator badge (green micro dot at top of tonefield)
    if (hasCustomSample) {
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 3.5.dp.toPx(),
            center = Offset(center.x, center.y - radius * 0.68f)
        )
    }

    // Draw Main Numeric / Ding Label
    cachedTonePaint.textSize = (radius * 0.72f).coerceAtLeast(28f)
    cachedTonePaint.color = if (isActive) Color.Black.toArgb() else Color.White.toArgb()

    val yOffset = (cachedTonePaint.descent() + cachedTonePaint.ascent()) / 2
    drawContext.canvas.nativeCanvas.drawText(
        displayText,
        center.x,
        center.y - yOffset,
        cachedTonePaint
    )

    // Hand indicator (L / R) with distinct color coding: Left = Violet, Right = Cyan/Teal
    if (showHandsGuide && handLabel != null) {
        val handColor = if (handLabel == "L") Color(0xFFC084FC) else Color(0xFF38BDF8)
        cachedHandPaint.textSize = (radius * 0.36f).coerceAtLeast(18f)
        cachedHandPaint.color = if (isActive) Color(0xFF111111).toArgb() else handColor.toArgb()

        drawContext.canvas.nativeCanvas.drawText(
            handLabel,
            center.x,
            center.y + radius * 0.74f,
            cachedHandPaint
        )
    }
}

private fun findTappedNote(tapOffset: Offset, canvasWidth: Float, canvasHeight: Float): Int {
    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
    val outerRadius = minOf(canvasWidth, canvasHeight) / 2f * 0.94f

    val distToCenter = hypot(tapOffset.x - center.x, tapOffset.y - center.y)

    // Outside the instrument
    if (distToCenter > outerRadius) {
        return -1
    }

    // 1. Check Ding (Center - Note 0)
    val dingRadius = outerRadius * 0.30f
    if (distToCenter <= dingRadius) {
        return NotePitchConfig.NOTE_DING
    }

    // 2. Check surrounding 8 notes
    val rimNoteRadius = outerRadius * 0.21f
    val orbitRadius = outerRadius * 0.67f

    for (i in 0 until 8) {
        val angleRad = Math.toRadians(HandpanNote.TONE_FIELD_ANGLES_DEG[i])
        val noteCenter = Offset(
            x = center.x + (orbitRadius * cos(angleRad)).toFloat(),
            y = center.y + (orbitRadius * sin(angleRad)).toFloat()
        )
        val dist = hypot(tapOffset.x - noteCenter.x, tapOffset.y - noteCenter.y)
        if (dist <= rimNoteRadius) {
            return i + 1
        }
    }

    // 3. Tapped on shoulder / body between notes -> Slap / Tak hit (Note 9)
    return NotePitchConfig.NOTE_SLAP
}
