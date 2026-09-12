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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharn.handpan.model.HandpanTechnique
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

@Composable
fun HandpanGuideDialog(
    onDismiss: () -> Unit
) {
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
                .testTag("handpan_guide_dialog"),
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
                            Icon(Icons.Default.Info, contentDescription = null, tint = HandpanGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "راهنمای جامع آناتومی و تکنیک‌های هنگ‌درام",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اصول آکوستیک، ارگونومی ضربه، جنس فلز و تکنیک‌های نوازندگی",
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

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: Anatomy
                    GuideSectionCard(
                        title = "۱. ساختار و آناتومی فیزیکی ساز",
                        icon = Icons.Default.MusicNote,
                        accentColor = HandpanGold
                    ) {
                        AnatomyBullet(
                            term = "دینگ (Ding):",
                            desc = "گنبد یا فرورفتگی مرکزی در پوسته فوقانی که بم‌ترین نت و فرکانس پایه هلمهولتز ساز را تولید می‌کند."
                        )
                        AnatomyBullet(
                            term = "فیلدهای صوتی (Tone Fields):",
                            desc = "نواحی بیضوی کوک‌شده دور دینگ که هر یک شامل ۳ مود هارمونیک (فرکانس اصلی، اکتاو و فاصله پنجم) هستند."
                        )
                        AnatomyBullet(
                            term = "دیمپل (Dimple):",
                            desc = "فرورفتگی یا برآمدگی مرکزی در وسط هر فیلد صوتی که باعث پایداری کوک و کنترل هارمونیک‌ها می‌شود."
                        )
                        AnatomyBullet(
                            term = "پورت گو (Gu Port):",
                            desc = "دهانه گرد زیرین ساز که نقش رزوناتور کاویتی بم (صدای وووش و بیس هلمهولتز) را ایفا می‌کند."
                        )
                    }

                    // Section 2: Touch & Bounce Ergonomics
                    GuideSectionCard(
                        title = "۲. ارگونومی ضربه جهشی (Touch & Bounce)",
                        icon = Icons.Default.TouchApp,
                        accentColor = Color(0xFF4CAF50)
                    ) {
                        Text(
                            text = "راز صدای زنگ‌دار، شفاف و پرطنین هنگ‌درام در «عدم توقف دست روی فلز» است. ضربه باید مانند لمس یک قابلمه داغ باشد؛ در کسری از ثانیه (زیر ۵ میلی‌ثانیه) پس از اصابت، دست را به سمت بالا پرتاب کنید تا فلز آزادانه نوسان کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6C8BB),
                            lineHeight = 20.sp
                        )
                    }

                    // Section 3: Steel Materials
                    GuideSectionCard(
                        title = "۳. مقایسه استیل نیترید شده در برابر ضدزنگ (Stainless)",
                        icon = Icons.Default.Info,
                        accentColor = Color(0xFF2196F3)
                    ) {
                        Text(
                            text = "• استیل نیترید (Nitrided): جنس سرامیکی، ضربه کوبه‌ای و پانچ عمیق با سستین (ماندگاری صدا) کنترل‌شده، ایده‌آل برای ریتم‌های سریع و پرکاسیو.\n• استیل ضدزنگ (Stainless/Ember): طنین ممتد، بسیار درخشان و طولانی، فوق‌العاده برای ملودی‌های آرام، مراقبه و آرپژهای احساسی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6C8BB),
                            lineHeight = 20.sp
                        )
                    }

                    // Section 4: Techniques Encyclopedia
                    GuideSectionCard(
                        title = "۴. دانشنامه ۱۰ تکنیک اصلی هنگ‌درام",
                        icon = Icons.Default.MusicNote,
                        accentColor = Color(0xFFFF9800)
                    ) {
                        HandpanTechnique.values().forEach { tech ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CharcoalDark)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${tech.persianName} (${tech.notationSymbol})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = HandpanGoldLight
                                    )
                                    Text(
                                        text = tech.englishName,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tech.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFFC0B3A5),
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Dismiss Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "متوجه شدم، بستن راهنما",
                            color = HandpanGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun AnatomyBullet(
    term: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            color = HandpanGold,
            fontWeight = FontWeight.Bold
        )
        Column {
            Text(
                text = term,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = desc,
                color = Color(0xFFD6C8BB),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
