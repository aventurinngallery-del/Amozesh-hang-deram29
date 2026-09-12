package com.sharn.handpan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharn.handpan.model.DifficultyLevel
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.model.NotationRenderer
import com.sharn.handpan.model.PatternCategory
import com.sharn.handpan.model.TimeSignature
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight
import com.sharn.handpan.ui.theme.HandpanTerracotta
import com.sharn.handpan.ui.theme.RestColor
import com.sharn.handpan.ui.theme.getNoteColor
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PatternEditorScreen(
    viewModel: HandpanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appState by viewModel.appUiState.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("الگوی شخصی جدید") }
    var bpm by remember { mutableIntStateOf(70) }
    var selectedTimeSignature by remember { mutableStateOf(TimeSignature.Common44) }
    var bars by remember { mutableIntStateOf(1) }

    // List of notes created
    val noteEvents = remember {
        mutableStateListOf(
            NoteEvent(noteNumber = NotePitchConfig.NOTE_DING, beatPosition = 0.0, accent = true),
            NoteEvent(noteNumber = 1, beatPosition = 1.0, accent = false),
            NoteEvent(noteNumber = NotePitchConfig.NOTE_SLAP, beatPosition = 2.0, accent = false),
            NoteEvent(noteNumber = 1, beatPosition = 3.0, accent = false)
        )
    }

    var isNextAccent by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val currentPreviewJob by rememberUpdatedState(previewJob)
    val previewScope = rememberCoroutineScope()

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        isPreviewPlaying = false
    }

    fun startPreview() {
        if (noteEvents.isEmpty()) return
        stopPreview()
        val events = noteEvents.sortedBy { it.beatPosition }
        previewJob = previewScope.launch {
            isPreviewPlaying = true
            try {
                var previousBeat = 0.0
                events.forEach { event ->
                    val waitMs = ((event.beatPosition - previousBeat) * 60_000.0 / bpm)
                        .toLong()
                        .coerceAtLeast(0L)
                    delay(waitMs)
                    if (!event.isRest) {
                        viewModel.audioEngine.playNote(event.noteNumber, event.accent, event.velocity)
                    }
                    previousBeat = event.beatPosition
                }
            } finally {
                isPreviewPlaying = false
                previewJob = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { currentPreviewJob?.cancel() }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(horizontal = 16.dp)
            .testTag("pattern_editor_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }

            Text(
                text = "ساخت الگوی شخصی",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Save Action
            IconButton(
                onClick = {
                    if (noteEvents.isNotEmpty()) {
                        val newPattern = HandpanPattern(
                            id = "custom_" + UUID.randomUUID().toString().take(8),
                            title = title.ifBlank { "الگوی بدون عنوان" },
                            description = "الگوی سفارشی ساخته‌شده توسط کاربر",
                            bpm = bpm,
                            timeSignature = selectedTimeSignature,
                            bars = bars,
                            events = noteEvents.toList(),
                            difficulty = DifficultyLevel.BEGINNER,
                            category = PatternCategory.CUSTOM,
                            isCustom = true
                        )
                        viewModel.saveCustomPattern(newPattern)
                    }
                },
                modifier = Modifier.testTag("editor_save_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "ذخیره", tint = HandpanGold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pattern Name Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("نام الگو") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HandpanGold,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = HandpanGold,
                    unfocusedLabelColor = HandpanBronze,
                    cursorColor = HandpanGold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pattern_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // BPM & Time Signature Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("سرعت: $bpm BPM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("میزان‌نما: ${selectedTimeSignature.displayName}", color = HandpanBronze, fontSize = 13.sp)
                    }

                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { bpm = it.toInt() },
                        valueRange = 40f..200f,
                        steps = 159,
                        colors = SliderDefaults.colors(
                            thumbColor = HandpanGold,
                            activeTrackColor = HandpanGold,
                            inactiveTrackColor = CharcoalBorder
                        )
                    )
                }
            }

            // Sequence Preview Visualizer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "توالی نت‌ها (${noteEvents.size} نت):",
                            style = MaterialTheme.typography.titleSmall,
                            color = HandpanGold,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (noteEvents.isEmpty()) "هیچ نتی افزوده نشده" else noteEvents.joinToString(" - ") {
                                val symbol = NotationRenderer.render(
                                    noteNumber = it.noteNumber,
                                    technique = it.technique,
                                    system = appState.preferredNotationSystem
                                )
                                if (it.isRest) "𝄽" else if (it.accent) "[$symbol]" else symbol
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )

                        Button(
                            onClick = { if (isPreviewPlaying) stopPreview() else startPreview() },
                            enabled = noteEvents.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPreviewPlaying) HandpanTerracotta else HandpanGold
                            ),
                            modifier = Modifier.testTag("editor_preview_button")
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CharcoalBlack
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPreviewPlaying) "توقف" else "پیش‌نمایش",
                                color = CharcoalBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal scrolling list of note bubbles
                    if (noteEvents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("از صفحه‌کلید زیر برای افزودن نت استفاده کنید", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(noteEvents) { index, event ->
                                val noteColor = if (event.isRest) RestColor else getNoteColor(event.noteNumber)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (event.isRest) CharcoalSurfaceVariant else noteColor)
                                        .border(
                                            1.dp,
                                            if (event.accent) HandpanGoldLight else CharcoalBorder,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (event.accent) {
                                            Text("▲", fontSize = 8.sp, color = HandpanGoldLight)
                                        }
                                        Text(
                                            text = NotationRenderer.render(
                                                noteNumber = event.noteNumber,
                                                technique = event.technique,
                                                system = appState.preferredNotationSystem
                                            ),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (event.isRest) Color.LightGray else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Numeric Keypad for Note Entry
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, HandpanBronze.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "صفحه‌کلید نت‌ها (Numeric Keypad):",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    // Row 0: Ding (D) and Slap (S) prominent buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumericPadKey(
                            label = "D",
                            subLabel = "دینگ (Ding)",
                            isSpecial = true,
                            color = getNoteColor(NotePitchConfig.NOTE_DING),
                            modifier = Modifier.weight(1.2f),
                            onClick = {
                                val nextPos = noteEvents.size.toDouble()
                                noteEvents.add(
                                    NoteEvent(
                                        noteNumber = NotePitchConfig.NOTE_DING,
                                        beatPosition = nextPos,
                                        accent = isNextAccent
                                    )
                                )
                                viewModel.playNoteDirect(NotePitchConfig.NOTE_DING, accent = isNextAccent)
                                isNextAccent = false
                            }
                        )

                        NumericPadKey(
                            label = "S",
                            subLabel = "اسلپ (Slap / Tak)",
                            isSpecial = true,
                            color = getNoteColor(NotePitchConfig.NOTE_SLAP),
                            modifier = Modifier.weight(1.2f),
                            onClick = {
                                val nextPos = noteEvents.size.toDouble()
                                noteEvents.add(
                                    NoteEvent(
                                        noteNumber = NotePitchConfig.NOTE_SLAP,
                                        beatPosition = nextPos,
                                        accent = isNextAccent
                                    )
                                )
                                viewModel.playNoteDirect(NotePitchConfig.NOTE_SLAP, accent = isNextAccent)
                                isNextAccent = false
                            }
                        )
                    }

                    // Row 1: Notes 1, 2, 3, 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (note in 1..4) {
                            NumericPadKey(
                                label = "$note",
                                subLabel = if (note % 2 != 0) "چپ (L)" else "راست (R)",
                                isSpecial = false,
                                color = getNoteColor(note),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val nextPos = noteEvents.size.toDouble()
                                    noteEvents.add(
                                        NoteEvent(
                                            noteNumber = note,
                                            beatPosition = nextPos,
                                            accent = isNextAccent
                                        )
                                    )
                                    viewModel.playNoteDirect(note, accent = isNextAccent)
                                    isNextAccent = false
                                }
                            )
                        }
                    }

                    // Row 2: Notes 5, 6, 7, 8
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (note in 5..8) {
                            NumericPadKey(
                                label = "$note",
                                subLabel = if (note % 2 != 0) "چپ (L)" else "راست (R)",
                                isSpecial = false,
                                color = getNoteColor(note),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val nextPos = noteEvents.size.toDouble()
                                    noteEvents.add(
                                        NoteEvent(
                                            noteNumber = note,
                                            beatPosition = nextPos,
                                            accent = isNextAccent
                                        )
                                    )
                                    viewModel.playNoteDirect(note, accent = isNextAccent)
                                    isNextAccent = false
                                }
                            )
                        }
                    }

                    // Row 3: Modifiers (Rest, Accent, Backspace, Clear)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Rest (سکوت)
                        Button(
                            onClick = {
                                val nextPos = noteEvents.size.toDouble()
                                noteEvents.add(
                                    NoteEvent(
                                        noteNumber = 0,
                                        beatPosition = nextPos,
                                        isRest = true
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سکوت 𝄽", fontSize = 11.sp, color = Color.White)
                        }

                        // Accent Toggle (تأکید)
                        Button(
                            onClick = { isNextAccent = !isNextAccent },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNextAccent) HandpanGold else CharcoalSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "تأکید ▲",
                                fontSize = 11.sp,
                                color = if (isNextAccent) CharcoalBlack else Color.White,
                                fontWeight = if (isNextAccent) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Backspace (حذف آخرین نت)
                        IconButton(
                            onClick = {
                                if (noteEvents.isNotEmpty()) {
                                    noteEvents.removeAt(noteEvents.size - 1)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CharcoalSurfaceVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "حذف", tint = Color.LightGray)
                        }

                        // Clear All
                        IconButton(
                            onClick = { noteEvents.clear() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CharcoalSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "پاکسازی", tint = Color.LightGray)
                        }
                    }
                }
            }

            // Save Pattern Button
            Button(
                onClick = {
                    if (noteEvents.isNotEmpty()) {
                        val calculatedBars = ((noteEvents.size + 3) / 4).coerceAtLeast(1)
                        val newPattern = HandpanPattern(
                            id = "custom_" + UUID.randomUUID().toString().take(8),
                            title = title.ifBlank { "الگوی شخصی من" },
                            description = "الگوی سفارشی ساخته‌شده توسط کاربر",
                            bpm = bpm,
                            timeSignature = selectedTimeSignature,
                            bars = calculatedBars,
                            events = noteEvents.toList(),
                            difficulty = DifficultyLevel.BEGINNER,
                            category = PatternCategory.CUSTOM,
                            isCustom = true
                        )
                        viewModel.saveCustomPattern(newPattern)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_save_pattern"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HandpanGold)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = CharcoalBlack)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ذخیره در کتابخانه تمرین‌ها", color = CharcoalBlack, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumericPadKey(
    label: String,
    subLabel: String?,
    isSpecial: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = if (isSpecial) 18.sp else 19.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.75f)
                )
            }
        }
    }
}
