package com.toeicflashcard.core.time

import java.util.Calendar

interface TimeProvider {
    fun nowMillis(): Long
    fun endOfTodayMillis(): Long
    fun todayEpochDay(): Long
    fun epochDayForMillis(timestampMillis: Long): Long
}

class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun endOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis()
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    override fun todayEpochDay(): Long = epochDayForMillis(nowMillis())

    override fun epochDayForMillis(timestampMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val monthOneBased = calendar.get(Calendar.MONTH) + 1
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        return civilToEpochDay(year, monthOneBased, dayOfMonth)
    }

    private fun civilToEpochDay(year: Int, monthOneBased: Int, dayOfMonth: Int): Long {
        val y = year.toLong() - if (monthOneBased <= 2) 1L else 0L
        val era = (if (y >= 0L) y else y - 399L) / 400L
        val yoe = y - era * 400L
        val m = monthOneBased.toLong()
        val doy = (153L * (m + if (m > 2L) -3L else 9L) + 2L) / 5L + dayOfMonth.toLong() - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }
}
