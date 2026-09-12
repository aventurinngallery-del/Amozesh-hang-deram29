package com.sharn.handpan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharn.handpan.audio.StrikeAccuracyStatus
import com.sharn.handpan.audio.MicrophoneState
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.PracticeInputMode
import com.sharn.handpan.model.PracticeMode
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.components.AcousticAssessmentSummaryDialog
import com.sharn.handpan.ui.components.ExportPatternDialog
import com.sharn.handpan.ui.components.HandpanDiscView
import com.sharn.handpan.ui.components.NoteTimelineView
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
import com.sharn.handpan.ui.theme.RestColor
import com.sharn.handpan.ui.theme.getNoteColor
import com.sharn.handpan.model.AudioCalibrationState

@Composable
fun PracticeScreen(
    viewModel: HandpanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appState by viewModel.appUiState.collectAsStateWithLifecycle()
    val practiceState by viewModel.practiceEngine.uiState.collectAsStateWithLifecycle()
    val acousticState by viewModel.practiceEngine.acousticEvaluator.state.collectAsStateWithLifecycle()
    val pattern = practiceState.pattern
    val targetState = practiceState.targetState

    var showExportDialog by remember { mutableStateOf(false) }

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
            viewModel.practiceEngine.setInputMode(PracticeInputMode.REAL_HANDPAN)
        }
    }

    val scrollState = rememberScrollState()

    if (pattern == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(CharcoalBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("الگویی انتخاب نشده است", color = Color.White)
        }
        return
    }

    // Acoustic Assessment Result Summary Dialog
    LaunchedEffect(
        acousticState.isSummaryDialogVisible,
        pattern.id,
        acousticState.totalStrikesEvaluated
    ) {
        if (!acousticState.isSummaryDialogVisible) return@LaunchedEffect
        val matchedLesson = com.sharn.handpan.ui.components.MASTER_LESSONS.find { it.patternId == pattern.id }
        if (matchedLesson != null) {
            viewModel.saveLessonResult(matchedLesson.id, acousticState.accuracyPercentage.toInt())
        }
    }

    if (acousticState.isSummaryDialogVisible) {

        AcousticAssessmentSummaryDialog(
            state = acousticState,
            onDismiss = {
                viewModel.practiceEngine.acousticEvaluator.dismissSummary()
            },
            onRestartPractice = {
                viewModel.practiceEngine.acousticEvaluator.dismissSummary()
                viewModel.practiceEngine.restart()
            }
        )
    }

    // Export Pattern Dialog
    if (showExportDialog) {
        ExportPatternDialog(
            pattern = pattern,
            onDismiss = { showExportDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(horizontal = 16.dp)
            .testTag("practice_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("practice_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pattern.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${pattern.timeSignature.displayName} • ${practiceState.effectiveBpm} BPM ${if (practiceState.speedLadderEnabled) "• نردبان سرعت فعال" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (practiceState.speedLadderEnabled) HandpanGoldLight else HandpanBronze
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Export/Share Pattern Button
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("share_pattern_button")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری الگو",
                        tint = Color.White
                    )
                }

                // Mute Sound / Physical Instrument Mode (Only metronome clicks & visual guide)
                IconButton(
                    onClick = { viewModel.practiceEngine.toggleSound() },
                    modifier = Modifier.testTag("toggle_sound_mode_button")
                ) {
                    Icon(
                        if (practiceState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (practiceState.soundEnabled) "صدای نت فعال" else "حالت تمرین با ساز واقعی (بی‌صدا)",
                        tint = if (practiceState.soundEnabled) Color.White else HandpanTerracotta
                    )
                }

                // Metronome Click Toggle
                IconButton(
                    onClick = { viewModel.practiceEngine.toggleMetronome() },
                    modifier = Modifier.testTag("toggle_metronome_button")
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "صدای مترونوم",
                        tint = if (practiceState.metronomeEnabled) HandpanGold else Color.Gray
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Count-in Overlay if Active
            AnimatedVisibility(visible = practiceState.phase == com.sharn.handpan.audio.PracticePhase.PREVIEW || practiceState.isCountIn || practiceState.phase == com.sharn.handpan.audio.PracticePhase.PAUSED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HandpanGold.copy(alpha = 0.25f))
                        .border(1.dp, HandpanGold, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (practiceState.phase == com.sharn.handpan.audio.PracticePhase.PREVIEW) {
                            "گوش کن: ضرب ${practiceState.previewBeat} از ${practiceState.previewBeatCount}"
                        } else if (practiceState.phase == com.sharn.handpan.audio.PracticePhase.PAUSED) {
                            "مکث"
                        } else {
                            "آماده شو: ${practiceState.countInBeat}"
                        },
                        fontSize = if (practiceState.isCountIn || practiceState.phase == com.sharn.handpan.audio.PracticePhase.PREVIEW) 26.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HandpanGoldLight
                    )
                }
            }

            // Input Mode Banner (Real Handpan vs Virtual Handpan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) CharcoalSurface else CharcoalDark
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) HandpanGold else CharcoalBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) HandpanGold.copy(alpha = 0.2f) else CharcoalSurfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) Icons.Default.Mic else Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) HandpanGold else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) "حالت ساز واقعی (Real Handpan)" else "حالت ساز مجازی (Virtual Handpan)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) "میکروفن صدای ساز را می‌شنود • صدای مجازی برای جلوگیری از تداخل قطع است" else "تمرین با لمس صفحه • صدای مجازی فعال است",
                                    fontSize = 10.sp,
                                    color = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) HandpanGoldLight else Color.LightGray
                                )
                            }
                        }

                        // Mode switcher switch
                        Surface(
                            shape = CircleShape,
                            color = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) HandpanGold else CharcoalSurfaceVariant,
                            modifier = Modifier
                                .semantics {
                                    contentDescription = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) {
                                        "حالت ساز واقعی، میکروفن فعال"
                                    } else {
                                        "حالت ساز مجازی، تمرین لمسی"
                                    }
                                    stateDescription = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) {
                                        "ساز واقعی انتخاب شده است"
                                    } else {
                                        "ساز مجازی انتخاب شده است"
                                    }
                                }
                                .clickable {
                                if (practiceState.inputMode == PracticeInputMode.VIRTUAL_HANDPAN) {
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.practiceEngine.setInputMode(PracticeInputMode.REAL_HANDPAN)
                                    }
                                } else {
                                    viewModel.practiceEngine.setInputMode(PracticeInputMode.VIRTUAL_HANDPAN)
                                }
                            }
                        ) {
                            Text(
                                text = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) "ساز واقعی ✓" else "تغییر به واقعی",
                                color = if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) CharcoalBlack else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Live Feedback HUD Bar when in REAL_HANDPAN mode
                    if (practiceState.inputMode == PracticeInputMode.REAL_HANDPAN) {
                        if (acousticState.microphoneState == MicrophoneState.MIC_UNAVAILABLE ||
                            acousticState.microphoneState == MicrophoneState.MIC_ERROR
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "میکروفون در دسترس نیست\nاجازه دسترسی به میکروفون را فعال کن",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Text("تلاش دوباره")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val calibrationMessage = when (acousticState.calibration.state) {
                            AudioCalibrationState.NOT_STARTED -> "آماده شنیدن ساز توست؛ برای شروع یک ضربه واضح بزن."
                            AudioCalibrationState.LISTENING -> "در حال گوش دادن به ساز واقعی..."
                            AudioCalibrationState.READY -> "برنامه آماده شنیدن اجرای توست."
                            AudioCalibrationState.NO_SIGNAL -> "صدای کافی دریافت نشد؛ کمی نزدیک‌تر و واضح‌تر به ساز ضربه بزن."
                            AudioCalibrationState.TOO_NOISY -> "صدای محیط زیاد است؛ تلویزیون یا موسیقی را کم کن یا جای آرام‌تری پیدا کن."
                            AudioCalibrationState.OVERLOADED -> "ورودی صدا خیلی قوی است؛ گوشی را کمی از ساز دورتر کن."
                            AudioCalibrationState.FAILED -> acousticState.calibration.failureReason
                                ?: "آماده‌سازی صدا انجام نشد؛ دوباره امتحان کن."
                        }
                        val calibrationColor = when (acousticState.calibration.state) {
                            AudioCalibrationState.READY -> Color(0xFF8BC34A)
                            AudioCalibrationState.FAILED,
                            AudioCalibrationState.NO_SIGNAL,
                            AudioCalibrationState.TOO_NOISY,
                            AudioCalibrationState.OVERLOADED -> Color(0xFFFFA726)
                            else -> HandpanGoldLight
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = calibrationMessage
                                    stateDescription = "وضعیت صدای ورودی: $calibrationMessage"
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (acousticState.calibration.state == AudioCalibrationState.READY) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Hearing
                                },
                                contentDescription = null,
                                tint = calibrationColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = calibrationMessage,
                                color = calibrationColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CharcoalDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Strike Accuracy Status Badge
                            val lastStatus = acousticState.lastFeedback?.status
                            val statusColor = when (lastStatus) {
                                StrikeAccuracyStatus.PERFECT -> Color(0xFF4CAF50)
                                StrikeAccuracyStatus.GOOD -> Color(0xFF8BC34A)
                                StrikeAccuracyStatus.EARLY -> Color(0xFFFFA726)
                                StrikeAccuracyStatus.LATE -> Color(0xFFFF7043)
                                StrikeAccuracyStatus.WRONG_NOTE -> Color(0xFFE91E63)
                                StrikeAccuracyStatus.UNKNOWN_NOTE -> Color(0xFFFFC107)
                                StrikeAccuracyStatus.MISSED -> Color(0xFFF44336)
                                StrikeAccuracyStatus.EXTRA_STRIKE -> Color(0xFFBA68C8)
                                else -> Color.Gray
                            }

                            val statusText = when (lastStatus) {
                                StrikeAccuracyStatus.PERFECT -> "عالی (Perfect!) 🎯"
                                StrikeAccuracyStatus.GOOD -> "خوب (Good) 👍"
                                StrikeAccuracyStatus.EARLY -> "کمی زود (Early) ⚡"
                                StrikeAccuracyStatus.LATE -> "کمی دیر (Late) 🐢"
                                StrikeAccuracyStatus.WRONG_NOTE -> "نت اشتباه ❌"
                                StrikeAccuracyStatus.UNKNOWN_NOTE -> "ضربه تشخیص داده شد، اما نت نامشخص است"
                                StrikeAccuracyStatus.MISSED -> "از دست رفته (Miss) ⚠️"
                                StrikeAccuracyStatus.EXTRA_STRIKE -> "ضربه اضافه؛ روی الگو بمانید"
                                else -> "در انتظار ضربه ساز واقعی..."
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Accuracy Stats
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = HandpanGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val correctCount = acousticState.perfectCount + acousticState.goodCount
                                Text(
                                    text = "دقت: ${acousticState.accuracyPercentage.toInt()}% ($correctCount/${acousticState.totalExpectedNotes})",
                                    fontSize = 11.sp,
                                    color = HandpanGoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 1. Prominent Active Note Display (Large Center Highlight)
            GiantNoteDisplay(
                activeNoteNumber = targetState?.currentNote?.noteNumber ?: -1,
                isRest = targetState?.currentNote?.isRest == true,
                isAccent = targetState?.currentNote?.accent == true,
                hand = targetState?.currentNote?.hand,
                mode = practiceState.mode
            )

            // 2. Note Timeline & Beat Indicators (e.g. 1 3 5 3 / ● ● ● ●)
            NoteTimelineView(
                pattern = pattern,
                currentNoteIndex = targetState?.currentNoteIndex ?: -1,
                currentBeatInBar = targetState?.currentBeatInBar ?: 1.0,
                currentBar = targetState?.barNumber ?: 1,
                mode = practiceState.mode,
                notationSystem = appState.preferredNotationSystem
            )

            // 3. Interactive Handpan Disc View
            HandpanDiscView(
                activeNoteNumber = targetState?.currentNote?.noteNumber ?: -1,
                onNoteTapped = { noteNum ->
                    viewModel.practiceEngine.playInputNote(noteNum)
                },
                modifier = Modifier.size(240.dp),
                isInteractive = true,
                customSamplesMap = appState.customSamplesMap
            )

            // 4. Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restart Button
                IconButton(
                    onClick = { viewModel.practiceEngine.restart() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CharcoalSurfaceVariant)
                        .testTag("practice_restart_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "شروع مجدد", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Play / Pause Giant Button
                Button(
                    onClick = { viewModel.practiceEngine.togglePlay() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (practiceState.isPlaying) HandpanTerracotta else HandpanGold
                    ),
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("practice_play_pause_button")
                ) {
                    Icon(
                        if (practiceState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (practiceState.isPlaying) "توقف" else "پخش",
                        tint = CharcoalBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Loop Toggle Button
                IconButton(
                    onClick = { viewModel.practiceEngine.toggleLoop() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (practiceState.isLoopEnabled) HandpanGold.copy(alpha = 0.25f)
                            else CharcoalSurfaceVariant
                        )
                        .border(
                            1.dp,
                            if (practiceState.isLoopEnabled) HandpanGold else Color.Transparent,
                            CircleShape
                        )
                        .testTag("practice_loop_button")
                ) {
                    Icon(
                        Icons.Default.Loop,
                        contentDescription = "تکرار پیوسته (لوپ)",
                        tint = if (practiceState.isLoopEnabled) HandpanGold else Color.Gray
                    )
                }
            }

            // 5. Speed Controls Card (BPM Slider + Presets + Speed % + Speed Ladder Trainer)
            SpeedControlsCard(
                bpm = practiceState.bpm,
                effectiveBpm = practiceState.effectiveBpm,
                speedMultiplier = practiceState.speedMultiplier,
                speedLadderEnabled = practiceState.speedLadderEnabled,
                ladderIncrement = practiceState.ladderBpmIncrement,
                ladderRounds = practiceState.ladderRoundsPerStep,
                ladderTargetBpm = practiceState.ladderTargetBpm,
                roundsCompleted = practiceState.totalRoundsCompleted,
                onBpmChanged = { viewModel.practiceEngine.setBpm(it) },
                onMultiplierChanged = { viewModel.practiceEngine.setSpeedMultiplier(it) },
                onToggleSpeedLadder = { viewModel.practiceEngine.toggleSpeedLadder() },
                onConfigureLadder = { inc, rounds, target ->
                    viewModel.practiceEngine.configureSpeedLadder(inc, rounds, target)
                }
            )

            // 6. Practice Mode Selector
            PracticeModeSelector(
                currentMode = practiceState.mode,
                onModeSelected = { viewModel.practiceEngine.setPracticeMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GiantNoteDisplay(
    activeNoteNumber: Int,
    isRest: Boolean,
    isAccent: Boolean,
    hand: String?,
    mode: PracticeMode
) {
    val hasActiveNote = (activeNoteNumber >= 0) || isRest
    val noteColor = if (isRest) RestColor else getNoteColor(activeNoteNumber)

    val scale by animateFloatAsState(
        targetValue = if (hasActiveNote) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "giantScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .border(
                2.dp,
                if (hasActiveNote) noteColor else CharcoalBorder,
                RoundedCornerShape(22.dp)
            )
            .testTag("giant_note_display"),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "نت یا ضربه در حال اجرا:",
                style = MaterialTheme.typography.labelMedium,
                color = HandpanBronze
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isRest) {
                Text(
                    text = "سکوت (𝄽)",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = RestColor
                )
                Text(
                    text = "دست‌ها را ثابت نگه دارید",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            } else if (activeNoteNumber in 0..9) {
                val symbol = when (activeNoteNumber) {
                    0 -> "D"
                    9 -> "S"
                    else -> "$activeNoteNumber"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isAccent) {
                        Text(
                            text = "▲ ",
                            fontSize = 28.sp,
                            color = HandpanGoldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = symbol,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = noteColor
                    )
                }

                val noteHint = when (activeNoteNumber) {
                    0 -> "نت دینگ (مرکز بم)"
                    9 -> "ضربه اسلپ (تکنیک ریتمیک بدنه)"
                    else -> "نت شماره $activeNoteNumber"
                }
                Text(
                    text = "$noteHint ${if (hand != null) "• دست ${if (hand == "R") "راست" else "چپ"}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HandpanGoldLight,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "—",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = "دکمه پخش را بزنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SpeedControlsCard(
    bpm: Int,
    effectiveBpm: Int,
    speedMultiplier: Float,
    speedLadderEnabled: Boolean,
    ladderIncrement: Int,
    ladderRounds: Int,
    ladderTargetBpm: Int,
    roundsCompleted: Int,
    onBpmChanged: (Int) -> Unit,
    onMultiplierChanged: (Float) -> Unit,
    onToggleSpeedLadder: () -> Unit,
    onConfigureLadder: (Int, Int, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
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
                Text(
                    text = "سرعت تمرین (BPM)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$effectiveBpm BPM",
                    style = MaterialTheme.typography.titleMedium,
                    color = HandpanGold,
                    fontWeight = FontWeight.Bold
                )
            }

            // BPM Slider
            Slider(
                value = bpm.toFloat(),
                onValueChange = { onBpmChanged(it.toInt()) },
                valueRange = 40f..220f,
                steps = 179,
                colors = SliderDefaults.colors(
                    thumbColor = HandpanGold,
                    activeTrackColor = HandpanGold,
                    inactiveTrackColor = CharcoalBorder
                ),
                modifier = Modifier.testTag("practice_bpm_slider")
            )

            // Quick BPM Buttons (50, 60, 70, 80, 90)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(50, 60, 70, 80, 90).forEach { presetBpm ->
                    val isSelected = (bpm == presetBpm && speedMultiplier == 1.0f)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HandpanGold else CharcoalSurfaceVariant)
                            .clickable {
                                onMultiplierChanged(1.0f)
                                onBpmChanged(presetBpm)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$presetBpm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CharcoalBlack else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speed Ladder (مربی افزایش خودکار سرعت)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (speedLadderEnabled) HandpanGold.copy(alpha = 0.5f) else CharcoalBorder,
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (speedLadderEnabled) HandpanGold.copy(alpha = 0.12f) else CharcoalSurfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (speedLadderEnabled) HandpanGold else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مربی نردبان سرعت (Auto-BPM)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (speedLadderEnabled) Color.White else Color.LightGray
                            )
                        }

                        Button(
                            onClick = onToggleSpeedLadder,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (speedLadderEnabled) HandpanGold else CharcoalSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (speedLadderEnabled) "فعال" else "خاموش",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (speedLadderEnabled) CharcoalBlack else Color.White
                            )
                        }
                    }

                    if (speedLadderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هر $ladderRounds دور اجرا ➔ +$ladderIncrement BPM (تا سقف $ladderTargetBpm BPM)",
                            fontSize = 11.sp,
                            color = HandpanGoldLight
                        )
                        Text(
                            text = "دورهای تکمیل‌شده فعلی: $roundsCompleted",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Percentage Multiplier (50%, 75%, 100%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ضریب سرعت:",
                    style = MaterialTheme.typography.bodySmall,
                    color = HandpanBronze
                )

                listOf(0.5f to "۵۰٪ (آهسته)", 0.75f to "۷۵٪", 1.0f to "۱۰۰٪ (عادی)").forEach { (multiplier, label) ->
                    val isSelected = (speedMultiplier == multiplier)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HandpanBronze else CharcoalSurfaceVariant)
                            .clickable { onMultiplierChanged(multiplier) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeModeSelector(
    currentMode: PracticeMode,
    onModeSelected: (PracticeMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "حالت تمرین",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PracticeMode.values().forEach { mode ->
                    val isSelected = (mode == currentMode)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) HandpanGold else CharcoalSurfaceVariant)
                            .border(1.dp, if (isSelected) Color.White else CharcoalBorder, RoundedCornerShape(12.dp))
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                PracticeMode.FOLLOW -> "هم‌نوازی"
                                PracticeMode.RHYTHM -> "ریتم"
                                PracticeMode.CHALLENGE -> "چالش حافظه"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CharcoalBlack else Color.White
                        )
                    }
                }
            }
        }
    }
}
