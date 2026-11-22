package com.toeicflashcard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.toeicflashcard.domain.model.VocabularyDecks

@Entity(
    tableName = "vocabulary_words",
    indices = [
        Index(value = ["topic"]),
        Index(value = ["nextReviewTimestamp"]),
        Index(value = ["deckId"])
    ]
)
data class VocabularyEntity(
    @PrimaryKey val id: Int,
    val topic: String,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val meaningVi: String,
    val exampleEn: String,
    val exampleVi: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewTimestamp: Long = 0L,
    val lastReviewedTimestamp: Long = 0L,
    val deckId: String = VocabularyDecks.CORE_775_ID,
    val deckName: String = VocabularyDecks.CORE_775_NAME,
    val isStarred: Boolean = false
)
