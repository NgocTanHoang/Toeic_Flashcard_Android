package com.toeicflashcard.data.model

import com.toeicflashcard.domain.model.VocabularyDecks
import kotlinx.serialization.Serializable

@Serializable
data class SeedVocabularyDto(
    val id: Int,
    val deckId: String = VocabularyDecks.CORE_775_ID,
    val deckName: String = VocabularyDecks.CORE_775_NAME,
    val topic: String,
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val meaningVi: String,
    val exampleEn: String,
    val exampleVi: String
)
