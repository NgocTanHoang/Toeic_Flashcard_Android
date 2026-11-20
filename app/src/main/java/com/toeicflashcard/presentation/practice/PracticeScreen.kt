package com.toeicflashcard.presentation.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

private val PracticeCardShape = RoundedCornerShape(26.dp)
private val PracticeSpeedShape = RoundedCornerShape(22.dp)

@Composable
fun PracticeScreen(
    dueTodayCount: Int,
    totalCount: Int,
    speechRate: Float,
    headerHeightDp: Dp,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    onOpenStandardQuiz: () -> Unit,
    onOpenListeningQuiz: () -> Unit,
    onToggleSpeechRate: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val rateLabel = if (speechRate > 0.85f) "1.0x (Chuẩn)" else "0.75x (Chậm)"

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = headerHeightDp + 12.dp,
            bottom = 108.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "__practice_quiz__", contentType = "practice_mode") {
            PracticeModeGlassCard(
                icon = Icons.Rounded.Quiz,
                badge = "10 câu • Trắc nghiệm",
                title = "Trắc nghiệm nhanh",
                description = "Chọn nghĩa tiếng Việt đúng cho từ vựng ngẫu nhiên theo từng chủ đề hoặc toàn bộ $totalCount từ, hỗ trợ đánh dấu từ sai vào diện ôn gấp.",
                buttonText = "Chọn phạm vi & Bắt đầu",
                primaryHighlight = true,
                accentColor = MaterialTheme.colorScheme.primary,
                onStart = onOpenStandardQuiz
            )
        }

        item(key = "__practice_listening__", contentType = "practice_mode") {
            PracticeModeGlassCard(
                icon = Icons.Rounded.Headphones,
                badge = "Mới • Phản xạ nghe TTS",
                title = "Luyện nghe từ vựng",
                description = "Ẩn mặt chữ tiếng Anh và phiên âm, tự động phát âm qua Text-to-Speech để bạn luyện phản xạ nghe rồi chọn nhanh nghĩa tiếng Việt.",
                buttonText = "Chọn phạm vi & Luyện nghe",
                primaryHighlight = false,
                accentColor = MaterialTheme.colorScheme.secondary,
                onStart = onOpenListeningQuiz
            )
        }

        item(key = "__practice_speed__", contentType = "practice_settings") {
            LiquidGlassCard(
                shape = PracticeSpeedShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Tốc độ phát âm luyện nghe",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Còn $dueTodayCount từ đến hạn hôm nay • Đang đặt: $rateLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LiquidGlassButton(
                        onClick = onToggleSpeechRate,
                        tintColor = if (glassColors.isDark) {
                            Color.White.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        },
                        contentColor = if (glassColors.isDark) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        borderColor = if (glassColors.isDark) null else MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (speechRate > 0.85f) "1.0x" else "0.75x",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item(key = "__practice_nav_spacer__", contentType = "nav_spacer") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun FloatingPracticeHeader(
    selectedDeckName: String,
    totalCount: Int,
    contentHazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    val headerTint = if (glassColors.isDark) {
        glassColors.hazeBackground.copy(alpha = glassOpacity)
    } else {
        Color(0xFFF4F8FF).copy(alpha = (glassOpacity * 0.85f).coerceIn(0.45f, 0.95f))
    }
    val headerHazeStyle = remember(glassColors.hazeBackground, glassColors.isDark, headerTint) {
        HazeStyle(
            backgroundColor = glassColors.hazeBackground,
            tint = HazeTint(headerTint),
            blurRadius = 16.dp,
            noiseFactor = 0.0f,
            fallbackTint = HazeTint(
                headerTint.copy(alpha = if (glassColors.isDark) 0.92f else 0.88f)
            )
        )
    }

    Column(
        modifier = modifier
            .hazeChild(
                state = contentHazeState,
                style = headerHazeStyle
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Luyện tập",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$selectedDeckName ($totalCount từ) • Trắc nghiệm & Luyện nghe",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(glassColors.divider)
        )
    }
}

@Composable
private fun PracticeModeGlassCard(
    icon: ImageVector,
    badge: String,
    title: String,
    description: String,
    buttonText: String,
    primaryHighlight: Boolean,
    accentColor: Color,
    onStart: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val iconCircleBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.09f)
    } else {
        accentColor.copy(alpha = 0.14f)
    }
    val iconTint = if (glassColors.isDark) MaterialTheme.colorScheme.primary else accentColor

    LiquidGlassCard(
        shape = PracticeCardShape,
        containerColor = glassColors.elevatedGlassFill,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg)
                        .border(
                            width = if (glassColors.isDark) 0.75.dp else 1.dp,
                            brush = glassColors.defaultBorderBrush,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (glassColors.isDark) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val btnTint = when {
                primaryHighlight && glassColors.isDark -> MaterialTheme.colorScheme.primary
                glassColors.isDark -> Color.White.copy(alpha = 0.11f)
                else -> accentColor.copy(alpha = 0.20f)
            }
            val btnFg = when {
                primaryHighlight && glassColors.isDark -> MaterialTheme.colorScheme.onPrimary
                glassColors.isDark -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface
            }

            LiquidGlassButton(
                onClick = onStart,
                tintColor = btnTint,
                contentColor = btnFg,
                borderColor = if (glassColors.isDark) null else accentColor,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
