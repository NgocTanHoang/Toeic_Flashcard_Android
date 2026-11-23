package com.toeicflashcard.presentation.study.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.LiquidGlassIconButton
import com.toeicflashcard.core.ui.glass.LocalAppWindowMetrics
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import com.toeicflashcard.domain.model.VocabularyWord

private val CrystalCardShape = RoundedCornerShape(32.dp)
private val ExampleBoxShape = RoundedCornerShape(22.dp)
private val ShortCardHeightBreakpoint = 780.dp

internal data class FlashcardLayoutMetrics(
    val isWide: Boolean,
    val isShortHeight: Boolean,
    val isFoldInner: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val sectionSpacing: Dp,
    val exampleInnerPadding: Dp
)

internal fun computeFlashcardLayoutMetrics(
    screenWidthDp: Dp,
    screenHeightDp: Dp,
    isTabletLandscape: Boolean = false
): FlashcardLayoutMetrics {
    val isWide = screenWidthDp >= AdaptiveWideBreakpoint
    val isFoldInner = screenWidthDp >= AdaptiveWideBreakpoint && screenWidthDp < 900.dp && !isTabletLandscape
    val isShortHeight = screenWidthDp < AdaptiveWideBreakpoint && screenHeightDp < ShortCardHeightBreakpoint && !isTabletLandscape
    return FlashcardLayoutMetrics(
        isWide = isWide,
        isShortHeight = isShortHeight,
        isFoldInner = isFoldInner,
        horizontalPadding = when {
            isTabletLandscape -> 36.dp
            isWide -> 28.dp
            else -> 20.dp
        },
        verticalPadding = when {
            isTabletLandscape -> 36.dp
            isShortHeight -> 16.dp
            isWide -> 28.dp
            else -> 20.dp
        },
        sectionSpacing = if (isShortHeight) 10.dp else 16.dp,
        exampleInnerPadding = if (isShortHeight) 14.dp else 18.dp
    )
}

@Composable
fun Flashcard3D(
    word: VocabularyWord,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakExample: () -> Unit,
    modifier: Modifier = Modifier,
    isTabletLandscape: Boolean = false
) {
    val glassColors = LocalLiquidGlassColors.current
    val rotationState = animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "flashcard_rotation"
    )
    val showBack by remember { derivedStateOf { rotationState.value > 90f } }
    val density = LocalDensity.current.density
    val sheenModifier = if (glassColors.sheenBrush != null) {
        Modifier.background(glassColors.sheenBrush)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotationState.value
                cameraDistance = 14f * density
            }
            .clip(CrystalCardShape)
            .background(glassColors.elevatedGlassFill)
            .then(sheenModifier)
            .border(
                width = if (glassColors.isDark) 0.75.dp else 1.25.dp,
                brush = glassColors.defaultBorderBrush,
                shape = CrystalCardShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlip
            )
    ) {
        if (!showBack) {
            FlashcardFrontFace(
                word = word,
                onSpeakWord = onSpeakWord,
                isTabletLandscape = isTabletLandscape
            )
        } else {
            FlashcardBackFace(
                word = word,
                onSpeakWord = onSpeakWord,
                onSpeakExample = onSpeakExample,
                isTabletLandscape = isTabletLandscape,
                modifier = Modifier.graphicsLayer {
                    rotationY = 180f
                }
            )
        }
    }
}

