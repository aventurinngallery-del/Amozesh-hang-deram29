package com.sharn.handpan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharn.handpan.model.TimeSignature
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.BeatDownbeatColor
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight
import com.sharn.handpan.ui.theme.HandpanTerracotta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TapTimingAccuracy {
    PERFECT,
    EARLY,
    LATE,
    MISSED
}

data class TapScoreEntry(
    val tapTimeNanos: Long,
    val targetTimeNanos: Long,
    val deltaMs: Long,
    val accuracy: TapTimingAccuracy
)

object RhythmTapEvaluator {
    const val DEFAULT_WINDOW_MS = 160L

    fun evaluate(
        tapNanos: Long,
        previousTargetNanos: Long,
        nextTargetNanos: Long,
        windowMs: Long = DEFAULT_WINDOW_MS
    ): Pair<Long, TapTimingAccuracy> {
        if (previousTargetNanos <= 0L && nextTargetNanos <= 0L) {
            return 0L to TapTimingAccuracy.MISSED
        }
        val previousDistance = if (previousTargetNanos > 0L) {
            kotlin.math.abs(tapNanos - previousTargetNanos)
        } else Long.MAX_VALUE
        val nextDistance = if (nextTargetNanos > 0L) {
            kotlin.math.abs(tapNanos - nextTargetNanos)
        } else Long.MAX_VALUE
        val target = if (previousDistance <= nextDistance) previousTargetNanos else nextTargetNanos
        val deltaMs = (tapNanos - target) / 1_000_000L
        val accuracy = when {
            kotlin.math.abs(deltaMs) <= 45L -> TapTimingAccuracy.PERFECT
            kotlin.math.abs(deltaMs) <= windowMs ->
                if (deltaMs < 0L) TapTimingAccuracy.EARLY else TapTimingAccuracy.LATE
            else -> TapTimingAccuracy.MISSED
        }
        return deltaMs to accuracy
    }
}

