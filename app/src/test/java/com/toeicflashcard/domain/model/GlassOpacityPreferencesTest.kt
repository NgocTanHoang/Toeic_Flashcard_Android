package com.toeicflashcard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassOpacityPreferencesTest {

    @Test
    fun `resolvedGlassOpacity uses theme defaults when unset`() {
        val prefs = UserPreferences(glassOpacity = null)
        assertEquals(0.82f, prefs.resolvedGlassOpacity(isDark = true), 0.0001f)
        assertEquals(0.75f, prefs.resolvedGlassOpacity(isDark = false), 0.0001f)
    }

    @Test
    fun `resolvedGlassOpacity clamps values to allowed range`() {
        assertEquals(
            0.45f,
            UserPreferences(glassOpacity = 0.10f).resolvedGlassOpacity(isDark = true),
            0.0001f
        )
        assertEquals(
            0.95f,
            UserPreferences(glassOpacity = 1.25f).resolvedGlassOpacity(isDark = false),
            0.0001f
        )
        assertEquals(
            0.62f,
            UserPreferences(glassOpacity = 0.62f).resolvedGlassOpacity(isDark = true),
            0.0001f
        )
    }
}
