package com.toeicflashcard.core.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toeicflashcard.core.ui.theme.LocalGlassOpacity
import com.toeicflashcard.core.ui.theme.LocalLiquidGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

enum class MainDockTab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Trang chủ", Icons.Rounded.Home),
    VAULT("Kho từ", Icons.AutoMirrored.Rounded.MenuBook),
    PRACTICE("Luyện tập", Icons.Rounded.Quiz),
    STATS("Thống kê", Icons.Rounded.BarChart)
}

@Stable
class DockScrollController {
    var isVisible by mutableStateOf(true)
        private set

    private var accumulatedDelta = 0f

    fun show() {
        accumulatedDelta = 0f
        if (!isVisible) {
            isVisible = true
        }
    }

    fun hide() {
        accumulatedDelta = 0f
        if (isVisible) {
            isVisible = false
        }
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (dy < -1f) {
                if (accumulatedDelta > 0f) accumulatedDelta = 0f
                accumulatedDelta += dy
                if (accumulatedDelta <= -24f) {
                    hide()
                }
            } else if (dy > 1f) {
                if (accumulatedDelta < 0f) accumulatedDelta = 0f
                accumulatedDelta += dy
                if (accumulatedDelta >= 16f) {
                    show()
                }
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // When hitting the bottom edge (user drags up but list cannot scroll further down), reveal Dock.
            if (available.y < -4f) {
                show()
            }
            return Offset.Zero
        }
    }
}

private val DockHideSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Floating pill-shaped Apple Liquid Glass bottom dock.
 * Auto-hide translation runs 100% on Draw-phase via [graphicsLayer] (0 recompositions),
 * and blurs live scrolling content behind it via fixed [hazeChild] (`blurRadius = 18.dp`).
 */
@Composable
fun LiquidGlassBottomDock(
    selectedTab: MainDockTab,
    onSelectTab: (MainDockTab) -> Unit,
    visible: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val glassOpacity = LocalGlassOpacity.current
    val glassColors = LocalLiquidGlassColors.current

    var measuredHeightPx by remember { mutableIntStateOf(0) }
    val extraSlidePx = with(LocalDensity.current) { 28.dp.toPx() }
    val hideProgressState = animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = DockHideSpring,
        label = "floating_dock_visibility"
    )

    val dockTint = if (glassColors.isDark) {
        glassColors.dialogSheetFill.copy(alpha = glassOpacity)
    } else {
        Color(0xFFF6FAFF).copy(alpha = (glassOpacity * 0.90f).coerceIn(0.55f, 0.96f))
    }

    val dockHazeStyle = remember(glassColors.hazeBackground, glassColors.isDark, dockTint) {
        HazeStyle(
            backgroundColor = glassColors.hazeBackground,
            tint = HazeTint(dockTint),
            blurRadius = 18.dp,
            noiseFactor = 0.0f,
            fallbackTint = HazeTint(
                dockTint.copy(alpha = if (glassColors.isDark) 0.92f else 0.90f)
            )
        )
    }

    val sheenModifier = if (glassColors.sheenBrush != null) {
        Modifier.background(glassColors.sheenBrush)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .onSizeChanged { measuredHeightPx = it.height }
            .graphicsLayer {
                val p = hideProgressState.value
                val distance = (measuredHeightPx + extraSlidePx).coerceAtLeast(extraSlidePx)
                translationY = p * distance
                alpha = (1f - p * 0.35f).coerceIn(0f, 1f)
                val scale = 1f - p * 0.04f
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .hazeChild(
                    state = hazeState,
                    style = dockHazeStyle
                )
                .then(sheenModifier)
                .border(
                    width = 0.75.dp,
                    brush = glassColors.defaultBorderBrush,
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainDockTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val pillFill by animateColorAsState(
                    targetValue = when {
                        isSelected && glassColors.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "dock_tab_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "dock_tab_fg"
                )
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .widthIn(min = 66.dp)
                        .clip(CircleShape)
                        .background(pillFill)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 0.6.dp,
                                    brush = glassColors.defaultBorderBrush,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .semantics { selected = isSelected }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = contentColor),
                            role = Role.Tab,
                            onClick = { onSelectTab(tab) }
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
