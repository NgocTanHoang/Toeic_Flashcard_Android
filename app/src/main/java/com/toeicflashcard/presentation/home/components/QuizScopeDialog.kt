package com.toeicflashcard.presentation.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.FocusedContentMaxWidth
import com.toeicflashcard.core.ui.glass.LiquidGlassButton
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.LiquidGlassDialogSurface
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.TopicProgress
import com.toeicflashcard.domain.model.VocabularyDecks
import dev.chrisbanes.haze.HazeState

private val ScopeOptionCardShape = RoundedCornerShape(18.dp)

@Composable
fun QuizScopeDialog(
    visible: Boolean,
    hazeState: HazeState,
    topics: List<TopicProgress>,
    selectedDeckName: String = "Tất cả bộ từ",
    title: String = "Chọn phạm vi kiểm tra",
    subtitle: String = "Kiểm tra nhanh tối đa 10 câu trắc nghiệm nghĩa tiếng Việt:",
    onDismiss: () -> Unit,
    onSelectScope: (String?) -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current

    BackHandler(enabled = visible) {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glassColors.sheetScrim)
                    .semantics { contentDescription = "Đóng hộp thoại chọn phạm vi" }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(tween(160)) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(durationMillis = 160)
            ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LiquidGlassDialogSurface(
                hazeState = hazeState,
                blurRadius = 20.dp,
                modifier = Modifier
                    .widthIn(max = FocusedContentMaxWidth)
                    .fillMaxWidth(0.90f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(
                            key = "__all__",
                            contentType = "all_topics"
                        ) {
                            val onSelectAll = remember(onSelectScope) { { onSelectScope(null) } }
                            val allTitle = if (selectedDeckName == VocabularyDecks.ALL_NAME) {
                                "Toàn bộ từ vựng"
                            } else {
                                "Toàn bộ $selectedDeckName"
                            }
                            ScopeOptionGlassCard(
                                title = allTitle,
                                subtitle = "Trộn ngẫu nhiên từ tất cả ${topics.size} chủ đề",
                                highlight = true,
                                onClick = onSelectAll
                            )
                        }

                        items(
                            items = topics,
                            key = { it.topic },
                            contentType = { "scope_option" }
                        ) { topic ->
                            val onSelectTopic = remember(topic.topic, onSelectScope) {
                                { onSelectScope(topic.topic) }
                            }
                            ScopeOptionGlassCard(
                                title = topic.topic,
                                subtitle = "${topic.totalCount} từ vựng trong chủ đề",
                                highlight = false,
                                onClick = onSelectTopic
                            )
                        }
                    }

                    LiquidGlassButton(
                        onClick = onDismiss,
                        tintColor = if (glassColors.isDark) {
                            Color.White.copy(alpha = 0.10f)
                        } else {
                            Color.White.copy(alpha = 0.55f)
                        },
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        borderColor = null,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Đóng",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeOptionGlassCard(
    title: String,
    subtitle: String,
    highlight: Boolean,
    onClick: () -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val fill = when {
        highlight && glassColors.isDark -> Color.White.copy(alpha = 0.10f)
        highlight && !glassColors.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        glassColors.isDark -> Color.White.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.56f)
    }

    LiquidGlassCard(
        onClick = onClick,
        pressScaleEnabled = false,
        shape = ScopeOptionCardShape,
        containerColor = fill,
        borderBrush = rememberLiquidGlassBorderBrush(
            glassColors = glassColors,
            accentColor = if (highlight && !glassColors.isDark) MaterialTheme.colorScheme.primary else null
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = when {
                    glassColors.isDark && highlight -> MaterialTheme.colorScheme.primary
                    glassColors.isDark -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
