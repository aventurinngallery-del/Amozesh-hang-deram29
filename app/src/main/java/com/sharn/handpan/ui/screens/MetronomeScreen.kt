package com.sharn.handpan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimeSignature
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.BeatDownbeatColor
import com.sharn.handpan.ui.theme.BeatRegularColor
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight
import com.sharn.handpan.ui.theme.HandpanTerracotta

@Composable
fun MetronomeScreen(
    viewModel: HandpanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metronomeState by viewModel.metronomeEngine.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(horizontal = 16.dp)
            .testTag("metronome_screen")
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
                modifier = Modifier.testTag("metronome_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }

            Text(
                text = "مترونوم اختصاصی",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Visual Metronome Pulse Ring
            VisualMetronomeRing(
                isPlaying = metronomeState.isPlaying,
                currentBeat = metronomeState.currentBeat,
                isDownbeat = metronomeState.isDownbeat,
                beatsPerBar = metronomeState.timeSignature.beatsPerBar
            )

            // BPM Display with Stepper Buttons
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
                    Text(
                        text = "سرعت ضرب‌آهنگ (BPM)",
                        style = MaterialTheme.typography.labelMedium,
                        color = HandpanBronze
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { viewModel.metronomeEngine.setBpm(metronomeState.bpm - 1) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CharcoalSurfaceVariant)
                                .testTag("metronome_minus_btn")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "کاهش", tint = Color.White)
                        }

                        Text(
                            text = "${metronomeState.bpm}",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = HandpanGold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        IconButton(
                            onClick = { viewModel.metronomeEngine.setBpm(metronomeState.bpm + 1) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CharcoalSurfaceVariant)
                                .testTag("metronome_plus_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزایش", tint = Color.White)
                        }
                    }

                    // Slider
                    Slider(
                        value = metronomeState.bpm.toFloat(),
                        onValueChange = { viewModel.metronomeEngine.setBpm(it.toInt()) },
                        valueRange = 40f..240f,
                        steps = 199,
                        colors = SliderDefaults.colors(
                            thumbColor = HandpanGold,
                            activeTrackColor = HandpanGold,
                            inactiveTrackColor = CharcoalBorder
                        ),
                        modifier = Modifier.testTag("metronome_slider")
                    )

                    // Tap Tempo Button
                    Button(
                        onClick = { viewModel.metronomeEngine.tapTempo() },
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tap_tempo_button")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = HandpanGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tap Tempo (ضربه برای تشخیص سرعت)", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Main Play / Stop Button
            Button(
                onClick = { viewModel.metronomeEngine.togglePlay() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (metronomeState.isPlaying) HandpanTerracotta else HandpanGold
                ),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("metronome_play_button")
            ) {
                Icon(
                    if (metronomeState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (metronomeState.isPlaying) "توقف" else "شروع",
                    tint = CharcoalBlack,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Time Signature Selector (میزان‌نما)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "میزان‌نما (Time Signature)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeSignature.ALL_PRESETS.forEach { ts ->
                            val isSelected = (ts == metronomeState.timeSignature)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) HandpanGold else CharcoalSurfaceVariant)
                                    .border(1.dp, if (isSelected) Color.White else CharcoalBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.metronomeEngine.setTimeSignature(ts) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ts.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CharcoalBlack else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Subdivision Selector (تقسیم ضربات)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "تقسیم ضربات (Subdivision)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Subdivision.values().forEach { sub ->
                            val isSelected = (sub == metronomeState.subdivision)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) HandpanBronze else CharcoalSurfaceVariant)
                                    .border(1.dp, if (isSelected) Color.White else CharcoalBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.metronomeEngine.setSubdivision(sub) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sub.persianName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // Downbeat Accent Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تأکید صدای ضرب اول (Accent)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "پخش تیک بم‌تر و رساتر در ضرب اول هر میزان",
                            style = MaterialTheme.typography.bodySmall,
                            color = HandpanBronze
                        )
                    }

                    Switch(
                        checked = metronomeState.accentFirstBeat,
                        onCheckedChange = { viewModel.metronomeEngine.setAccentFirstBeat(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HandpanGold,
                            checkedTrackColor = CharcoalBorder
                        ),
                        modifier = Modifier.testTag("metronome_accent_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VisualMetronomeRing(
    isPlaying: Boolean,
    currentBeat: Int,
    isDownbeat: Boolean,
    beatsPerBar: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying && isDownbeat) 1.12f else if (isPlaying) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "pulseRing"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(scale)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = if (isPlaying && isDownbeat) BeatDownbeatColor.copy(alpha = 0.25f)
                else if (isPlaying) HandpanGold.copy(alpha = 0.15f)
                else CharcoalSurfaceVariant,
                radius = size.minDimension / 2f
            )

            drawCircle(
                color = if (isPlaying && isDownbeat) BeatDownbeatColor
                else if (isPlaying) HandpanGold
                else CharcoalBorder,
                radius = size.minDimension / 2f,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isPlaying) "$currentBeat" else "آماده",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = if (isDownbeat) BeatDownbeatColor else HandpanGold
            )
            Text(
                text = if (isPlaying) "از $beatsPerBar" else "مترونوم",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
    }
}
