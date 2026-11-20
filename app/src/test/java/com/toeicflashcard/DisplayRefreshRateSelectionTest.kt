package com.toeicflashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayRefreshRateSelectionTest {

    @Test
    fun `selectHighestRefreshRateModeId chooses highest refresh rate matching current resolution`() {
        val candidates = listOf(
            DisplayModeCandidate(modeId = 2, physicalWidth = 1080, physicalHeight = 2436, refreshRate = 60.0f),
            DisplayModeCandidate(modeId = 1, physicalWidth = 1080, physicalHeight = 2436, refreshRate = 120.00001f),
            DisplayModeCandidate(modeId = 3, physicalWidth = 1440, physicalHeight = 3200, refreshRate = 144.0f)
        )

        val selected = selectHighestRefreshRateModeId(
            currentWidth = 1080,
            currentHeight = 2436,
            candidates = candidates
        )

        assertEquals(1, selected)
    }

    @Test
    fun `selectHighestRefreshRateModeId returns null when no mode matches resolution`() {
        val candidates = listOf(
            DisplayModeCandidate(modeId = 1, physicalWidth = 1440, physicalHeight = 3200, refreshRate = 120.0f)
        )

        assertNull(
            selectHighestRefreshRateModeId(
                currentWidth = 1080,
                currentHeight = 2436,
                candidates = candidates
            )
        )
    }
}
