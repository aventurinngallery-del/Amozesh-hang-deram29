package com.sharn.handpan.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

data class OnboardingStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tip: String
)

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OnboardingStep(
            stepNumber = 1,
            title = "ساختار هندپن استاندارد ۹ نت (D Kurd)",
            description = "ساز شما دارای ۱ نت در مرکز به نام «دینگ (D)»، ۸ نت ملودیک در دور ساز (۱ تا ۸) و تکنیک کوبه‌ای اسلپ (S) است.",
            icon = Icons.Default.PanTool,
            tip = "دینگ در مرکز بم‌ترین و طنین‌اندازترین نت ساز است."
        ),
        OnboardingStep(
            stepNumber = 2,
            title = "نت‌نویسی عددی و چینش زیگزاگ",
            description = "نت‌ها به ترتیب زیر و بم با شماره‌های ۱ تا ۸ نام‌گذاری شده‌اند و بین دست چپ و راست به صورت متناوب تقسیم می‌شوند.",
            icon = Icons.Default.Numbers,
            tip = "نت‌های فرد سمت چپ و نت‌های زوج سمت راست قرار دارند."
        ),
        OnboardingStep(
            stepNumber = 3,
            title = "تکنیک اسلپ (S / Tak)",
            description = "دایره شانه ساز بین دینگ و نت‌ها برای ضربات کوبه‌ای بدون طنین (اسلپ) است که ریتم‌های بسیار پرانرژی خلق می‌کند.",
            icon = Icons.Default.MusicNote,
            tip = "در تبلچر و تمرین‌ها این ضربه با حرف S مشخص می‌شود."
        ),
        OnboardingStep(
            stepNumber = 4,
            title = "هماهنگی دقیق با مترونوم",
            description = "مترونوم ضربان قلب نوازندگی هندپن است. ضرب اول هر میزان با صدای مشخص‌تر و چراغ چشمک‌زن همراهی‌تان می‌کند.",
            icon = Icons.Default.Timer,
            tip = "تمرکز کنید ضربه دستتان دقیقاً روی پالس مترونوم بنشیند."
        ),
        OnboardingStep(
            stepNumber = 5,
            title = "تمرین آرام و روان‌سازی دست‌ها",
            description = "ابتدا با سرعت‌های ملایم (مثلاً ۶۰ BPM) تمرین کنید تا رفلکس دست‌ها شکل بگیرد و سپس سرعت را افزایش دهید.",
            icon = Icons.Default.Speed,
            tip = "روزانه ۱۰ تا ۱۵ دقیقه تمرین پیوسته بهترین نتیجه را به همراه دارد."
        )
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, HandpanBronze.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .testTag("onboarding_dialog"),
            color = CharcoalDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "آموزش اولیه و راهنمای شروع",
                    style = MaterialTheme.typography.titleMedium,
                    color = HandpanGold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Step Counter Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStepIndex) 28.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentStepIndex) HandpanGold
                                    else if (index < currentStepIndex) HandpanBronze
                                    else CharcoalBorder
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == currentStepIndex) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalBlack
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Card with icon and text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(HandpanBronze.copy(alpha = 0.2f))
                                .border(1.dp, HandpanGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentStep.icon,
                                contentDescription = null,
                                tint = HandpanGoldLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = currentStep.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentStep.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD6C8BB),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tip box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CharcoalBlack.copy(alpha = 0.7f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "💡 نکته: ${currentStep.tip}",
                                style = MaterialTheme.typography.bodySmall,
                                color = HandpanGoldLight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_prev_button")
                        ) {
                            Text("قبلی", color = HandpanBronze)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    if (currentStepIndex < steps.size - 1) {
                        Button(
                            onClick = { currentStepIndex++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HandpanGold),
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text("مرحله بعد", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HandpanGold),
                            modifier = Modifier.testTag("onboarding_start_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = CharcoalBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("شروع نوازندگی!", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
