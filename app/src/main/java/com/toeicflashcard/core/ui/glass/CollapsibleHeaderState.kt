package com.toeicflashcard.core.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

val CollapsibleHeaderSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = Spring.StiffnessMediumLow
)

@Stable
class CollapsibleHeaderState(
    private val dockScrollController: DockScrollController,
    private val collapseThresholdPx: Float = 28f,
    private val expandThresholdPx: Float = 18f,
    private val expandOnBottomOverscroll: Boolean = true
) {
    var isCollapsed by mutableStateOf(false)
        private set

    private var accumulatedDelta = 0f

    fun expand() {
        accumulatedDelta = 0f
        if (isCollapsed) {
            isCollapsed = false
        }
        dockScrollController.show()
    }

    fun collapse() {
        accumulatedDelta = 0f
        if (!isCollapsed) {
            isCollapsed = true
        }
        dockScrollController.hide()
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val dy = consumed.y
            if (dy < -1f) {
                if (accumulatedDelta > 0f) accumulatedDelta = 0f
                accumulatedDelta += dy
                if (accumulatedDelta <= -collapseThresholdPx) {
                    collapse()
                }
            } else if (dy > 1f) {
                if (accumulatedDelta < 0f) accumulatedDelta = 0f
                accumulatedDelta += dy
                if (accumulatedDelta >= expandThresholdPx) {
                    expand()
                }
            }
            if (expandOnBottomOverscroll && available.y < -4f) {
                expand()
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberCollapsibleHeaderState(
    dockScrollController: DockScrollController,
    collapseThresholdPx: Float = 28f,
    expandThresholdPx: Float = 18f,
    expandOnBottomOverscroll: Boolean = true
): CollapsibleHeaderState {
    return remember(
        dockScrollController,
        collapseThresholdPx,
        expandThresholdPx,
        expandOnBottomOverscroll
    ) {
        CollapsibleHeaderState(
            dockScrollController = dockScrollController,
            collapseThresholdPx = collapseThresholdPx,
            expandThresholdPx = expandThresholdPx,
            expandOnBottomOverscroll = expandOnBottomOverscroll
        )
    }
}
