package com.toeicflashcard.core.ui.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppWindowMetricsTest {

    @Test
    fun `phone portrait is compact and not landscape short`() {
        val metrics = AppWindowMetrics(widthDp = 411.dp, heightDp = 928.dp)
        assertFalse(metrics.isExpandedWidth)
        assertFalse(metrics.isLandscapeShort)
    }

    @Test
    fun `phone landscape and split screen are landscape short`() {
        val phoneLandscape = AppWindowMetrics(widthDp = 928.dp, heightDp = 411.dp)
        assertFalse(phoneLandscape.isExpandedWidth)
        assertTrue(phoneLandscape.isLandscapeShort)

        val splitScreen = AppWindowMetrics(widthDp = 680.dp, heightDp = 600.dp)
        assertTrue(splitScreen.isExpandedWidth)
        assertTrue(splitScreen.isLandscapeShort)
    }

    @Test
    fun `z fold inner 20_18 is expanded and not landscape short`() {
        // 1968x2184 @ 374dpi => ~841.9dp x 934.3dp
        val widthDp = (1968f / (374f / 160f)).dp
        val heightDp = (2184f / (374f / 160f)).dp
        val metrics = AppWindowMetrics(widthDp = widthDp, heightDp = heightDp)
        assertTrue(metrics.isExpandedWidth)
        assertFalse(metrics.isLandscapeShort)
    }

    @Test
    fun `oppo pad se 7_5 portrait is expanded and not landscape short`() {
        // 1600x2240 @ 280dpi => ~914.3dp x 1280dp
        val widthDp = (1600f / (280f / 160f)).dp
        val heightDp = (2240f / (280f / 160f)).dp
        val metrics = AppWindowMetrics(widthDp = widthDp, heightDp = heightDp)
        assertTrue(metrics.isExpandedWidth)
        assertFalse(metrics.isLandscapeShort)
    }

    @Test
    fun `redmi pad 2 16_10 landscape is expanded and not landscape short`() {
        // 2560x1600 @ 274dpi => ~1494.9dp x 934.3dp
        val widthDp = (2560f / (274f / 160f)).dp
        val heightDp = (1600f / (274f / 160f)).dp
        val metrics = AppWindowMetrics(widthDp = widthDp, heightDp = heightDp)
        assertTrue(metrics.isExpandedWidth)
        assertFalse(metrics.isLandscapeShort)
    }

    @Test
    fun `adaptiveHorizontalPadding centers 1100dp content symmetrically`() {
        val sidePad = adaptiveHorizontalPadding(
            availableWidth = 1495.dp,
            maxContentWidth = MainContentMaxWidth,
            basePadding = 18.dp
        )
        // (1495 - 1100) / 2 + 18 = 197.5 + 18 = 215.5
        assertEquals(215.5f, sidePad.value, 0.1f)
    }

    @Test
    fun `adaptive spacing tokens scale cleanly across phone, fold, and landscape tablet`() {
        assertEquals(12.dp, screenHorizontalPadding(345.dp))
        assertEquals(16.dp, screenHorizontalPadding(411.dp))
        assertEquals(22.dp, screenHorizontalPadding(842.dp))
        assertEquals(28.dp, screenHorizontalPadding(914.dp))
        assertEquals(40.dp, screenHorizontalPadding(1280.dp))

        assertEquals(14.dp, sectionVerticalSpacing(411.dp))
        assertEquals(22.dp, sectionVerticalSpacing(842.dp))

        assertEquals(10.dp, gridItemSpacing(411.dp))
        assertEquals(14.dp, gridItemSpacing(842.dp))
        assertEquals(16.dp, gridItemSpacing(914.dp))

        assertEquals(16.dp, cardInnerPadding(411.dp))
        assertEquals(22.dp, cardInnerPadding(842.dp))
    }
}
