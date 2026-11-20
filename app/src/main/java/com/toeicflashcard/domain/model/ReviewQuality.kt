package com.toeicflashcard.domain.model

enum class ReviewQuality(val quality: Int, val labelVi: String) {
    AGAIN(quality = 1, labelVi = "Quên"),
    HARD(quality = 3, labelVi = "Khó"),
    GOOD(quality = 4, labelVi = "Tốt"),
    EASY(quality = 5, labelVi = "Dễ")
}
