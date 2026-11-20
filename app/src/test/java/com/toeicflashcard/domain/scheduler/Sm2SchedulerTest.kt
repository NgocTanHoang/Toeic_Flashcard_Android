package com.toeicflashcard.domain.scheduler

import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.Sm2Result
import com.toeicflashcard.domain.model.VocabularyWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Sm2SchedulerTest {

    private lateinit var scheduler: Sm2Scheduler
    private val baseNow = 1_700_000_000_000L

    @Before
    fun setUp() {
        scheduler = Sm2Scheduler()
    }

    @Test
    fun `first review with GOOD sets repetitions to 1 interval to 1 day and keeps EF at 2_5`() {
        val result = scheduler.calculate(
            currentRepetitions = 0,
            currentIntervalDays = 0,
            currentEaseFactor = Sm2Scheduler.DEFAULT_EASE_FACTOR,
            quality = ReviewQuality.GOOD,
            nowMillis = baseNow
        )

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(2.5f, result.easeFactor, 0.0001f)
        assertEquals(baseNow + Sm2Scheduler.MILLIS_PER_DAY, result.nextReviewTimestamp)
    }

    @Test
    fun `first review with HARD sets repetitions to 1 interval to 1 day and lowers EF to 2_36`() {
        val result = scheduler.calculate(
            currentRepetitions = 0,
            currentIntervalDays = 0,
            currentEaseFactor = Sm2Scheduler.DEFAULT_EASE_FACTOR,
            quality = ReviewQuality.HARD,
            nowMillis = baseNow
        )

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(2.36f, result.easeFactor, 0.0001f)
        assertEquals(baseNow + Sm2Scheduler.MILLIS_PER_DAY, result.nextReviewTimestamp)
    }

    @Test
    fun `first review with EASY sets repetitions to 1 interval to 1 day and raises EF to 2_6`() {
        val result = scheduler.calculate(
            currentRepetitions = 0,
            currentIntervalDays = 0,
            currentEaseFactor = Sm2Scheduler.DEFAULT_EASE_FACTOR,
            quality = ReviewQuality.EASY,
            nowMillis = baseNow
        )

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(2.6f, result.easeFactor, 0.0001f)
        assertEquals(baseNow + Sm2Scheduler.MILLIS_PER_DAY, result.nextReviewTimestamp)
    }

    @Test
    fun `consecutive GOOD reviews follow standard SM-2 intervals 1 6 15 38 95`() {
        var reps = 0
        var interval = 0
        var ef = Sm2Scheduler.DEFAULT_EASE_FACTOR
        var now = baseNow

        val expectedIntervals = listOf(1, 6, 15, 38, 95)

        expectedIntervals.forEachIndexed { index, expectedInterval ->
            val result = scheduler.calculate(
                currentRepetitions = reps,
                currentIntervalDays = interval,
                currentEaseFactor = ef,
                quality = ReviewQuality.GOOD,
                nowMillis = now
            )

            assertEquals(index + 1, result.repetitions)
            assertEquals(expectedInterval, result.intervalDays)
            assertEquals(2.5f, result.easeFactor, 0.0001f)
            assertEquals(now + expectedInterval * Sm2Scheduler.MILLIS_PER_DAY, result.nextReviewTimestamp)

            reps = result.repetitions
            interval = result.intervalDays
            ef = result.easeFactor
            now = result.nextReviewTimestamp
        }
    }

    @Test
    fun `consecutive EASY reviews increase ease factor by 0_10 each time and grow intervals faster`() {
        var reps = 0
        var interval = 0
        var ef = Sm2Scheduler.DEFAULT_EASE_FACTOR
        var now = baseNow

        val expectedIntervals = listOf(1, 6, 17, 49)
        val expectedEaseFactors = listOf(2.6f, 2.7f, 2.8f, 2.9f)

        expectedIntervals.indices.forEach { step ->
            val result = scheduler.calculate(
                currentRepetitions = reps,
                currentIntervalDays = interval,
                currentEaseFactor = ef,
                quality = ReviewQuality.EASY,
                nowMillis = now
            )

            assertEquals(step + 1, result.repetitions)
            assertEquals(expectedIntervals[step], result.intervalDays)
            assertEquals(expectedEaseFactors[step], result.easeFactor, 0.001f)

            reps = result.repetitions
            interval = result.intervalDays
            ef = result.easeFactor
            now = result.nextReviewTimestamp
        }
    }

    @Test
    fun `consecutive HARD reviews reduce ease factor and clamp at minimum 1_3`() {
        var reps = 0
        var interval = 0
        var ef = Sm2Scheduler.DEFAULT_EASE_FACTOR
        var now = baseNow

        repeat(15) {
            val result = scheduler.calculate(
                currentRepetitions = reps,
                currentIntervalDays = interval,
                currentEaseFactor = ef,
                quality = ReviewQuality.HARD,
                nowMillis = now
            )
            assertTrue(result.easeFactor >= Sm2Scheduler.MIN_EASE_FACTOR)
            reps = result.repetitions
            interval = result.intervalDays
            ef = result.easeFactor
            now = result.nextReviewTimestamp
        }

        assertEquals(Sm2Scheduler.MIN_EASE_FACTOR, ef, 0.0001f)
    }

    @Test
    fun `pressing AGAIN resets repetitions to 0 intervalDays to 1 and keeps card due immediately`() {
        val result = scheduler.calculate(
            currentRepetitions = 4,
            currentIntervalDays = 38,
            currentEaseFactor = 2.6f,
            quality = ReviewQuality.AGAIN,
            nowMillis = baseNow
        )

        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(2.06f, result.easeFactor, 0.001f)
        assertEquals(baseNow, result.nextReviewTimestamp)
    }

    @Test
    fun `repeated AGAIN reviews never drop ease factor below 1_3`() {
        var reps = 3
        var interval = 15
        var ef = 1.7f

        repeat(5) {
            val result = scheduler.calculate(
                currentRepetitions = reps,
                currentIntervalDays = interval,
                currentEaseFactor = ef,
                quality = ReviewQuality.AGAIN,
                nowMillis = baseNow
            )
            assertEquals(0, result.repetitions)
            assertEquals(1, result.intervalDays)
            assertTrue(result.easeFactor >= Sm2Scheduler.MIN_EASE_FACTOR)
            assertEquals(baseNow, result.nextReviewTimestamp)

            reps = result.repetitions
            interval = result.intervalDays
            ef = result.easeFactor
        }

        assertEquals(Sm2Scheduler.MIN_EASE_FACTOR, ef, 0.0001f)
    }

    @Test
    fun `recovery after AGAIN followed by GOOD answers restarts 1 day then 6 days`() {
        val failed = scheduler.calculate(
            currentRepetitions = 3,
            currentIntervalDays = 15,
            currentEaseFactor = 2.5f,
            quality = ReviewQuality.AGAIN,
            nowMillis = baseNow
        )
        assertEquals(0, failed.repetitions)
        assertEquals(1, failed.intervalDays)
        assertEquals(1.96f, failed.easeFactor, 0.001f)

        val firstGood = scheduler.calculate(
            currentRepetitions = failed.repetitions,
            currentIntervalDays = failed.intervalDays,
            currentEaseFactor = failed.easeFactor,
            quality = ReviewQuality.GOOD,
            nowMillis = baseNow + 60_000L
        )
        assertEquals(1, firstGood.repetitions)
        assertEquals(1, firstGood.intervalDays)

        val secondGood = scheduler.calculate(
            currentRepetitions = firstGood.repetitions,
            currentIntervalDays = firstGood.intervalDays,
            currentEaseFactor = firstGood.easeFactor,
            quality = ReviewQuality.GOOD,
            nowMillis = firstGood.nextReviewTimestamp
        )
        assertEquals(2, secondGood.repetitions)
        assertEquals(6, secondGood.intervalDays)
    }

    @Test
    fun `previewAll formats interval labels accurately for all four buttons`() {
        val word = VocabularyWord(
            id = 1,
            topic = "Business & Office",
            word = "agenda",
            phonetic = "/əˈdʒendə/",
            partOfSpeech = "n.",
            meaningVi = "chương trình nghị sự",
            exampleEn = "Please review the meeting agenda.",
            exampleVi = "Vui lòng xem lại chương trình nghị sự.",
            repetitions = 2,
            intervalDays = 6,
            easeFactor = 2.5f
        )

        val previews = scheduler.previewAll(word, baseNow)

        assertEquals("< 1 ngày", previews[ReviewQuality.AGAIN])
        assertEquals("14 ngày", previews[ReviewQuality.HARD])
        assertEquals("15 ngày", previews[ReviewQuality.GOOD])
        assertEquals("16 ngày", previews[ReviewQuality.EASY])

        assertEquals(
            "3 tháng",
            scheduler.formatIntervalPreview(
                ReviewQuality.GOOD,
                Sm2Result(repetitions = 5, intervalDays = 95, easeFactor = 2.5f, nextReviewTimestamp = baseNow)
            )
        )
        assertEquals(
            "1 năm",
            scheduler.formatIntervalPreview(
                ReviewQuality.EASY,
                Sm2Result(repetitions = 7, intervalDays = 400, easeFactor = 2.6f, nextReviewTimestamp = baseNow)
            )
        )
    }
}
