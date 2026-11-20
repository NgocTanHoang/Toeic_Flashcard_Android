package com.toeicflashcard.presentation.study.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.ui.glass.LiquidGlassCard
import com.toeicflashcard.core.ui.glass.rememberLiquidGlassBorderBrush
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalSm2RatingColors
import com.toeicflashcard.core.ui.theme.Sm2ColorPair
import com.toeicflashcard.domain.model.ReviewQuality

private val Sm2PillShape = RoundedCornerShape(20.dp)

@Composable
fun Sm2RatingBar(
    visible: Boolean,
    intervalPreviews: Map<ReviewQuality, String>,
    onSelectQuality: (ReviewQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    val sm2Colors = LocalSm2RatingColors.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Đánh giá mức độ ghi nhớ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewQuality.entries.forEach { quality ->
                    val palette = when (quality) {
                        ReviewQuality.AGAIN -> sm2Colors.again
                        ReviewQuality.HARD -> sm2Colors.hard
                        ReviewQuality.GOOD -> sm2Colors.good
                        ReviewQuality.EASY -> sm2Colors.easy
                    }
                    Sm2GlassPillButton(
                        label = quality.labelVi,
                        intervalHint = intervalPreviews[quality].orEmpty(),
                        colors = palette,
                        onClick = { onSelectQuality(quality) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Sm2GlassPillButton(
    label: String,
    intervalHint: String,
    colors: Sm2ColorPair,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalLiquidGlassColors.current
    LiquidGlassCard(
        onClick = onClick,
        shape = Sm2PillShape,
        containerColor = colors.container,
        borderWidth = if (glassColors.isDark) 0.75.dp else 1.dp,
        borderBrush = rememberLiquidGlassBorderBrush(glassColors, accentColor = colors.border),
        modifier = modifier.heightIn(min = 66.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = intervalHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.content.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
