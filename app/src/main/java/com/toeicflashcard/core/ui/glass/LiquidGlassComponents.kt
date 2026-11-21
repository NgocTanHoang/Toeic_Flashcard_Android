package com.toeicflashcard.core.ui.glass

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.toeicflashcard.core.ui.theme.LiquidGlassColors
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

private val CardDefaultShape = RoundedCornerShape(24.dp)
private val DialogDefaultShape = RoundedCornerShape(28.dp)
private val SheetDefaultShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

private val PressSpringSpec = spring<Float>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Light-refraction specular border: bright top-left edge fading softly toward bottom-right.
 */
fun liquidGlassBorderBrush(
    glassColors: LiquidGlassColors,
    accentColor: Color? = null
): Brush {
    if (accentColor == null) return glassColors.defaultBorderBrush
    val startColor = accentColor.copy(
        alpha = if (glassColors.isDark) 0.38f else 0.72f
    )
    val midColor = accentColor.copy(
        alpha = if (glassColors.isDark) 0.14f else 0.30f
    )
    val endColor = accentColor.copy(
        alpha = if (glassColors.isDark) 0.05f else 0.18f
    )
    return Brush.linearGradient(
        0.0f to startColor,
        0.48f to midColor,
        1.0f to endColor,
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

/**
 * Returns cached theme default border brush or remembers an accent-tinted border brush.
 */
@Composable
fun rememberLiquidGlassBorderBrush(
    glassColors: LiquidGlassColors,
    accentColor: Color? = null
): Brush {
    if (accentColor == null) return glassColors.defaultBorderBrush
    return remember(glassColors, accentColor) {
        liquidGlassBorderBrush(glassColors, accentColor)
    }
}

/**
 * Soft diagonal highlight sheen across Light Mode glass surfaces.
 */
fun liquidGlassSheenBrush(glassColors: LiquidGlassColors): Brush {
    return glassColors.sheenBrush ?: Brush.linearGradient(
        colors = listOf(glassColors.sheenStart, glassColors.sheenEnd),
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

/**
 * Reusable Liquid Glass card combining:
 * 1) Translucent glass fill + optional [hazeChild] blur for fixed overlays,
 * 2) Thin cached light-refraction border (`0.75.dp` in Dark Mode, `1.dp` in Light Mode),
 * 3) Zero-recomposition draw-phase spring bounce when [onClick] and [pressScaleEnabled] are active.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: Shape = CardDefaultShape,
    containerColor: Color = LocalLiquidGlassColors.current.surfaceFill,
    blurRadius: Dp = 16.dp,
    borderWidth: Dp = if (LocalLiquidGlassColors.current.isDark) 0.75.dp else 1.dp,
    borderBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    pressScaleEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val resolvedBorder = borderBrush ?: glassColors.defaultBorderBrush

    val glassSurfaceModifier = if (hazeState != null) {
        val effectiveBlur = blurRadius.coerceIn(16.dp, 20.dp)
        val fallbackAlpha = if (glassColors.isDark) {
            0.90f
        } else {
            (containerColor.alpha + 0.28f).coerceAtMost(0.92f)
        }
        val hazeStyle = remember(
            glassColors.hazeBackground,
            containerColor,
            effectiveBlur,
            fallbackAlpha
        ) {
            HazeStyle(
                backgroundColor = glassColors.hazeBackground,
                tint = HazeTint(containerColor),
                blurRadius = effectiveBlur,
                noiseFactor = 0.0f,
                fallbackTint = HazeTint(containerColor.copy(alpha = fallbackAlpha))
            )
        }
        Modifier.hazeChild(
            state = hazeState,
            style = hazeStyle
        )
    } else {
        Modifier.background(containerColor)
    }

    val applySheen = hazeState != null || onClick == null || pressScaleEnabled
    val sheenModifier = if (applySheen && glassColors.sheenBrush != null) {
        Modifier.background(glassColors.sheenBrush)
    } else {
        Modifier
    }

    val cardModifier = when {
        onClick == null -> {
            modifier
                .clip(shape)
                .then(glassSurfaceModifier)
                .then(sheenModifier)
                .border(width = borderWidth, brush = resolvedBorder, shape = shape)
        }
        !pressScaleEnabled -> {
            val interactionSource = remember { MutableInteractionSource() }
            modifier
                .clip(shape)
                .then(glassSurfaceModifier)
                .then(sheenModifier)
                .border(width = borderWidth, brush = resolvedBorder, shape = shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    enabled = enabled,
                    onClick = onClick
                )
        }
        else -> {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scaleState = animateFloatAsState(
                targetValue = if (enabled && isPressed) 0.97f else 1f,
                animationSpec = PressSpringSpec,
                label = "glass_card_scale"
            )
            modifier
                .graphicsLayer {
                    val s = scaleState.value
                    scaleX = s
                    scaleY = s
                }
                .clip(shape)
                .then(glassSurfaceModifier)
                .then(sheenModifier)
                .border(width = borderWidth, brush = resolvedBorder, shape = shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    enabled = enabled,
                    onClick = onClick
                )
        }
    }

    Box(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Pill-shaped Liquid Glass action button featuring translucent glass fill,
 * thin light-refraction border, and draw-phase spring physics bounce on press.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    tintColor: Color = if (LocalLiquidGlassColors.current.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    },
    contentColor: Color = if (LocalLiquidGlassColors.current.isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    },
    borderColor: Color? = if (LocalLiquidGlassColors.current.isDark) {
        null
    } else {
        Color.White.copy(alpha = 0.85f)
    },
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.97f else 1f,
        animationSpec = PressSpringSpec,
        label = "glass_button_scale"
    )

    val effectiveAlpha = if (enabled) 1f else 0.50f
    val activeTint = tintColor.copy(alpha = tintColor.alpha * effectiveAlpha)

    val blurModifier = if (hazeState != null) {
        val hazeStyle = remember(glassColors.hazeBackground, glassColors.isDark, activeTint) {
            if (glassColors.isDark) {
                HazeStyle(
                    backgroundColor = glassColors.hazeBackground,
                    tint = HazeTint(activeTint),
                    blurRadius = 16.dp,
                    noiseFactor = 0.0f,
                    fallbackTint = HazeTint(activeTint)
                )
            } else {
                val lightOverlay = Color.White.copy(alpha = 0.32f)
                HazeStyle(
                    backgroundColor = glassColors.hazeBackground,
                    tints = listOf(HazeTint(lightOverlay), HazeTint(activeTint)),
                    blurRadius = 16.dp,
                    noiseFactor = 0.0f,
                    fallbackTint = HazeTint(activeTint.copy(alpha = (activeTint.alpha + 0.35f).coerceAtMost(0.92f)))
                )
            }
        }
        Modifier.hazeChild(
            state = hazeState,
            style = hazeStyle
        )
    } else {
        if (glassColors.isDark) {
            Modifier.background(activeTint)
        } else {
            Modifier
                .background(Color.White.copy(alpha = 0.32f))
                .background(activeTint)
        }
    }

    val sheenModifier = if (glassColors.sheenBrush != null) {
        Modifier.background(glassColors.sheenBrush)
    } else {
        Modifier
    }

    val borderWidth = if (glassColors.isDark) 0.75.dp else 1.dp
    val borderBrush = rememberLiquidGlassBorderBrush(glassColors, accentColor = borderColor)

    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
                alpha = effectiveAlpha
            }
            .clip(shape)
            .then(blurModifier)
            .then(sheenModifier)
            .border(width = borderWidth, brush = borderBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Circular frosted glass icon button for top bars, speaker playback, and compact controls.
 */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    hazeState: HazeState? = null,
    containerColor: Color = if (LocalLiquidGlassColors.current.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
    },
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalLiquidGlassColors.current.isDark
    LiquidGlassCard(
        modifier = modifier.size(size),
        hazeState = hazeState,
        shape = CircleShape,
        containerColor = containerColor,
        blurRadius = 16.dp,
        borderWidth = if (isDark) 0.75.dp else 1.dp,
        onClick = onClick,
        enabled = enabled
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}

/**
 * Ultra-thin horizontal hairline progress indicator drawn purely in the draw phase
 * (zero relayout or recomposition during animated progress transitions).
 */
@Composable
fun LiquidGlassHairlineProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 2.dp,
    trackColor: Color = LocalLiquidGlassColors.current.hairlineTrack,
    progressBrush: Brush = LocalLiquidGlassColors.current.hairlineProgressBrush,
    animated: Boolean = false
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgressState = if (animated) {
        animateFloatAsState(
            targetValue = clamped,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            label = "hairline_progress"
        )
    } else {
        null
    }

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val radius = size.height * 0.5f
                val cornerRadius = CornerRadius(radius, radius)
                drawRoundRect(
                    color = trackColor,
                    size = size,
                    cornerRadius = cornerRadius
                )
                val fraction = animatedProgressState?.value ?: clamped
                if (fraction > 0f) {
                    drawRoundRect(
                        brush = progressBrush,
                        size = Size(width = size.width * fraction, height = size.height),
                        cornerRadius = cornerRadius
                    )
                }
            }
    )
}

/**
 * Enables native window behind-blur and background-blur on Android 12+ (API 31+)
 * inside Dialog windows without re-applying on every recomposition.
 */
@Composable
fun ApplyDialogWindowBlur(blurRadius: Dp = 18.dp) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val view = LocalView.current
        val blurPx = with(LocalDensity.current) {
            blurRadius.coerceIn(16.dp, 20.dp).roundToPx()
        }
        LaunchedEffect(view, blurPx) {
            runCatching {
                val window = (view.parent as? DialogWindowProvider)?.window
                window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window?.attributes = window?.attributes?.apply {
                    blurBehindRadius = blurPx
                }
                window?.setBackgroundBlurRadius(blurPx)
            }
        }
    }
}

