package com.toeicflashcard.domain.streak

data class StreakState(
    val currentStreak: Int = 0,
    val lastStudyDateEpochDay: Long = NO_STUDY_DATE,
    val dailyReviewedCount: Int = 0
) {
    companion object {
        const val NO_STUDY_DATE = -1L
    }
}

class StreakCalculator {

    fun onCardReviewed(
        current: StreakState,
        todayEpochDay: Long
    ): StreakState {
        return when {
            current.lastStudyDateEpochDay == todayEpochDay -> {
                StreakState(
                    currentStreak = maxOf(1, current.currentStreak),
                    lastStudyDateEpochDay = todayEpochDay,
                    dailyReviewedCount = current.dailyReviewedCount + 1
                )
            }

            current.lastStudyDateEpochDay == todayEpochDay - 1L -> {
                StreakState(
                    currentStreak = maxOf(0, current.currentStreak) + 1,
                    lastStudyDateEpochDay = todayEpochDay,
                    dailyReviewedCount = 1
                )
            }

            current.lastStudyDateEpochDay == StreakState.NO_STUDY_DATE ||
                todayEpochDay - current.lastStudyDateEpochDay > 1L -> {
                StreakState(
                    currentStreak = 1,
                    lastStudyDateEpochDay = todayEpochDay,
                    dailyReviewedCount = 1
                )
            }

            else -> {
                // Defensive handling if system clock shifts backward slightly
                StreakState(
                    currentStreak = maxOf(1, current.currentStreak),
                    lastStudyDateEpochDay = current.lastStudyDateEpochDay,
                    dailyReviewedCount = current.dailyReviewedCount + 1
                )
            }
        }
    }

    fun effectiveStateForDisplay(
        stored: StreakState,
        todayEpochDay: Long
    ): StreakState {
        return when (stored.lastStudyDateEpochDay) {
            todayEpochDay -> stored
            todayEpochDay - 1L -> stored.copy(dailyReviewedCount = 0)
            else -> stored.copy(currentStreak = 0, dailyReviewedCount = 0)
        }
    }
}
