package com.sharn.handpan.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharn.handpan.data.builtin.BuiltinExercises
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.LearningSkill
import com.sharn.handpan.model.PatternCategory
import com.sharn.handpan.model.PracticeInputMode
import com.sharn.handpan.ui.AppScreen
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.components.HandpanDiscView
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanBronzeDark
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight
import com.sharn.handpan.ui.theme.HandpanTerracotta

@Composable
fun HomeScreen(
    viewModel: HandpanViewModel,
    onNavigate: (AppScreen) -> Unit,
    onStartPractice: (HandpanPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    val appState by viewModel.appUiState.collectAsStateWithLifecycle()
    val allPatterns by viewModel.allPatterns.collectAsStateWithLifecycle()
    val recommendation by viewModel.nextPracticeRecommendation.collectAsStateWithLifecycle()
    val featuredPattern = BuiltinExercises.ALL_BUILTIN_PATTERNS.getOrNull(2)
        ?: BuiltinExercises.ALL_BUILTIN_PATTERNS.first()
    val recommendationPattern = allPatterns.firstOrNull { it.id == recommendation.patternId }
    var tappedNoteInHome by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Top App Bar / Greeting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "آموزش هنگ درام شارن",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sharn Handpan • ساز ۹ نت استاندارد (D Kurd)",
                        style = MaterialTheme.typography.bodySmall,
                        color = HandpanBronze
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.openOnboarding() },
                        modifier = Modifier.testTag("home_onboarding_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "راهنما", tint = HandpanGold)
                    }
                    IconButton(
                        onClick = { onNavigate(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = Color.LightGray)
                    }
                }
            }
        }

        recommendationPattern?.let { pattern ->
            item {
                RecommendationCard(
                    pattern = pattern,
                    recommendation = recommendation,
                    onStart = { onStartPractice(pattern) }
                )
            }
        }

        // Practice Mode Selection Banner (Real Handpan First Architecture)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "نحوه تمرین (Input Mode):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRealMode = (appState.defaultPracticeInputMode == PracticeInputMode.REAL_HANDPAN)
                        
                        // Real Handpan Option (Primary)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.5.dp,
                                    if (isRealMode) HandpanGold else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.setPracticeInputMode(PracticeInputMode.REAL_HANDPAN)
                                },
                            color = if (isRealMode) HandpanGold.copy(alpha = 0.2f) else CharcoalSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isRealMode) HandpanGold else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "ساز واقعی (اصلی)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRealMode) HandpanGoldLight else Color.White
                                    )
                                    Text(
                                        text = "تحلیل صوتی میکروفن",
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        // Virtual Handpan Option (Fallback)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.5.dp,
                                    if (!isRealMode) HandpanBronze else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.setPracticeInputMode(PracticeInputMode.VIRTUAL_HANDPAN)
                                },
                            color = if (!isRealMode) HandpanBronze.copy(alpha = 0.2f) else CharcoalSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = if (!isRealMode) HandpanBronze else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "ساز مجازی (کمکی)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isRealMode) HandpanGoldLight else Color.White
                                    )
                                    Text(
                                        text = "تمرین بدون ساز / لمسی",
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hero Continue Practice Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, HandpanBronze.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .clickable {
                        onStartPractice(featuredPattern)
                    }
                    .testTag("hero_practice_card"),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2C1D13),
                                    CharcoalSurface,
                                    CharcoalDark
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HandpanGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "آموزش گام‌به‌گام مبتدی",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HandpanGoldLight
                                )
                            }

                            Text(
                                text = "${featuredPattern.bpm} BPM",
                                style = MaterialTheme.typography.labelMedium,
                                color = HandpanBronze
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = featuredPattern.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "با ساز واقعی شروع کن؛ برنامه ضربه‌هایت را از طریق میکروفن می‌شنود و همراهت پیش می‌رود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6C8BB),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onStartPractice(featuredPattern)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HandpanGold),
                            modifier = Modifier.testTag("start_hero_practice_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CharcoalBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "شروع تمرین این الگو",
                                color = CharcoalBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Grid (Metronome, Masterclass, Ambience, Scales, Looper, Guide)
        item {
            val featuredProgress = viewModel.practiceStats.collectAsStateWithLifecycle().value[featuredPattern.id]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
                    .testTag("practice_progress_card"),
                colors = CardDefaults.cardColors(containerColor = CharcoalDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = HandpanGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "پیشرفت تمرین",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = featuredProgress?.let {
                                "${it.practiceCount} بار تمرین کرده‌ای • ${it.completedRounds} دور کامل"
                            } ?: "هنوز رکوردی ثبت نشده؛ اولین تمرینت را شروع کن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // Quick Actions Grid (Metronome, Masterclass, Ambience, Scales, Looper, Guide)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "مدرسه تعاملی",
                        subtitle = "۶ درس گام‌به‌گام",
                        icon = Icons.Default.School,
                        accentColor = HandpanGold,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openLessonStudioDialog() },
                        testTag = "quick_masterclass_card"
                    )

                    QuickActionCard(
                        title = "مربی ریتم و تپ",
                        subtitle = "سنجش میلی‌ثانیه‌ای ضرب",
                        icon = Icons.Default.Speed,
                        accentColor = Color(0xFF26A69A),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.RHYTHM_TRAINER) },
                        testTag = "quick_rhythm_trainer_card"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "مترونوم حرفه‌ای",
                        subtitle = "تنظیم دقیق ضرب و تمپو",
                        icon = Icons.Default.Timer,
                        accentColor = Color(0xFFFFB74D),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.METRONOME) },
                        testTag = "quick_metronome_card"
                    )

                    QuickActionCard(
                        title = "ضبط و لوپر زنده",
                        subtitle = "ثبت بداهه‌نوازی",
                        icon = Icons.Default.FiberManualRecord,
                        accentColor = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openRecorderDialog() },
                        testTag = "quick_recorder_card"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "گام‌ها و کوک‌ها",
                        subtitle = "کورد، سلتیک، پیگمی",
                        icon = Icons.Default.Layers,
                        accentColor = Color(0xFFAB47BC),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openScaleDialog() },
                        testTag = "quick_scales_card"
                    )

                    QuickActionCard(
                        title = "آناتومی و تکنیک‌ها",
                        subtitle = "راهنمای جامع ۱۰ تکنیک",
                        icon = Icons.Default.Info,
                        accentColor = Color(0xFF66BB6A),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openGuideDialog() },
                        testTag = "quick_guide_card"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "فضاسازی و بک‌گراند",
                        subtitle = "موسیقی متن برای تمرین",
                        icon = Icons.Default.Spa,
                        accentColor = Color(0xFF80CBC4),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openBackingTracksDialog() },
                        testTag = "quick_backing_tracks_card"
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Interactive Virtual Handpan Pad (Tap to Play)
        item {
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ساز مجازی (تمرین بدون ساز / تست صدا)",
                            style = MaterialTheme.typography.titleSmall,
                            color = HandpanGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (tappedNoteInHome) {
                                0 -> "نت دینگ مرکزی (D)"
                                9 -> "ضربه اسلپ شانه (S)"
                                in 1..8 -> "نت شماره $tappedNoteInHome"
                                else -> "روی هر نت ضربه بزنید"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = HandpanBronze
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HandpanDiscView(
                        activeNoteNumber = tappedNoteInHome,
                        onNoteTapped = { note ->
                            tappedNoteInHome = note
                            viewModel.playNoteDirect(note)
                        },
                        modifier = Modifier.size(260.dp),
                        isInteractive = true,
                        customSamplesMap = appState.customSamplesMap
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sampler Studio Shortcut
                    OutlinedButton(
                        onClick = {
                            viewModel.openSamplerDialog(if (tappedNoteInHome >= 0) tappedNoteInHome else 0)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = HandpanGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tappedNoteInHome >= 0) "ضبط صدای واقعی نت ($tappedNoteInHome)" else "کالیبراسیون و ضبط صدای هنگ‌درام شما",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Category Carousel Header & Items
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسته‌بندی تمرین‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.bodySmall,
                    color = HandpanGold,
                    modifier = Modifier.clickable { onNavigate(AppScreen.EXERCISE_LIBRARY) }
                )
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val categories = listOf(
                    Triple(PatternCategory.BEGINNER, "مبتدی (گام‌به‌گام)", Icons.Default.School),
                    Triple(PatternCategory.INDEPENDENCE, "استقلال دست و پارادیدل", Icons.Default.Psychology),
                    Triple(PatternCategory.RHYTHM, "ریتم و ضرب‌آهنگ", Icons.Default.Timer),
                    Triple(PatternCategory.MELODY, "ملودی‌های آرامش", Icons.Default.MusicNote),
                    Triple(PatternCategory.WARM_UP, "گرم کردن دست", Icons.Default.Speed),
                    Triple(PatternCategory.CUSTOM, "الگوهای من", Icons.Default.Tune)
                )

                items(categories) { (cat, title, icon) ->
                    CategoryBadgeCard(
                        title = title,
                        icon = icon,
                        onClick = {
                            viewModel.selectCategory(cat)
                            onNavigate(AppScreen.EXERCISE_LIBRARY)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun RecommendationCard(
    pattern: HandpanPattern,
    recommendation: com.sharn.handpan.model.LearningRecommendation,
    onStart: () -> Unit
) {
    val reason = when (recommendation.skill) {
        LearningSkill.NOTE_ACCURACY -> "برای تقویت دقت نت‌ها"
        LearningSkill.PITCH_ACCURACY -> "برای تقویت دقت زیر و بمی نت‌ها"
        LearningSkill.TIMING -> "برای تقویت زمان‌بندی اجرا"
        LearningSkill.RHYTHM -> "برای تقویت ریتم"
        LearningSkill.CONSISTENCY -> "برای تقویت ثبات اجرا"
        LearningSkill.SPEED -> "برای تقویت سرعت اجرا"
        LearningSkill.TECHNIQUE -> "برای تقویت تکنیک اجرا"
        LearningSkill.DYNAMICS -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HandpanGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .testTag("learning_recommendation_card"),
        colors = CardDefaults.cardColors(containerColor = CharcoalDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "پیشنهاد تمرین بعدی",
                style = MaterialTheme.typography.titleMedium,
                color = HandpanGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = pattern.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listOfNotNull(
                    pattern.category.persianTitle,
                    pattern.difficulty.persianLabel,
                    "${pattern.bpm} BPM"
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = HandpanBronze
            )
            reason?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = HandpanGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("learning_recommendation_start_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CharcoalBlack)
                Spacer(modifier = Modifier.width(6.dp))
                Text("شروع تمرین", color = CharcoalBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CategoryBadgeCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = CharcoalSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = HandpanGold, modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
