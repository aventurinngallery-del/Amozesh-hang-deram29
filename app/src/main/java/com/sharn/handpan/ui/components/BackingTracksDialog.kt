package com.sharn.handpan.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharn.handpan.ui.HandpanViewModel
import com.sharn.handpan.ui.theme.CharcoalDark
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight

data class AmbienceTrackInfo(
    val id: String,
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun BackingTracksDialog(
    viewModel: HandpanViewModel,
    onDismiss: () -> Unit
) {
    val tracks = listOf(
        AmbienceTrackInfo(
            id = "desert_drone",
            name = "پد درون دی مینور (Mystic Desert Drone)",
            desc = "هارمونیک‌های ممتد و گرم فرکانس D3 با نوسان ملایم LFO برای بداهه‌نوازی مدیتیشن",
            icon = Icons.Default.Spa,
            accentColor = HandpanGold
        ),
        AmbienceTrackInfo(
            id = "cajon_groove",
            name = "ریتم کاخن آکوستیک (Acoustic Cajon Pulse)",
            desc = "بک‌گراند پرکاشن آکوستیک با ضربات باس و اسلپ ریتمیک برای تمرین روی تمپوی ثابت",
            icon = Icons.Default.GraphicEq,
            accentColor = Color(0xFFFF9800)
        ),
        AmbienceTrackInfo(
            id = "rain_nature",
            name = "صدای باران و طبیعت (Nature & Rain Soundscape)",
            desc = "نویز صورتی سنتزشده و اتمسفر قطرات باران برای افزایش تمرکز و آرامش ذهن در تمرین",
            icon = Icons.Default.WaterDrop,
            accentColor = Color(0xFF29B6F6)
        )
    )

    var activeTrackId by remember { mutableStateOf(viewModel.ambienceEngine.getActiveTrackId()) }
    var volume by remember { mutableFloatStateOf(0.6f) }
    var tempoBpm by remember { mutableIntStateOf(80) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 660.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, HandpanBronze.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .testTag("backing_tracks_dialog"),
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
                            Icon(Icons.Default.Spa, contentDescription = null, tint = HandpanGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "موسیقی متن و فضاسازی (Backing Ambience)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سینتی‌سایزر درون، ریتم کاخن و اصوات طبیعت برای نوازندگی همزمان",
                                style = MaterialTheme.typography.bodySmall,
                                color = HandpanBronze
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Track Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tracks.forEach { track ->
                        val isCurrentPlaying = activeTrackId == track.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isCurrentPlaying) 2.dp else 1.dp,
                                    color = if (isCurrentPlaying) track.accentColor else CharcoalSurfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (isCurrentPlaying) {
                                        viewModel.ambienceEngine.stopAmbience()
                                        activeTrackId = null
                                    } else {
                                        viewModel.ambienceEngine.startAmbience(track.id, bpm = tempoBpm)
                                        activeTrackId = track.id
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentPlaying) CharcoalSurface else CharcoalSurface.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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
                                            .background(track.accentColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(track.icon, contentDescription = null, tint = track.accentColor, modifier = Modifier.size(20.dp))
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isCurrentPlaying) HandpanGoldLight else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = track.desc,
                                            fontSize = 11.sp,
                                            color = Color(0xFFD6C8BB),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentPlaying) track.accentColor else CharcoalDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isCurrentPlaying) "توقف" else "پخش",
                                        tint = if (isCurrentPlaying) CharcoalDark else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Volume and Controls
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
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = HandpanGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("میزان صدای پس‌زمینه", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${(volume * 100).toInt()}%", color = HandpanGoldLight, fontSize = 12.sp)
                        }

                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                viewModel.ambienceEngine.setVolume(it)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = HandpanGold,
                                activeTrackColor = HandpanGold,
                                inactiveTrackColor = CharcoalDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (activeTrackId != null) {
                        Button(
                            onClick = {
                                viewModel.ambienceEngine.stopAmbience()
                                activeTrackId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2410C))
                        ) {
                            Text("توقف پخش صدا", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

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
