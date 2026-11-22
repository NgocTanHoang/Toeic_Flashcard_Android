package com.toeicflashcard.domain.repository

import com.toeicflashcard.domain.model.VocabularyWord
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    suspend fun ensureSeeded()
    fun getCachedAllWords(): List<VocabularyWord>? = null
    fun observeAllWords(): Flow<List<VocabularyWord>>
    suspend fun getAllWords(): List<VocabularyWord>
    suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord>
    suspend fun getWordsByTopic(topic: String): List<VocabularyWord>
    suspend fun getMaxWordId(): Int? = getAllWords().maxOfOrNull { it.id }
    suspend fun insertCustomWord(word: VocabularyWord): Boolean = false
    suspend fun insertCustomWords(words: List<VocabularyWord>): Int = 0
    suspend fun updateWord(word: VocabularyWord)
    suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long)
    suspend fun setWordStarred(word: VocabularyWord, isStarred: Boolean) {}
    suspend fun resetWordSm2Progress(word: VocabularyWord) {}
    suspend fun updateCustomWord(word: VocabularyWord): Boolean = false
    suspend fun deleteCustomWord(wordId: Int): Boolean = false
}
