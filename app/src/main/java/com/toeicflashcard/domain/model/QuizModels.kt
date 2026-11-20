package com.toeicflashcard.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class QuizOption(
    val textVi: String,
    val isCorrect: Boolean
)

@Immutable
data class QuizQuestion(
    val word: VocabularyWord,
    val options: List<QuizOption>
)
