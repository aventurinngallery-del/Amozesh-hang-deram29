package com.sharn.handpan.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharn.handpan.data.builtin.BuiltinExercises
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.PracticeInputMode
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

import androidx.compose.material.icons.filled.Star
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class LessonItem(
    val id: String,
    val title: String,
    val levelName: String,
    val patternId: String,
    val description: String,
    val stepsSequence: List<Int>,
    val tips: List<String>
)

val MASTER_LESSONS = listOf(
    LessonItem(
        id = "lesson_1_ding",
        title = "درس ۱: تسلط بر دینگ مرکزی و ضربه جهشی",
        levelName = "مبتدی ۱",
        patternId = "beg_01_single_ding",
        description = "اصول پرتاب دست (Touch & Bounce) و تولید طنین غنی و زنگ‌دار دینگ D3 بدون خفه شدن صدا.",
        stepsSequence = listOf(0, 0, 0, 0),
        tips = listOf("ضربه باید زیر ۵ میلی‌ثانیه دست را از فلز جدا کند", "از بند اول انگشت شست یا انگشت وسط استفاده کنید")
    ),
    LessonItem(
        id = "lesson_2_zigzag",
        title = "درس ۲: گردش متناوب دست‌ها روی گام زیگزاگی",
        levelName = "مبتدی ۲",
        patternId = "beg_05_scale_eight_notes",
        description = "پیمایش منظم نت‌های ۱ تا ۸ با جابجایی چپ و راست برای استقرار حافظه عضلانی دست‌ها.",
        stepsSequence = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8),
        tips = listOf("دست راست روی نت‌های فرد (۱، ۳، ۵، ۷)", "دست چپ روی نت‌های زوج (۲، ۴، ۶، ۸)")
    ),
    LessonItem(
        id = "lesson_3_groove44",
        title = "درس ۳: ریتم پایه ۴/۴ و ضربه اسلپ بدنه",
        levelName = "مبتدی ۳",
        patternId = "beg_04_intro_to_slap",
        description = "تلفیق نوای ملودیک دینگ و نت‌های جانبی با ضربه کوبه‌ای اسلپ (S) روی بدنه فلزی.",
        stepsSequence = listOf(0, 1, 9, 2),
        tips = listOf("ضربه اسلپ را روی فضای مسطح بین نت‌ها بزنید", "ریتم را آرام و شمرده با مترونوم هماهنگ کنید")
    ),
    LessonItem(
        id = "lesson_4_persian68",
        title = "درس ۴: ریتم اصیل ایرانی ۶/۸ (شش و هشت)",
        levelName = "متوسط ۱",
        patternId = "rhy_03_six_eight_groove",
        description = "نواختن تریپلت‌های ریتمیک شش هشت ایرانی با تاکیدهای داینامیک گوش‌نواز.",
        stepsSequence = listOf(0, 1, 2, 0, 3, 4),
        tips = listOf("تاکید (Accent) را روی ضرب‌های اول و چهارم قرار دهید", "حس شناور و رقصان ضربات را حفظ کنید")
    ),
    LessonItem(
        id = "lesson_5_arpeggio",
        title = "درس ۵: آرپژهای ملودیک و سه‌صدایی",
        levelName = "متوسط ۲",
        patternId = "beg_03_triad_arpeggio",
        description = "ایجاد بافت هارمونیک پیوسته و آکوردهای تجزیه‌شده روی درجات گام دی کورد.",
        stepsSequence = listOf(0, 1, 3, 5, 3, 1),
        tips = listOf("توالی نت‌ها را یکنواخت و بدون شتاب بنوازید", "اجازه دهید طنین نت قبلی در فضا جاری بماند")
    ),
    LessonItem(
        id = "lesson_6_song_wind",
        title = "درس ۶: قطعه کامل «نجوای باد» (Whisper of the Wind)",
        levelName = "پیشرفته",
        patternId = "mel_01_syncopated_speed",
        description = "اجرای یک قطعه موسیقی کامل با ترکیب تکنیک‌های دینگ، آرپژ، اسلپ و ملودی فراز و فرود.",
        stepsSequence = listOf(0, 1, 2, 1, 3, 4, 3, 9, 0, 5, 6, 5),
        tips = listOf("پویایی و احساس نوازندگی را در اولویت قرار دهید", "قبل از نواختن سریع، روی تمپوی ۵۰ تمرین کنید")
    )
)