@Composable
private fun FlashcardFrontFace(
    word: VocabularyWord,
    onSpeakWord: () -> Unit,
    isTabletLandscape: Boolean = false
) {
    val glassColors = LocalLiquidGlassColors.current
    val config = LocalConfiguration.current
    val windowMetrics = LocalAppWindowMetrics.current
    val effectiveWidth = maxOf(config.screenWidthDp.dp, windowMetrics.widthDp)
    val effectiveHeight = minOf(config.screenHeightDp.dp, windowMetrics.heightDp)
    val layoutMetrics = computeFlashcardLayoutMetrics(effectiveWidth, effectiveHeight, isTabletLandscape)
    val pillBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color.White.copy(alpha = 0.56f)
    }
    val borderWidth = if (glassColors.isDark) 0.75.dp else 0.8.dp
    val accentFg = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = layoutMetrics.horizontalPadding,
                vertical = layoutMetrics.verticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(borderWidth, glassColors.defaultBorderBrush, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = word.topic,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(borderWidth, glassColors.defaultBorderBrush, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = word.partOfSpeech,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = word.word,
                style = if (layoutMetrics.isFoldInner) {
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 40.sp
                    )
                } else {
                    MaterialTheme.typography.headlineLarge
                },
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (word.phonetic.isNotBlank()) {
                Text(
                    text = word.phonetic,
                    style = if (layoutMetrics.isFoldInner) {
                        MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp)
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Medium,
                    color = accentFg,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(if (layoutMetrics.isShortHeight) 8.dp else 14.dp))

            val speakerBg = MaterialTheme.colorScheme.primary.copy(
                alpha = if (glassColors.isDark) 0.14f else 0.16f
            )
            val speakerFg = MaterialTheme.colorScheme.primary
            LiquidGlassIconButton(
                onClick = onSpeakWord,
                size = if (layoutMetrics.isFoldInner) 64.dp else 60.dp,
                containerColor = speakerBg,
                contentColor = speakerFg
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Phát âm từ ${word.word}",
                    modifier = Modifier.size(if (layoutMetrics.isFoldInner) 30.dp else 28.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Chạm vào thẻ để xem nghĩa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun FlashcardBackFace(
    word: VocabularyWord,
    onSpeakWord: () -> Unit,
    onSpeakExample: () -> Unit,
    modifier: Modifier = Modifier,
    isTabletLandscape: Boolean = false
) {
    val glassColors = LocalLiquidGlassColors.current
    val config = LocalConfiguration.current
    val windowMetrics = LocalAppWindowMetrics.current
    val effectiveWidth = maxOf(config.screenWidthDp.dp, windowMetrics.widthDp)
    val effectiveHeight = minOf(config.screenHeightDp.dp, windowMetrics.heightDp)
    val layoutMetrics = computeFlashcardLayoutMetrics(effectiveWidth, effectiveHeight, isTabletLandscape)
    val exampleWashBg = if (glassColors.isDark) {
        Color.White.copy(alpha = 0.055f)
    } else {
        Color(0xFFF0F5FC)
    }
    val exampleBorderColor = if (glassColors.isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    } else {
        Color(0xFFCFDDF0)
    }
    val accentFg = MaterialTheme.colorScheme.primary
    val dividerBrush = remember(glassColors.divider) {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                glassColors.divider,
                Color.Transparent
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = layoutMetrics.horizontalPadding,
                vertical = layoutMetrics.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(layoutMetrics.sectionSpacing)
    ) {
        // Zone 1: Word + IPA + POS + compact speaker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = word.word,
                    style = if (layoutMetrics.isFoldInner) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            lineHeight = 36.sp
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
                val backMeta = if (word.phonetic.isNotBlank()) {
                    "${word.phonetic}  •  ${word.partOfSpeech}"
                } else {
                    word.partOfSpeech
                }
                Text(
                    text = backMeta,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentFg
                )
            }

            LiquidGlassIconButton(
                onClick = onSpeakWord,
                size = if (layoutMetrics.isFoldInner) 44.dp else 36.dp,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Phát âm từ ${word.word}",
                    modifier = Modifier.size(if (layoutMetrics.isFoldInner) 22.dp else 18.dp)
                )
            }
        }

        // Zone 2: Divider + Vietnamese Meaning centered in remaining space & independently scrollable on short viewports
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val centerMinHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = centerMinHeight)
                    .padding(top = if (isTabletLandscape) 12.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(
                    if (layoutMetrics.isShortHeight) 8.dp else 12.dp,
                    if (isTabletLandscape || layoutMetrics.isShortHeight) Alignment.Top else Alignment.CenterVertically
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(dividerBrush)
                )

                Column(verticalArrangement = Arrangement.spacedBy(if (layoutMetrics.isShortHeight) 4.dp else 8.dp)) {
                    Text(
                        text = "NGHĨA TIẾNG VIỆT",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = word.meaningVi,
                        style = if (isTabletLandscape) {
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 36.sp
                            )
                        } else if (layoutMetrics.isFoldInner) {
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 25.sp,
                                lineHeight = 33.sp
                            )
                        } else if (layoutMetrics.isWide) {
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                lineHeight = 32.sp
                            )
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Zone 3: Context Example Card pinned cleanly at bottom without overlapping meaning
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ExampleBoxShape)
                .background(exampleWashBg)
                .border(
                    width = if (glassColors.isDark) 0.85.dp else 1.dp,
                    color = exampleBorderColor,
                    shape = ExampleBoxShape
                )
                .padding(layoutMetrics.exampleInnerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ví dụ ngữ cảnh",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (word.exampleEn.isNotBlank()) {
                        LiquidGlassIconButton(
                            onClick = onSpeakExample,
                            size = if (layoutMetrics.isFoldInner) 44.dp else 40.dp,
                            containerColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (glassColors.isDark) 0.14f else 0.16f
                            ),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Đọc câu ví dụ",
                                modifier = Modifier.size(if (layoutMetrics.isFoldInner) 22.dp else 20.dp)
                            )
                        }
                    }
                }

                if (word.exampleEn.isNotBlank()) {
                    Text(
                        text = word.exampleEn,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (word.exampleVi.isNotBlank()) {
                        Text(
                            text = word.exampleVi,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Chưa có câu ví dụ cho từ này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
