package com.sharn.handpan.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.sharn.handpan.audio.DetectedPitchResult
import com.sharn.handpan.audio.PitchDetector
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.HandpanGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomSamplerDialog(
    viewModel: HandpanViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val state = viewModel.appUiState.value
    val selectedNote = state.selectedSamplerNote
    val isRecording = state.isRecordingSample
    val amplitude = state.recordingAmplitude
    val customSamplesMap = state.customSamplesMap
    val successMessage = state.sampleSuccessMessage
    val scaleConfig = state.currentScaleConfig

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startRecordingCustomSample(selectedNote)
        }
    }

    val noteLabels = listOf(
        0 to "Ding (نت مرکزی)",
        1 to "نت ۱ (${scaleConfig.getPitchName(1)})",
        2 to "نت ۲ (${scaleConfig.getPitchName(2)})",
        3 to "نت ۳ (${scaleConfig.getPitchName(3)})",
        4 to "نت ۴ (${scaleConfig.getPitchName(4)})",
        5 to "نت ۵ (${scaleConfig.getPitchName(5)})",
        6 to "نت ۶ (${scaleConfig.getPitchName(6)})",
        7 to "نت ۷ (${scaleConfig.getPitchName(7)})",
        8 to "نت ۸ (${scaleConfig.getPitchName(8)})",
        NotePitchConfig.NOTE_SLAP to "نت S (Slap / اسلپ)"
    )

    var isTunerActive by remember { mutableStateOf(true) }
    var detectedPitch by remember { mutableStateOf<DetectedPitchResult?>(null) }
    val pitchDetector = remember { PitchDetector() }

    DisposableEffect(isTunerActive, hasMicPermission, isRecording) {
        if (isTunerActive && hasMicPermission && !isRecording) {
            pitchDetector.startListening(
                scaleConfig = scaleConfig,
                onContinuousPitch = { pitch ->
                    detectedPitch = pitch
                },
                onStrikeDetected = { pitch, _ ->
                    detectedPitch = pitch
                    if (pitch.matchedNoteNumber != null) {
                        viewModel.selectSamplerNote(pitch.matchedNoteNumber)
                    }
                }
            )
        } else {
            pitchDetector.stopListening()
        }

        onDispose {
            pitchDetector.stopListening()
        }
    }

    val isCurrentNoteCustom = customSamplesMap[selectedNote] == true

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude,
        label = "amplitude"
    )

    AlertDialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .clip(RoundedCornerShape(24.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "استودیوی کالیبراسیون ساز واقعی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "وارد کردن صدای نت‌های هنگ‌درام شخصی شما",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Info banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "یک نت را انتخاب کنید، دکمه ضبط را بزنید و یک ضربه شفاف روی همان نت ساز خود بنوازید تا از این پس در تمام تمرین‌ها و آهنگ‌ها با صدای واقعی ساز خودتان بنوازید!",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Pitch Detector & Tuner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = HandpanGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تشخیص هوشمند نت با ضربه به ساز (Tuner)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isTunerActive && hasMicPermission) Color(0xFF2E7D32) else Color.Gray,
                                modifier = Modifier.clickable {
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        isTunerActive = !isTunerActive
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isTunerActive && hasMicPermission) "شنیدن فعال" else "فعال‌سازی میکروفن",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (isTunerActive && hasMicPermission) {
                            Spacer(modifier = Modifier.height(10.dp))

                            val pitch = detectedPitch
                            val freqText = if (pitch != null && pitch.frequencyHz > 0) "${pitch.frequencyHz.toInt()} Hz" else "-- Hz"
                            val noteName = pitch?.noteName ?: "--"
                            val cents = pitch?.centsOffset ?: 0
                            val matchedNote = pitch?.matchedNoteNumber

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = noteName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (matchedNote != null) Color(0xFF4CAF50) else HandpanGold
                                    )
                                    Text(
                                        text = freqText,
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    if (matchedNote != null) {
                                        val noteIdx: Int = matchedNote
                                        val matchedName = if (noteIdx == 0) "نت مرکزی (Ding)" else if (noteIdx == NotePitchConfig.NOTE_SLAP) "ضربه اسلپ (S)" else "نت شماره $noteIdx (${scaleConfig.getPitchName(noteIdx)})"
                                        Text(
                                            text = "مطابق با: $matchedName",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "با ضربه زدن، این نت خودکار انتخاب شد",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                    } else {
                                        Text(
                                            text = "به ساز ضربه بزنید تا نت تشخیص داده شود",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }

                            // Tuner deviation bar (-50 to +50 cents)
                            if (pitch != null && pitch.frequencyHz > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("♭ بم (-50c)", fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = if (kotlin.math.abs(cents) <= 5) "دقیقاً کوک (In Tune)" else if (cents > 0) "+$cents سنت (کمی زیر)" else "$cents سنت (کمی بم)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (kotlin.math.abs(cents) <= 8) Color(0xFF4CAF50) else Color(0xFFFFA726)
                                    )
                                    Text("زیر (+50c) ♯", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "انتخاب دستی نت جهت ضبط و تست:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Note selection chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    noteLabels.forEach { (noteNum, label) ->
                        val isSelected = selectedNote == noteNum
                        val hasCustom = customSamplesMap[noteNum] == true
                        val chipText = if (noteNum == 0) "Ding" else if (noteNum == NotePitchConfig.NOTE_SLAP) "S (اسلپ)" else "نت $noteNum"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (hasCustom) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else if (hasCustom) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isRecording) {
                                    viewModel.selectSamplerNote(noteNum)
                                }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (hasCustom) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasCustom) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "ضبط شده",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = chipText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Active Selected Note Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedNote == 0) "نت دینگ (Ding - مرکز)" else if (selectedNote == NotePitchConfig.NOTE_SLAP) "ضربه اسلپ (Slap / Tak)" else "نت شماره $selectedNote (${scaleConfig.getPitchName(selectedNote)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status badge
                        Surface(
                            shape = CircleShape,
                            color = if (isCurrentNoteCustom) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCurrentNoteCustom) Icons.Default.GraphicEq else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isCurrentNoteCustom) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCurrentNoteCustom) "صوت ساز واقعی شما فعال است" else "صوت استاندارد سنتز شده",
                                    color = if (isCurrentNoteCustom) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live recording animation visualizer
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(1.0f + (animatedAmp * 0.4f).coerceIn(0f, 0.4f))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color.Red.copy(alpha = 0.8f),
                                                Color.Red.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .scale(pulseScale)
                                        .background(Color.Red, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "در حال ضبط",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "در حال ضبط... لطفاً همین حالا یک ضربه به نت بزنید!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { animatedAmp.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = Color.Red,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            // Idle Visualizer / Test Button
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.playNoteDirect(selectedNote, accent = true)
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تست و شنیدن نت")
                                }

                                if (isCurrentNoteCustom) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCustomSample(selectedNote)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف صدای ضبط شده",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Record Button
                        if (isRecording) {
                            Button(
                                onClick = {
                                    viewModel.stopRecordingCustomSample()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("پایان و ذخیره ضبط", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (hasMicPermission) {
                                        viewModel.startRecordingCustomSample(selectedNote)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCurrentNoteCustom) "ضبط مجدد صدای این نت" else "ضبط صدای ساز واقعی برای این نت",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Success message toast
                        AnimatedVisibility(visible = successMessage != null) {
                            successMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (msg.contains("خطا")) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.deleteAllCustomSamples()
                        },
                        enabled = customSamplesMap.values.any { it }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازنشانی همه نت‌ها", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss
                    ) {
                        Text("تایید و بازگشت")
                    }
                }
            }
        },
        confirmButton = {}
    )
}
