package com.sharn.handpan.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharn.handpan.audio.RecordedTrack
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@Composable
fun PerformanceRecorderDialog(
    viewModel: HandpanViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recorderState by viewModel.performanceRecorder.state.collectAsStateWithLifecycle()
    var customTrackName by remember { mutableStateOf("") }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.performanceRecorder.startRecording(viewModel.appUiState.value.currentScaleConfig)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, HandpanBronze.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("performance_recorder_dialog"),
            colors = CardDefaults.cardColors(containerColor = CharcoalDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (recorderState.isRecording) Color.Red.copy(alpha = 0.2f) else HandpanGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (recorderState.isRecording) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (recorderState.isRecording) Color.Red else HandpanGold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "استودیوی ضبط و لوپر (Recorder & Looper)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (recorderState.isRecording) "در حال ضبط زنده... (${recorderState.recordingEventsCount} ضربه)" else "ضبط ضربات زنده، پخش حلقه‌ای (Loop) و خروجی نت‌ها",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (recorderState.isRecording) Color(0xFFFF8A80) else HandpanBronze
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Record Action Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (recorderState.isRecording) Color(0xFF3B1515) else CharcoalSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (recorderState.isRecording) "در حال ضبط ضربات دست شما..." else "آماده برای ضبط بداهه‌نوازی",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (recorderState.isRecording) "هر نتی که بنوازید با دقت زمانی در پایگاه‌داده ثبت می‌شود." else "دکمه ضبط را بزنید و روی هنگ‌درام بنوازید.",
                                    color = Color(0xFFD6C8BB),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (recorderState.isRecording) {
                                        val scaleName = viewModel.appUiState.value.currentInstrumentProfile.scaleName
                                        val practiceState = viewModel.practiceEngine.uiState.value
                                        val track = viewModel.performanceRecorder.stopRecording(
                                            scaleName = scaleName,
                                            customTitle = customTrackName.ifBlank { null },
                                            bpm = practiceState.bpm,
                                            timeSignature = practiceState.pattern?.timeSignature?.displayName ?: "4/4"
                                        )
                                        customTrackName = ""
                                        if (track != null) {
                                            Toast.makeText(context, "قطعه «${track.title}» در پایگاه‌داده ذخیره شد", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (hasMicPermission) {
                                            viewModel.performanceRecorder.startRecording(
                                                viewModel.appUiState.value.currentScaleConfig
                                            )
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (recorderState.isRecording) Color.Red else HandpanGold
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (recorderState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = if (recorderState.isRecording) Color.White else CharcoalDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (recorderState.isRecording) "پایان ضبط" else "شروع ضبط",
                                    color = if (recorderState.isRecording) Color.White else CharcoalDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!recorderState.isRecording) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customTrackName,
                                onValueChange = { customTrackName = it },
                                label = { Text("نام قطعه (اختیاری)", fontSize = 11.sp, color = Color.Gray) },
                                placeholder = { Text("مثلاً: قطعه بداهه غروب بیات", fontSize = 11.sp, color = Color.DarkGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HandpanGold,
                                    unfocusedBorderColor = CharcoalSurfaceVariant,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Looper Controls (Speed & Loop)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("سرعت بازپخش:", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf(0.75f, 1.0f, 1.25f).forEach { speed ->
                            val isSelected = recorderState.playbackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) HandpanGold else CharcoalSurface)
                                    .clickable { viewModel.performanceRecorder.setPlaybackSpeed(speed) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CharcoalDark else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    // Loop Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (recorderState.isLooping) HandpanGold.copy(alpha = 0.25f) else CharcoalSurface)
                            .clickable { viewModel.performanceRecorder.toggleLoop() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Loop,
                                contentDescription = null,
                                tint = if (recorderState.isLooping) HandpanGoldLight else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (recorderState.isLooping) "حلقه روشن" else "حلقه خاموش",
                                fontSize = 11.sp,
                                color = if (recorderState.isLooping) HandpanGoldLight else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recorded Tracks List
                Text(
                    text = "قطعات ذخیره‌شده (${recorderState.tracks.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (recorderState.tracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalSurface.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هنوز قطعه‌ای ضبط نکرده‌اید.\nروی «شروع ضبط» بزنید و ساز را بنوازید.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recorderState.tracks, key = { it.id }) { track ->
                            val isPlaying = recorderState.playingTrackId == track.id

                            TrackItemCard(
                                track = track,
                                isPlaying = isPlaying,
                                onPlayToggle = {
                                    if (isPlaying) {
                                        viewModel.performanceRecorder.stopPlayback()
                                    } else {
                                        viewModel.performanceRecorder.playTrack(track)
                                    }
                                },
                                onExport = {
                                    val json = viewModel.performanceRecorder.exportTrackAsJSON(track)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Handpan Track JSON", json))
                                    Toast.makeText(context, "کد JSON قطعه در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    viewModel.performanceRecorder.deleteTrack(track.id)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = HandpanGold)
                    ) {
                        Text("بستن و ادامه نوازندگی", color = CharcoalDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItemCard(
    track: RecordedTrack,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) CharcoalSurfaceVariant else CharcoalSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) HandpanGold else CharcoalDark)
                        .clickable { onPlayToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) CharcoalDark else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = track.title,
                        color = if (isPlaying) HandpanGoldLight else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${track.date} • ${track.events.size} ضربه • ${(track.durationMs / 1000.0).toInt()} ثانیه",
                        color = HandpanBronze,
                        fontSize = 10.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.Share, contentDescription = "خروجی JSON", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
