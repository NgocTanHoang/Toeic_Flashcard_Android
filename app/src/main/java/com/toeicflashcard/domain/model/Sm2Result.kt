package com.toeicflashcard.domain.model

data class Sm2Result(
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val nextReviewTimestamp: Long
)