@Composable
fun RhythmTrainerScreen(
    viewModel: HandpanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appState by viewModel.appUiState.collectAsStateWithLifecycle()
    val metronomeState by viewModel.metronomeEngine.state.collectAsStateWithLifecycle()
    var isTrainingActive by remember { mutableStateOf(false) }
    var currentBpm by remember { mutableIntStateOf(70) }
    var selectedTimeSignature by remember { mutableStateOf(TimeSignature.Common44) }

    var lastTargetBeatNanos by remember { mutableLongStateOf(0L) }
    var nextTargetBeatNanos by remember { mutableLongStateOf(0L) }
    var currentBeatNumber by remember { mutableIntStateOf(1) }

    val recentTaps = remember { mutableStateListOf<TapScoreEntry>() }
    var lastFeedbackStatus by remember { mutableStateOf<TapTimingAccuracy?>(null) }
    var lastDeltaMs by remember { mutableLongStateOf(0L) }
    var streakCount by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }

    var perfectTapsCount by remember { mutableIntStateOf(0) }
    var earlyTapsCount by remember { mutableIntStateOf(0) }
    var lateTapsCount by remember { mutableIntStateOf(0) }
    var missedTapsCount by remember { mutableIntStateOf(0) }
    var totalTapsCount by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.metronomeEngine.stop()
        }
    }

    fun startTraining() {
        isTrainingActive = true
        recentTaps.clear()
        perfectTapsCount = 0
        earlyTapsCount = 0
        lateTapsCount = 0
        missedTapsCount = 0
        totalTapsCount = 0
        streakCount = 0
        lastFeedbackStatus = null

        viewModel.metronomeEngine.setBpm(currentBpm)
        viewModel.metronomeEngine.setTimeSignature(selectedTimeSignature)

        viewModel.metronomeEngine.start()
    }

    fun stopTraining() {
        isTrainingActive = false
        viewModel.metronomeEngine.stop()
    }

    fun registerTap() {
        if (!isTrainingActive) {
            startTraining()
            return
        }

        val tapNanos = viewModel.metronomeEngine.nowNanos()
        val tick = viewModel.metronomeEngine.state.value
        lastTargetBeatNanos = tick.lastTickTimestampNanos
        nextTargetBeatNanos = tick.nextTickTimestampNanos
        currentBeatNumber = tick.currentBeat
        val (deltaMs, accuracy) = RhythmTapEvaluator.evaluate(
            tapNanos = tapNanos,
            previousTargetNanos = lastTargetBeatNanos,
            nextTargetNanos = nextTargetBeatNanos
        )
        val nearestTargetNanos = if (lastTargetBeatNanos > 0L &&
            kotlin.math.abs(tapNanos - lastTargetBeatNanos) <= kotlin.math.abs(tapNanos - nextTargetBeatNanos)
        ) lastTargetBeatNanos else nextTargetBeatNanos

        lastFeedbackStatus = accuracy
        lastDeltaMs = deltaMs
        totalTapsCount++

        when (accuracy) {
            TapTimingAccuracy.PERFECT -> {
                perfectTapsCount++
                streakCount++
                if (streakCount > bestStreak) bestStreak = streakCount
                if (appState.isHapticEnabled) viewModel.hapticHelper.performClick(true)
            }
            TapTimingAccuracy.EARLY -> {
                earlyTapsCount++
                streakCount = 0
                if (appState.isHapticEnabled) viewModel.hapticHelper.performClick(false)
            }
            TapTimingAccuracy.LATE -> {
                lateTapsCount++
                streakCount = 0
                if (appState.isHapticEnabled) viewModel.hapticHelper.performClick(false)
            }
            TapTimingAccuracy.MISSED -> {
                missedTapsCount++
                streakCount = 0
            }
        }

        val entry = TapScoreEntry(
            tapTimeNanos = tapNanos,
            targetTimeNanos = nearestTargetNanos,
            deltaMs = deltaMs,
            accuracy = accuracy
        )
        recentTaps.add(0, entry)
        if (recentTaps.size > 8) {
            recentTaps.removeAt(recentTaps.lastIndex)
        }
    }

    val overallAccuracy = if (totalTapsCount > 0) {
        ((perfectTapsCount * 1.0f + (totalTapsCount - perfectTapsCount - earlyTapsCount - lateTapsCount - missedTapsCount) * 0.5f) / totalTapsCount * 100f).coerceIn(0f, 100f)
    } else 100f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("rhythm_trainer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("rhythm_trainer_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "مربی ریتم و زمان‌بندی (Rhythm Trainer)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تقویت دقت میلی‌ثانیه‌ای ضربات دست روی مترونوم",
                    style = MaterialTheme.typography.bodySmall,
                    color = HandpanGoldLight
                )
            }

            IconButton(
                onClick = {
                    stopTraining()
                    startTraining()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "شروع مجدد", tint = HandpanGold)
            }
        }

        // Metronome Beat Visualizer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..selectedTimeSignature.beatsPerBar) {
                        val isCurrent = isTrainingActive && (currentBeatNumber == i)
                        val isDownbeat = (i == 1)
                        val animatedScale by animateFloatAsState(targetValue = if (isCurrent) 1.25f else 1.0f)
                        val circleColor = when {
                            isCurrent && isDownbeat -> BeatDownbeatColor
                            isCurrent -> HandpanGold
                            else -> CharcoalSurfaceVariant
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(animatedScale)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$i",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isCurrent) CharcoalBlack else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDownbeat) "ضرب اول" else "ضرب $i",
                                fontSize = 9.sp,
                                color = if (isCurrent) HandpanGoldLight else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Feedback Badge
                val (feedbackText, feedbackColor) = when (lastFeedbackStatus) {
                    TapTimingAccuracy.PERFECT -> Pair("دقیقاً روی ضرب (Perfect! ±45ms) 🎯", Color(0xFF4CAF50))
                    TapTimingAccuracy.EARLY -> Pair("کمی زودتر زدید (${lastDeltaMs}ms Early) ⚡", Color(0xFFFFA726))
                    TapTimingAccuracy.LATE -> Pair("کمی دیرتر زدید (+${lastDeltaMs}ms Late) 🐢", Color(0xFFFF7043))
                    TapTimingAccuracy.MISSED -> Pair("ضربه از دست رفت ⚠️", Color(0xFFF44336))
                    null -> Pair("روی صفحه یا ساز دقیقاً همگام با کلیک مترونوم ضربه بزنید", Color.LightGray)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = feedbackColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, feedbackColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = feedbackText,
                        color = feedbackColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Giant Interactive Strike Pad (Large Tap Target)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, if (isTrainingActive) HandpanGold else HandpanBronze, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    registerTap()
                }
                .testTag("rhythm_trainer_strike_pad"),
            colors = CardDefaults.cardColors(
                containerColor = CharcoalDark
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                HandpanGold.copy(alpha = if (isTrainingActive) 0.25f else 0.08f),
                                CharcoalSurface,
                                CharcoalDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (isTrainingActive) HandpanGold else Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isTrainingActive) "اینجا ضربه بزنید (Tap / Strike)" else "لمس کنید تا مترونوم و سنجش آغاز شود",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isTrainingActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "استریک متوالی دقیق: $streakCount (بهترین: $bestStreak)",
                            fontSize = 12.sp,
                            color = HandpanGoldLight
                        )
                    }
                }
            }
        }

        // Live Timing Error Meter & History
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
                        text = "آمار دقت ضربات",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "میانگین دقت: ${overallAccuracy.toInt()}٪",
                        fontWeight = FontWeight.Bold,
                        color = HandpanGold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("عالی: $perfectTapsCount", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    Text("کمی زود: $earlyTapsCount", fontSize = 11.sp, color = Color(0xFFFFA726))
                    Text("کمی دیر: $lateTapsCount", fontSize = 11.sp, color = Color(0xFFFF7043))
                    Text("کل ضربات: $totalTapsCount", fontSize = 11.sp, color = Color.LightGray)
                }

                if (recentTaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("آخرین انحرافات زمانی (بر حسب میلی‌ثانیه):", fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentTaps.take(6).forEach { tap ->
                            val chipColor = when (tap.accuracy) {
                                TapTimingAccuracy.PERFECT -> Color(0xFF4CAF50)
                                TapTimingAccuracy.EARLY -> Color(0xFFFFA726)
                                TapTimingAccuracy.LATE -> Color(0xFFFF7043)
                                TapTimingAccuracy.MISSED -> Color(0xFFF44336)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = chipColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, chipColor)
                            ) {
                                Text(
                                    text = "${if (tap.deltaMs > 0) "+" else ""}${tap.deltaMs}ms",
                                    color = chipColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Settings (BPM & Time Signature)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // BPM Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سرعت تمپو (BPM)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentBpm > 40) {
                                    currentBpm--
                                    if (isTrainingActive) startTraining()
                                }
                            },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(CharcoalSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                        }
                        Text(
                            text = "$currentBpm BPM",
                            fontWeight = FontWeight.Bold,
                            color = HandpanGold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        IconButton(
                            onClick = {
                                if (currentBpm < 200) {
                                    currentBpm++
                                    if (isTrainingActive) startTraining()
                                }
                            },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(CharcoalSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Slider(
                    value = currentBpm.toFloat(),
                    onValueChange = {
                        currentBpm = it.toInt()
                        if (isTrainingActive) startTraining()
                    },
                    valueRange = 40f..200f,
                    colors = SliderDefaults.colors(thumbColor = HandpanGold, activeTrackColor = HandpanGold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Time Signature selector (including 6/8 and 7/8)
                Text("میزان‌نما:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeSignature.ALL_PRESETS.forEach { ts ->
                        val isSelected = (selectedTimeSignature == ts)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedTimeSignature = ts
                                    if (isTrainingActive) startTraining()
                                },
                            color = if (isSelected) HandpanGold else CharcoalSurfaceVariant
                        ) {
                            Text(
                                text = ts.displayName,
                                color = if (isSelected) CharcoalBlack else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Start / Stop Button
        Button(
            onClick = {
                if (isTrainingActive) stopTraining() else startTraining()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTrainingActive) HandpanTerracotta else HandpanGold
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("rhythm_trainer_toggle_button")
        ) {
            Icon(
                if (isTrainingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = CharcoalBlack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTrainingActive) "توقف تمرین ریتم" else "شروع سنجش زمان‌بندی ضربات",
                color = CharcoalBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
