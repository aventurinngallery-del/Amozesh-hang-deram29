package com.sharn.handpan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.NotationRenderer
import com.sharn.handpan.model.NotationSystem
import com.sharn.handpan.model.PracticeMode
import com.sharn.handpan.ui.theme.BeatDownbeatColor
import com.sharn.handpan.ui.theme.BeatRegularColor
import com.sharn.handpan.ui.theme.CharcoalBorder
import com.sharn.handpan.ui.theme.CharcoalSurface
import com.sharn.handpan.ui.theme.CharcoalSurfaceVariant
import com.sharn.handpan.ui.theme.HandpanBronze
import com.sharn.handpan.ui.theme.HandpanGold
import com.sharn.handpan.ui.theme.HandpanGoldLight
import com.sharn.handpan.ui.theme.RestColor
import com.sharn.handpan.ui.theme.getNoteColor

@Composable
fun NoteTimelineView(
    pattern: HandpanPattern,
    currentNoteIndex: Int,
    currentBeatInBar: Double,
    currentBar: Int,
    mode: PracticeMode,
    notationSystem: NotationSystem,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(currentNoteIndex) {
        if (currentNoteIndex >= 0 && pattern.events.isNotEmpty()) {
            val totalNotes = pattern.events.size
            if (totalNotes > 4) {
                val maxScroll = scrollState.maxValue
                val targetScroll = (maxScroll.toFloat() * (currentNoteIndex.toFloat() / (totalNotes - 1).toFloat())).toInt()
                scrollState.animateScrollTo(targetScroll)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp))
            .testTag("note_timeline_view"),
        color = CharcoalSurface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: Bar and Beat Tracker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "میزان $currentBar از ${pattern.bars}",
                    style = MaterialTheme.typography.labelMedium,
                    color = HandpanBronze
                )

                // Current Beat Progress Indicator (e.g. 1 — 2 — 3 — 4)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (b in 1..pattern.timeSignature.beatsPerBar) {
                        val isCurrentBeat = (b == currentBeatInBar.toInt())
                        val isDownbeat = (b == 1)
                        val beatColor by animateColorAsState(
                            targetValue = when {
                                isCurrentBeat && isDownbeat -> BeatDownbeatColor
                                isCurrentBeat -> BeatRegularColor
                                else -> CharcoalBorder
                            },
                            label = "beatColor"
                        )

                        Box(
                            modifier = Modifier
                                .size(if (isCurrentBeat) 26.dp else 20.dp)
                                .clip(CircleShape)
                                .background(beatColor)
                                .border(
                                    1.dp,
                                    if (isCurrentBeat) Color.White else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$b",
                                fontSize = if (isCurrentBeat) 13.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentBeat) Color.Black else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Note Events Sequence (Scannable Cards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pattern.events.forEachIndexed { index, event ->
                    val isCurrent = (index == currentNoteIndex)
                    val isPast = (index < currentNoteIndex)
                    val isHiddenInChallenge = (mode == PracticeMode.CHALLENGE && !isCurrent && !isPast)

                    NoteEventCard(
                        event = event,
                        index = index,
                        isCurrent = isCurrent,
                        isHidden = isHiddenInChallenge,
                        mode = mode,
                        notationSystem = notationSystem
                    )

                    if (index < pattern.events.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(
                                    if (isPast) HandpanGold.copy(alpha = 0.5f)
                                    else CharcoalBorder
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEventCard(
    event: NoteEvent,
    index: Int,
    isCurrent: Boolean,
    isHidden: Boolean,
    mode: PracticeMode,
    notationSystem: NotationSystem
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val noteColor = if (event.isRest) RestColor else getNoteColor(event.noteNumber)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrent && !event.isRest -> noteColor
            isCurrent && event.isRest -> RestColor
            else -> CharcoalSurfaceVariant
        },
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrent -> Color.White
            event.accent -> HandpanGoldLight
            else -> CharcoalBorder
        },
        label = "border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .padding(horizontal = 4.dp)
    ) {
        // Accent indicator
        if (event.accent) {
            Text(
                text = "▲",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) HandpanGoldLight else HandpanBronze,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Main Note Number Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(backgroundColor)
                .border(if (isCurrent) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isHidden) {
                Text(
                    text = "?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            } else if (event.isRest) {
                Text(
                    text = "𝄽",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White else Color.LightGray
                )
            } else {
                Text(
                    text = NotationRenderer.render(
                        noteNumber = event.noteNumber,
                        technique = event.technique,
                        system = notationSystem
                    ),
                    fontSize = if (event.isDing || event.isSlap) 20.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isCurrent) Color.Black else Color.White
                )
            }
        }

        // Hand indicator or beat position
        val handLabel = when (event.hand?.uppercase()) {
            "R" -> "راست (R)"
            "L" -> "چپ (L)"
            else -> event.hand
        }

        Text(
            text = if (event.isRest) "سکوت" else (handLabel ?: "ضرب ${event.beatPosition.toInt() + 1}"),
            fontSize = 10.sp,
            fontWeight = if (event.hand != null) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isCurrent -> HandpanGoldLight
                event.hand != null -> Color(0xFFFFD54F)
                else -> Color.Gray
            },
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