/**
 * Translucent frosted/smoked glass container tailored for Dialogs across the app.
 */
@Composable
fun LiquidGlassDialogSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Dp = 20.dp,
    shape: Shape = DialogDefaultShape,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val resolvedBlurRadius = blurRadius.coerceIn(16.dp, 20.dp)
    val sheetFill = glassColors.dialogSheetFill
    val sheenModifier = if (glassColors.sheenBrush != null) {
        Modifier.background(glassColors.sheenBrush)
    } else {
        Modifier
    }
    val surfaceModifier = if (hazeState != null) {
        val hazeStyle = remember(glassColors.hazeBackground, sheetFill, resolvedBlurRadius) {
            HazeStyle(
                backgroundColor = glassColors.hazeBackground,
                tint = HazeTint(sheetFill),
                blurRadius = resolvedBlurRadius,
                noiseFactor = 0.0f,
                fallbackTint = HazeTint(sheetFill)
            )
        }
        Modifier.hazeChild(
            state = hazeState,
            style = hazeStyle
        )
    } else {
        Modifier.background(sheetFill)
    }
    val borderWidth = if (glassColors.isDark) 0.75.dp else 1.dp

    if (hazeState == null) {
        ApplyDialogWindowBlur(blurRadius = resolvedBlurRadius)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(surfaceModifier)
            .then(sheenModifier)
            .border(width = borderWidth, brush = glassColors.defaultBorderBrush, shape = shape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Translucent frosted/smoked glass container tailored for in-window sheets with bespoke pill drag handle.
 */
@Composable
fun LiquidGlassSheetSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalLiquidGlassColors.current
    val resolvedBlurRadius = blurRadius.coerceIn(16.dp, 20.dp)
    val sheetFill = glassColors.dialogSheetFill
    val sheenModifier = if (glassColors.sheenBrush != null) {
        Modifier.background(glassColors.sheenBrush)
    } else {
        Modifier
    }
    val surfaceModifier = if (hazeState != null) {
        val hazeStyle = remember(glassColors.hazeBackground, sheetFill, resolvedBlurRadius) {
            HazeStyle(
                backgroundColor = glassColors.hazeBackground,
                tint = HazeTint(sheetFill),
                blurRadius = resolvedBlurRadius,
                noiseFactor = 0.0f,
                fallbackTint = HazeTint(sheetFill)
            )
        }
        Modifier.hazeChild(
            state = hazeState,
            style = hazeStyle
        )
    } else {
        Modifier.background(sheetFill)
    }
    val borderWidth = if (glassColors.isDark) 0.75.dp else 1.dp

    if (hazeState == null) {
        ApplyDialogWindowBlur(blurRadius = resolvedBlurRadius)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetDefaultShape)
            .then(surfaceModifier)
            .then(sheenModifier)
            .border(width = borderWidth, brush = glassColors.defaultBorderBrush, shape = SheetDefaultShape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
            content()
        }
    }
}

