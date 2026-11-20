package com.toeicflashcard

import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toeicflashcard.core.ui.theme.MeshLightBase
import com.toeicflashcard.core.ui.theme.ToeicFlashcardTheme
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.presentation.navigation.ToeicNavHost
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableAdaptiveHighRefreshRate(this)
        val preferencesRepository = (application as ToeicApplication).container.userPreferencesRepository

        setContent {
            val appearanceFlow = remember(preferencesRepository) {
                preferencesRepository.userPreferences
                    .map { Triple(it.themeMode, it.darkTone, it.glassOpacity) }
                    .distinctUntilChanged()
            }
            val appearance by appearanceFlow
                .collectAsStateWithLifecycle(
                    initialValue = Triple(ThemeMode.SYSTEM, DarkTone.OLED_OBSIDIAN, null)
                )
            val (themeMode, darkTone, glassOpacity) = appearance

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(isDark, darkTone) {
                window.decorView.setBackgroundColor(
                    (if (isDark) Color(darkTone.backgroundHex) else MeshLightBase).toArgb()
                )
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                enableAdaptiveHighRefreshRate(this@MainActivity)
                onDispose {}
            }

            ToeicFlashcardTheme(
                themeMode = themeMode,
                darkTone = darkTone,
                glassOpacity = glassOpacity
            ) {
                ToeicNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableAdaptiveHighRefreshRate(this)
    }
}

internal data class DisplayModeCandidate(
    val modeId: Int,
    val physicalWidth: Int,
    val physicalHeight: Int,
    val refreshRate: Float
)

internal fun selectHighestRefreshRateModeId(
    currentWidth: Int,
    currentHeight: Int,
    candidates: List<DisplayModeCandidate>
): Int? {
    return candidates
        .filter { it.physicalWidth == currentWidth && it.physicalHeight == currentHeight }
        .maxByOrNull { it.refreshRate }
        ?.modeId
}

/**
 * Configures adaptive high refresh rate (e.g. 120Hz when scrolling/animating) while preserving
 * Android 15+ Adaptive Refresh Rate (ARR) so static/reading screens scale down to 60Hz/1Hz.
 */
fun enableAdaptiveHighRefreshRate(activity: ComponentActivity) {
    val window = activity.window ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val params = window.attributes
        if (params.layoutInDisplayCutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT) {
            window.attributes = params.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        window.isFrameRatePowerSavingsBalanced = true
        window.frameRateBoostOnTouchEnabled = true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        val display: Display = activity.display ?: activity.windowManager.defaultDisplay ?: return
        val currentMode = display.mode ?: return
        val supportedModes = display.supportedModes.orEmpty()
        val highestModeId = selectHighestRefreshRateModeId(
            currentWidth = currentMode.physicalWidth,
            currentHeight = currentMode.physicalHeight,
            candidates = supportedModes.map { mode ->
                DisplayModeCandidate(
                    modeId = mode.modeId,
                    physicalWidth = mode.physicalWidth,
                    physicalHeight = mode.physicalHeight,
                    refreshRate = mode.refreshRate
                )
            }
        ) ?: return

        val layoutParams = window.attributes
        if (layoutParams.preferredDisplayModeId != highestModeId || layoutParams.preferredRefreshRate != 0f) {
            window.attributes = layoutParams.apply {
                preferredDisplayModeId = highestModeId
                preferredRefreshRate = 0f
            }
        }
    }
}
