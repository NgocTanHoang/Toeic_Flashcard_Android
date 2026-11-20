package com.toeicflashcard.domain.streak

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StreakCalculatorTest {

    private lateinit var calculator: StreakCalculator
    private val baseDay = 20_000L

    @Before
    fun setUp() {
        calculator = StreakCalculator()
    }

    @Test
    fun `first study ever sets streak to 1 and daily count to 1`() {
        val result = calculator.onCardReviewed(
            current = StreakState(),
            todayEpochDay = baseDay
        )

        assertEquals(1, result.currentStreak)
        assertEquals(baseDay, result.lastStudyDateEpochDay)
        assertEquals(1, result.dailyReviewedCount)
    }

    @Test
    fun `studying on consecutive days increments streak and resets daily count to 1`() {
        var state = StreakState()

        for (dayOffset in 0L..4L) {
            state = calculator.onCardReviewed(state, baseDay + dayOffset)
            assertEquals((dayOffset + 1).toInt(), state.currentStreak)
            assertEquals(baseDay + dayOffset, state.lastStudyDateEpochDay)
            assertEquals(1, state.dailyReviewedCount)
        }
    }

    @Test
    fun `missing one day resets streak back to 1`() {
        val previous = StreakState(
            currentStreak = 7,
            lastStudyDateEpochDay = baseDay,
            dailyReviewedCount = 12
        )

        val result = calculator.onCardReviewed(
            current = previous,
            todayEpochDay = baseDay + 2L
        )

        assertEquals(1, result.currentStreak)
        assertEquals(baseDay + 2L, result.lastStudyDateEpochDay)
        assertEquals(1, result.dailyReviewedCount)
    }

    @Test
    fun `missing many days resets streak back to 1`() {
        val previous = StreakState(
            currentStreak = 15,
            lastStudyDateEpochDay = baseDay,
            dailyReviewedCount = 20
        )

        val result = calculator.onCardReviewed(
            current = previous,
            todayEpochDay = baseDay + 14L
        )

        assertEquals(1, result.currentStreak)
        assertEquals(baseDay + 14L, result.lastStudyDateEpochDay)
        assertEquals(1, result.dailyReviewedCount)
    }

    @Test
    fun `multiple reviews on the same day keep streak constant and increment daily count`() {
        var state = calculator.onCardReviewed(
            current = StreakState(
                currentStreak = 3,
                lastStudyDateEpochDay = baseDay - 1L,
                dailyReviewedCount = 10
            ),
            todayEpochDay = baseDay
        )
        assertEquals(4, state.currentStreak)
        assertEquals(1, state.dailyReviewedCount)

        state = calculator.onCardReviewed(state, baseDay)
        assertEquals(4, state.currentStreak)
        assertEquals(2, state.dailyReviewedCount)

        state = calculator.onCardReviewed(state, baseDay)
        assertEquals(4, state.currentStreak)
        assertEquals(3, state.dailyReviewedCount)
    }

    @Test
    fun `effectiveStateForDisplay handles same day yesterday and expired streak accurately`() {
        val storedToday = StreakState(
            currentStreak = 5,
            lastStudyDateEpochDay = baseDay,
            dailyReviewedCount = 6
        )
        assertEquals(storedToday, calculator.effectiveStateForDisplay(storedToday, baseDay))

        val storedYesterday = StreakState(
            currentStreak = 5,
            lastStudyDateEpochDay = baseDay - 1L,
            dailyReviewedCount = 9
        )
        val yesterdayDisplay = calculator.effectiveStateForDisplay(storedYesterday, baseDay)
        assertEquals(5, yesterdayDisplay.currentStreak)
        assertEquals(0, yesterdayDisplay.dailyReviewedCount)

        val storedExpired = StreakState(
            currentStreak = 5,
            lastStudyDateEpochDay = baseDay - 2L,
            dailyReviewedCount = 9
        )
        val expiredDisplay = calculator.effectiveStateForDisplay(storedExpired, baseDay)
        assertEquals(0, expiredDisplay.currentStreak)
        assertEquals(0, expiredDisplay.dailyReviewedCount)
    }
}