/**
 * Shared frosted glass capsule search field used across Home and Vocabulary Vault headers.
 */
@Composable
fun LiquidGlassSearchCapsule(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    clearContentDescription: String = "Xóa tìm kiếm",
    hazeState: HazeState? = null,
    floatingBlurRadius: Dp = 18.dp
) {
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current
    val fill = if (glassColors.isDark) {
        glassColors.surfaceFill
    } else {
        Color.White.copy(alpha = (glassOpacity * 0.78f).coerceIn(0.42f, 0.92f))
    }

    val backgroundModifier = if (hazeState != null) {
        val pillTint = if (glassColors.isDark) {
            glassColors.dialogSheetFill.copy(alpha = (glassOpacity * 0.92f).coerceIn(0.65f, 0.94f))
        } else {
            Color(0xFFF6FAFF).copy(alpha = (glassOpacity * 0.88f).coerceIn(0.62f, 0.94f))
        }
        val blur = floatingBlurRadius.coerceIn(16.dp, 20.dp)
        val hazeStyle = remember(glassColors.hazeBackground, pillTint, blur, glassColors.isDark) {
            HazeStyle(
                backgroundColor = glassColors.hazeBackground,
                tint = HazeTint(pillTint),
                blurRadius = blur,
                noiseFactor = 0.0f,
                fallbackTint = HazeTint(
                    pillTint.copy(alpha = if (glassColors.isDark) 0.92f else 0.90f)
                )
            )
        }
        val sheenModifier = if (glassColors.sheenBrush != null) {
            Modifier.background(glassColors.sheenBrush)
        } else {
            Modifier
        }
        Modifier
            .hazeChild(
                state = hazeState,
                style = hazeStyle
            )
            .then(sheenModifier)
    } else {
        Modifier.background(fill)
    }

    val borderWidth = if (hazeState != null || glassColors.isDark) 0.75.dp else 1.dp

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .then(backgroundModifier)
            .border(
                width = borderWidth,
                brush = glassColors.defaultBorderBrush,
                shape = CircleShape
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = clearContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
