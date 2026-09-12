package com.sharn.handpan.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharn.handpan.model.NotationSystem
import com.sharn.handpan.model.NotePitchConfig
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.components.AudioInstructionsDialog
import com.sharn.handpan.ui.theme.CharcoalBlack
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

@Composable
fun SettingsScreen(
    viewModel: HandpanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appState by viewModel.appUiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var showAudioDialog by remember { mutableStateOf(false) }

    if (showAudioDialog) {
        AudioInstructionsDialog(onDismiss = { showAudioDialog = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen")
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
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
            }

            Text(
                text = "تنظیمات برنامه",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notation System Selector Card (Part 4-7)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = HandpanGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سیستم نت‌نویسی (Notation System)", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "شیوه نمایش نت‌ها در آموزش و تمرین را انتخاب کنید (تمام الگوها به صورت خودکار تبدیل می‌شوند):",
                        color = Color(0xFFD6C8BB),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NotationSystem.values().forEach { notation ->
                        val isSelected = (notation == appState.preferredNotationSystem)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HandpanGold.copy(alpha = 0.2f) else CharcoalSurfaceVariant)
                                .border(1.dp, if (isSelected) HandpanGold else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setNotationPreference(notation) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = notation.persianTitle,
                                        color = if (isSelected) HandpanGoldLight else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = notation.englishTitle,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Text("فعال ✓", color = HandpanGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Custom Real Handpan Sampler Studio Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = HandpanGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("کالیبراسیون و ضبط ساز واقعی", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        val customCount = appState.customSamplesMap.values.count { it }
                        Surface(
                            shape = CircleShape,
                            color = if (customCount > 0) Color(0xFF2E7D32) else CharcoalSurfaceVariant
                        ) {
                            Text(
                                text = if (customCount > 0) "$customCount نت ضبط شده" else "صدای پیش‌فرض",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "می‌توانید صدای تک‌تک نت‌های هنگ‌درام فیزیکی خود را از طریق میکروفن ضبط کنید تا تمام آموزش‌ها، مترونوم و الگوها دقیقاً با صدای ساز شما پخش شوند.",
                        color = Color(0xFFD6C8BB),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.openSamplerDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = HandpanGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = CharcoalBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ورود به استودیوی ضبط و کالیبراسیون",
                            color = CharcoalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Volume Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = HandpanGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("میزان بلندی صدا (Volume)", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("صدای ساز هنگدرام: ${(appState.masterVolume * 100).toInt()}٪", color = Color.LightGray, fontSize = 12.sp)
                    Slider(
                        value = appState.masterVolume,
                        onValueChange = { viewModel.setMasterVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = HandpanGold, activeTrackColor = HandpanGold)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("صدای کلیک مترونوم: ${(appState.metronomeVolume * 100).toInt()}٪", color = Color.LightGray, fontSize = 12.sp)
                    Slider(
                        value = appState.metronomeVolume,
                        onValueChange = { viewModel.setMetronomeVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = HandpanBronze, activeTrackColor = HandpanBronze)
                    )
                }
            }

            // Handpan Tuning & Scale Config Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = HandpanGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("کوک و فرکانس نتها (Scale Tuning)", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "سیستم آموزش بر پایه اعداد ۱ تا ۸ است، اما صدای ساز را می‌توانید با کوک فیزیکی هنگدرام خود منطبق کنید:",
                        color = Color(0xFFD6C8BB),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NotePitchConfig.SCALES.forEach { scale ->
                        val isSelected = (scale.scaleName == appState.currentScaleConfig.scaleName)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HandpanGold.copy(alpha = 0.2f) else CharcoalSurfaceVariant)
                                .border(1.dp, if (isSelected) HandpanGold else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setScaleTuning(scale) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scale.scaleName,
                                    color = if (isSelected) HandpanGoldLight else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )

                                if (isSelected) {
                                    Text("فعال ✓", color = HandpanGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Toggles Card (Haptics, Dark Mode)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ویبره و بازخورد لمسی (Haptic)", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("لرزش ظریف در ضربات مترونوم و نت‌ها", color = HandpanBronze, fontSize = 11.sp)
                        }

                        Switch(
                            checked = appState.isHapticEnabled,
                            onCheckedChange = { viewModel.toggleHaptic() },
                            colors = SwitchDefaults.colors(checkedThumbColor = HandpanGold)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تم روشن برنامه (Theme)", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("تغییر بین حالت تاریک و روشن", color = HandpanBronze, fontSize = 11.sp)
                        }

                        Switch(
                            checked = appState.darkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(checkedThumbColor = HandpanGold)
                        )
                    }
                }
            }

            // Audio Samples & Guide Dialog Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingActionRow(
                        title = "راهنمای فایل‌های صوتی و سمپل‌ها",
                        subtitle = "توضیح موتور سنتز و نحوه افزودن فایل‌های WAV",
                        icon = Icons.Default.Folder,
                        onClick = { showAudioDialog = true }
                    )

                    SettingActionRow(
                        title = "مشاهده مجدد آموزش اولیه",
                        subtitle = "راهنمای گام‌به‌گام ۵ مرحله‌ای برای مبتدیان",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        onClick = { viewModel.openOnboarding() }
                    )
                }
            }

            // About Application Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = HandpanBronze)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("درباره اپلیکیشن", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "آموزش هنگ درام شارن (Sharn Handpan) • نسخه ۱.۰.۰\n" +
                                "طراحی‌شده بر پایه سیستم نت‌نویسی عددی (۱ تا ۸) برای یادگیری سریع و بی‌دردسر، همراه با موتور زمان‌بندی دقیق نانومتری و سنتز فیزیکی آکوستیک.",
                        color = Color(0xFFD6C8BB),
                        fontSize = 12.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HandpanBronze.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = HandpanGold, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