@Composable
fun InteractiveLessonStudioDialog(
    viewModel: HandpanViewModel,
    onStartPractice: (HandpanPattern, PracticeInputMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLesson by remember { mutableStateOf(MASTER_LESSONS.first()) }
    val progressMap by viewModel.lessonProgressMap.collectAsStateWithLifecycle()

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
                .testTag("interactive_lesson_studio_dialog"),
            colors = CardDefaults.cardColors(containerColor = CharcoalDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
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
                                .background(HandpanGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = HandpanGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مدرسه تعاملی هنگ‌درام (Masterclass Studio)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "۶ درس گام‌به‌گام از مبانی ارگونومی تا اجرای قطعه کامل",
                                style = MaterialTheme.typography.bodySmall,
                                color = HandpanBronze
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lesson Selector List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(MASTER_LESSONS) { lesson ->
                        val isSelected = lesson.id == selectedLesson.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) HandpanGold else CharcoalSurfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedLesson = lesson }
                                .testTag("lesson_card_${lesson.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CharcoalSurface else CharcoalSurface.copy(alpha = 0.6f)
                            )
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
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(HandpanGold.copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = lesson.levelName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = HandpanGoldLight
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = lesson.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) HandpanGoldLight else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = HandpanGold, modifier = Modifier.size(18.dp))
                                    }
                                }

                                val progress = progressMap[lesson.id]
                                if (progress != null && progress.isCompleted) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row {
                                            repeat(3) { index ->
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (index < progress.stars) HandpanGold else Color.DarkGray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "بهترین امتیاز: ${progress.bestScore}٪ (${progress.attempts} بار تمرین)",
                                            fontSize = 10.sp,
                                            color = HandpanGoldLight
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = lesson.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFFD6C8BB),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Steps preview badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("توالی نت‌ها:", fontSize = 10.sp, color = Color.Gray)
                                    lesson.stepsSequence.forEach { stepNote ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (stepNote == 0) HandpanGold.copy(alpha = 0.3f) else CharcoalDark)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (stepNote == 0) "D" else if (stepNote == 9) "S" else "$stepNote",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (stepNote == 0) HandpanGoldLight else Color.LightGray
                                            )
                                        }
                                    }
                                }

                                // Tips checklist
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CharcoalDark)
                                            .padding(10.dp)
                                    ) {
                                        Text("نکات طلایی استاد:", color = HandpanGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        lesson.tips.forEach { tip ->
                                            Text("• $tip", color = Color(0xFFD6C8BB), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Real Handpan Practice Button (Primary)
                        Button(
                            onClick = {
                                val pattern = BuiltinExercises.ALL_BUILTIN_PATTERNS.find { it.id == selectedLesson.patternId }
                                    ?: BuiltinExercises.ALL_BUILTIN_PATTERNS.first()
                                onDismiss()
                                onStartPractice(pattern, PracticeInputMode.REAL_HANDPAN)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HandpanGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("lesson_start_real_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = CharcoalDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تمرین با ساز واقعی",
                                color = CharcoalDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Virtual Handpan Practice Button (Fallback)
                        Button(
                            onClick = {
                                val pattern = BuiltinExercises.ALL_BUILTIN_PATTERNS.find { it.id == selectedLesson.patternId }
                                    ?: BuiltinExercises.ALL_BUILTIN_PATTERNS.first()
                                onDismiss()
                                onStartPractice(pattern, PracticeInputMode.VIRTUAL_HANDPAN)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("lesson_start_virtual_button")
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = HandpanGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تمرین بدون ساز (مجازی)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("بستن", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
