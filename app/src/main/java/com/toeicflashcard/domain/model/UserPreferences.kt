package com.toeicflashcard.domain.model

import androidx.compose.runtime.Immutable
import com.toeicflashcard.domain.streak.StreakState

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ToeicTargetLevel(
    val id: String,
    val label: String,
    val recommendationVi: String
) {
    TARGET_500(
        id = "500+",
        label = "500+",
        recommendationVi = "Gợi ý: Bắt đầu với 600 Từ Cốt Lõi để nắm chắc nền tảng."
    ),
    TARGET_650(
        id = "650+",
        label = "650+",
        recommendationVi = "Gợi ý: Học xong 600 Từ Cốt Lõi rồi mở rộng sang 775 Từ Mở Rộng."
    ),
    TARGET_800(
        id = "800+",
        label = "800+",
        recommendationVi = "Gợi ý: Kết hợp 600 Từ Cốt Lõi và chinh phục 800 Từ Nâng Cao."
    );

    companion object {
        fun fromId(id: String?): ToeicTargetLevel {
            if (id == null) return TARGET_650
            return entries.firstOrNull { it.id == id || it.name == id } ?: TARGET_650
        }
    }
}

enum class DarkTone(
    val id: String,
    val label: String,
    val backgroundHex: Long,
    val glassSurfaceHex: Long,
    val elevatedSurfaceHex: Long,
    val primaryTextHex: Long,
    val secondaryTextHex: Long,
    val accentHex: Long,
    val onAccentHex: Long,
    val borderTintHex: Long
) {
    OLED_OBSIDIAN(
        id = "oled_obsidian",
        label = "Đen OLED",
        backgroundHex = 0xFF050507L,
        glassSurfaceHex = 0xFF121318L,
        elevatedSurfaceHex = 0xFF1A1C23L,
        primaryTextHex = 0xFFF4F4F6L,
        secondaryTextHex = 0xFFA1A1AAL,
        accentHex = 0xFFF5F5F7L,
        onAccentHex = 0xFF050507L,
        borderTintHex = 0xFFFFFFFFL
    ),
    CATPPUCCIN_MOCHA(
        id = "catppuccin_mocha",
        label = "Catppuccin Mocha",
        backgroundHex = 0xFF1E1E2EL,
        glassSurfaceHex = 0xFF2A2B3DL,
        elevatedSurfaceHex = 0xFF313244L,
        primaryTextHex = 0xFFCDD6F4L,
        secondaryTextHex = 0xFFA6ADC8L,
        accentHex = 0xFFCBA6F7L,
        onAccentHex = 0xFF1E1E2EL,
        borderTintHex = 0xFFCBA6F7L
    ),
    TOKYO_NIGHT(
        id = "tokyo_night",
        label = "Tokyo Night",
        backgroundHex = 0xFF1A1B26L,
        glassSurfaceHex = 0xFF24283BL,
        elevatedSurfaceHex = 0xFF292E42L,
        primaryTextHex = 0xFFC0CAF5L,
        secondaryTextHex = 0xFF9AA5CEL,
        accentHex = 0xFF7AA2F7L,
        onAccentHex = 0xFF1A1B26L,
        borderTintHex = 0xFF7AA2F7L
    ),
    NORD_ARCTIC(
        id = "nord_arctic",
        label = "Nord Bắc Cực",
        backgroundHex = 0xFF2E3440L,
        glassSurfaceHex = 0xFF3B4252L,
        elevatedSurfaceHex = 0xFF434C5EL,
        primaryTextHex = 0xFFECEFF4L,
        secondaryTextHex = 0xFFD8DEE9L,
        accentHex = 0xFF88C0D0L,
        onAccentHex = 0xFF2E3440L,
        borderTintHex = 0xFF88C0D0L
    ),
    ROSE_PINE(
        id = "rose_pine",
        label = "Rosé Pine",
        backgroundHex = 0xFF191724L,
        glassSurfaceHex = 0xFF1F1D2EL,
        elevatedSurfaceHex = 0xFF26233AL,
        primaryTextHex = 0xFFE0DEF4L,
        secondaryTextHex = 0xFF908CAAL,
        accentHex = 0xFFEBBCBAL,
        onAccentHex = 0xFF191724L,
        borderTintHex = 0xFFEBBCBAL
    ),
    GRUVBOX_DARK(
        id = "gruvbox_dark",
        label = "Gruvbox Ấm",
        backgroundHex = 0xFF282828L,
        glassSurfaceHex = 0xFF32302FL,
        elevatedSurfaceHex = 0xFF3C3836L,
        primaryTextHex = 0xFFEBDBB2L,
        secondaryTextHex = 0xFFBDAE93L,
        accentHex = 0xFFFABD2FL,
        onAccentHex = 0xFF282828L,
        borderTintHex = 0xFFFABD2FL
    ),
    GITHUB_DARK(
        id = "github_dark",
        label = "GitHub Dark",
        backgroundHex = 0xFF0D1117L,
        glassSurfaceHex = 0xFF161B22L,
        elevatedSurfaceHex = 0xFF1F242CL,
        primaryTextHex = 0xFFE6EDF3L,
        secondaryTextHex = 0xFF8B949EL,
        accentHex = 0xFF58A6FFL,
        onAccentHex = 0xFF0D1117L,
        borderTintHex = 0xFF30363DL
    );

    companion object {
        fun fromId(id: String?): DarkTone {
            if (id == null) return OLED_OBSIDIAN
            entries.firstOrNull { it.id == id || it.name == id }?.let { return it }
            return when (id) {
                "oled_black", "OLED_BLACK", "charcoal_gray", "CHARCOAL_GRAY" -> OLED_OBSIDIAN
                "midnight_slate", "MIDNIGHT_SLATE" -> GITHUB_DARK
                "warm_night", "WARM_NIGHT" -> GRUVBOX_DARK
                else -> OLED_OBSIDIAN
            }
        }
    }
}

@Immutable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkTone: DarkTone = DarkTone.OLED_OBSIDIAN,
    val speechRate: Float = 0.95f,
    val dailyGoal: Int = 10,
    val currentStreak: Int = 0,
    val lastStudyDateEpochDay: Long = StreakState.NO_STUDY_DATE,
    val dailyReviewedCount: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val glassOpacity: Float? = null,
    val dailyReviewHistory: Map<Long, Int> = emptyMap(),
    val selectedDeckId: String = VocabularyDecks.DEFAULT_SELECTED_DECK_ID,
    val autoPlayTts: Boolean = true,
    val enableHapticFeedback: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val targetToeicLevel: String = ToeicTargetLevel.TARGET_650.id
) {
    fun toStreakState(): StreakState = StreakState(
        currentStreak = currentStreak,
        lastStudyDateEpochDay = lastStudyDateEpochDay,
        dailyReviewedCount = dailyReviewedCount
    )

    fun resolvedGlassOpacity(isDark: Boolean): Float =
        (glassOpacity ?: defaultGlassOpacity(isDark)).coerceIn(MIN_GLASS_OPACITY, MAX_GLASS_OPACITY)

    companion object {
        const val MIN_GLASS_OPACITY = 0.45f
        const val MAX_GLASS_OPACITY = 0.95f
        const val DEFAULT_GLASS_OPACITY_DARK = 0.82f
        const val DEFAULT_GLASS_OPACITY_LIGHT = 0.75f

        fun defaultGlassOpacity(isDark: Boolean): Float =
            if (isDark) DEFAULT_GLASS_OPACITY_DARK else DEFAULT_GLASS_OPACITY_LIGHT
    }
}
