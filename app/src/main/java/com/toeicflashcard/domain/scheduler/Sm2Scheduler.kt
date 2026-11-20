package com.toeicflashcard.domain.scheduler

import com.toeicflashcard.domain.model.ReviewQuality
import com.toeicflashcard.domain.model.Sm2Result
import com.toeicflashcard.domain.model.VocabularyWord
import kotlin.math.roundToInt

class Sm2Scheduler {

    fun calculate(
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Float,
        quality: ReviewQuality,
        nowMillis: Long
    ): Sm2Result {
        val q = quality.quality
        val diff = 5 - q
        val delta = 0.1f - diff * (0.08f + diff * 0.02f)
        val newEaseFactor = maxOf(MIN_EASE_FACTOR, currentEaseFactor + delta)

        if (q < 3) {
            return Sm2Result(
                repetitions = 0,
                intervalDays = 1,
                easeFactor = newEaseFactor,
                nextReviewTimestamp = nowMillis
            )
        }

        val newRepetitions = currentRepetitions + 1
        val newIntervalDays = when (currentRepetitions) {
            0 -> 1
            1 -> 6
            else -> (currentIntervalDays * newEaseFactor).roundToInt().coerceAtLeast(1)
        }

        return Sm2Result(
            repetitions = newRepetitions,
            intervalDays = newIntervalDays,
            easeFactor = newEaseFactor,
            nextReviewTimestamp = nowMillis + newIntervalDays * MILLIS_PER_DAY
        )
    }

    fun calculateForWord(
        word: VocabularyWord,
        quality: ReviewQuality,
        nowMillis: Long
    ): Sm2Result {
        return calculate(
            currentRepetitions = word.repetitions,
            currentIntervalDays = word.intervalDays,
            currentEaseFactor = word.easeFactor,
            quality = quality,
            nowMillis = nowMillis
        )
    }

    fun previewAll(
        word: VocabularyWord,
        nowMillis: Long
    ): Map<ReviewQuality, String> {
        return ReviewQuality.entries.associateWith { quality ->
            val result = calculateForWord(word, quality, nowMillis)
            formatIntervalPreview(quality, result)
        }
    }

    fun formatIntervalPreview(quality: ReviewQuality, result: Sm2Result): String {
        if (quality == ReviewQuality.AGAIN) {
            return "< 1 ngày"
        }
        val days = result.intervalDays
        return when {
            days < 60 -> "$days ngày"
            days < 365 -> "${(days / 30.0).roundToInt()} tháng"
            else -> "${(days / 365.0).roundToInt()} năm"
        }
    }

    companion object {
        const val DEFAULT_EASE_FACTOR = 2.5f
        const val MIN_EASE_FACTOR = 1.3f
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
