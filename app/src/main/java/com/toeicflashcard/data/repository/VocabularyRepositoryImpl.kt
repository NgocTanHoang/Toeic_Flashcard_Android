package com.toeicflashcard.data.repository

import com.toeicflashcard.data.local.dao.VocabularyDao
import com.toeicflashcard.data.local.database.VocabularySeeder
import com.toeicflashcard.data.mapper.toDomain
import com.toeicflashcard.data.mapper.toEntity
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VocabularyRepositoryImpl(
    private val vocabularyDao: VocabularyDao,
    private val vocabularySeeder: VocabularySeeder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : VocabularyRepository {

    private val cacheLock = Any()

    @Volatile
    private var cachedAllWords: List<VocabularyWord>? = null

    override suspend fun ensureSeeded() {
        vocabularySeeder.seedIfEmpty(vocabularyDao)
    }

    override fun getCachedAllWords(): List<VocabularyWord>? = cachedAllWords

    override fun observeAllWords(): Flow<List<VocabularyWord>> {
        return vocabularyDao.observeAllWords()
            .map { entities ->
                val domain = entities.map { it.toDomain() }
                if (domain.isNotEmpty()) {
                    synchronized(cacheLock) {
                        cachedAllWords = domain
                    }
                }
                domain
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getAllWords(): List<VocabularyWord> = withContext(ioDispatcher) {
        val domain = vocabularyDao.getAllWords().map { it.toDomain() }
        if (domain.isNotEmpty()) {
            synchronized(cacheLock) {
                cachedAllWords = domain
            }
        }
        domain
    }

    override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> = withContext(ioDispatcher) {
        vocabularyDao.getDueWords(cutoffMillis).map { it.toDomain() }
    }

    override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> = withContext(ioDispatcher) {
        vocabularyDao.getWordsByTopic(topic).map { it.toDomain() }
    }

    override suspend fun getMaxWordId(): Int? = withContext(ioDispatcher) {
        vocabularyDao.getMaxWordId()
    }

    override suspend fun insertCustomWord(word: VocabularyWord): Boolean = withContext(ioDispatcher) {
        val insertedRowId = vocabularyDao.insertCustomWord(word.toEntity())
        val inserted = insertedRowId != -1L
        if (inserted) {
            synchronized(cacheLock) {
                cachedAllWords = cachedAllWords?.plus(word)
            }
        }
        inserted
    }

    override suspend fun insertCustomWords(words: List<VocabularyWord>): Int = withContext(ioDispatcher) {
        if (words.isEmpty()) return@withContext 0
        val rowIds = vocabularyDao.insertAllWords(words.map { it.toEntity() })
        val insertedWords = words.filterIndexed { index, _ ->
            rowIds.getOrNull(index)?.let { it != -1L } == true
        }
        if (insertedWords.isNotEmpty()) {
            synchronized(cacheLock) {
                cachedAllWords = cachedAllWords?.plus(insertedWords)
            }
        }
        insertedWords.size
    }

    override suspend fun updateWord(word: VocabularyWord) = withContext(ioDispatcher) {
        vocabularyDao.updateWord(word.toEntity())
        synchronized(cacheLock) {
            cachedAllWords = cachedAllWords?.map { if (it.id == word.id) word else it }
        }
    }

    override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) {
        if (wordIds.isEmpty()) return
        withContext(ioDispatcher) {
            vocabularyDao.markWordsUrgentReview(wordIds = wordIds, timestamp = nowMillis)
            val idSet = wordIds.toHashSet()
            synchronized(cacheLock) {
                cachedAllWords = cachedAllWords?.map {
                    if (it.id in idSet) it.copy(nextReviewTimestamp = nowMillis) else it
                }
            }
        }
    }
}
