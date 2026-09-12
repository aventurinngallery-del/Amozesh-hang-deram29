package com.sharn.handpan.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

@Composable
fun AudioInstructionsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, HandpanBronze.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("audio_instructions_dialog"),
            color = CharcoalDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "راهنمای موتور صوتی و سمپل‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        color = HandpanGold,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Current Synthesizer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(HandpanGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = HandpanGoldLight, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                            Text(
                                text = "موتور سنتز فیزیکی (Active Synthesizer)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "برنامه در حال حاضر از مدل‌سازی فیزیکی آکوستیک هارمونیک (فرکانس اصلی + هارمونیک اکتاو + کوک پنجم ترکیبی + رزونانس هلمهولتز برای دینگ) با نرخ نمونه‌برداری 44.1kHz استفاده می‌کند که تأخیر ناچیز (Zero-Latency) و کیفیت بالا را بدون نیاز به دانلود فراهم می‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6C8BB),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: How to replace with custom WAV samples
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(HandpanBronze.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = HandpanBronze, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                            Text(
                                text = "نحوه افزودن سمپل‌های ضبط‌شده واقعی",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "برای جایگزینی با فایل‌های صوتی ضبط شده از هنگدرام اختصاصی خود، فایل‌های WAV استاندارد ۱۶ بیتی را با نام‌های زیر در مسیر پوشه قرار دهید:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6C8BB)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CharcoalBlack)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "app/src/main/assets/audio/\n" +
                                        "├── note_0.wav  (دینگ مرکز - Ding D3)\n" +
                                        "├── note_1.wav  (نت ۱ - A3)\n" +
                                        "├── note_2.wav  (نت ۲ - Bb3)\n" +
                                        "├── note_3.wav  (نت ۳ - C4)\n" +
                                        "├── note_4.wav  (نت ۴ - D4)\n" +
                                        "├── note_5.wav  (نت ۵ - E4)\n" +
                                        "├── note_6.wav  (نت ۶ - F4)\n" +
                                        "├── note_7.wav  (نت ۷ - G4)\n" +
                                        "├── note_8.wav  (نت ۸ - A4)\n" +
                                        "└── note_9.wav  (ضربه اسلپ - Slap/Tak)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = HandpanGoldLight,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_instructions_close_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HandpanGold)
                ) {
                    Text("متوجه شدم", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
