package com.toeicflashcard.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TopicProgress(
    val topic: String,
    val totalCount: Int,
    val learnedCount: Int,
    val dueCount: Int
)
