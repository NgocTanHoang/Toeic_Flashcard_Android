package com.toeicflashcard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.toeicflashcard.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    @Query("SELECT * FROM vocabulary_words ORDER BY id ASC")
    fun observeAllWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_words ORDER BY id ASC")
    suspend fun getAllWords(): List<VocabularyEntity>

    @Query(
        "SELECT * FROM vocabulary_words " +
            "WHERE nextReviewTimestamp <= :cutoffMillis " +
            "ORDER BY nextReviewTimestamp ASC, id ASC"
    )
    suspend fun getDueWords(cutoffMillis: Long): List<VocabularyEntity>

    @Query(
        "SELECT * FROM vocabulary_words " +
            "WHERE topic = :topic " +
            "ORDER BY nextReviewTimestamp ASC, id ASC"
    )
    suspend fun getWordsByTopic(topic: String): List<VocabularyEntity>

    @Query("SELECT COUNT(*) FROM vocabulary_words")
    suspend fun getCount(): Int

    @Query("SELECT MAX(id) FROM vocabulary_words")
    suspend fun getMaxWordId(): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomWord(word: VocabularyEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllWords(words: List<VocabularyEntity>): List<Long>

    @Update
    suspend fun updateWord(word: VocabularyEntity)

    @Query(
        "UPDATE vocabulary_words SET " +
            "repetitions = :repetitions, " +
            "intervalDays = :intervalDays, " +
            "easeFactor = :easeFactor, " +
            "nextReviewTimestamp = :nextReviewTimestamp, " +
            "lastReviewedTimestamp = :lastReviewedTimestamp " +
            "WHERE LOWER(TRIM(word)) = :normalizedWord"
    )
    suspend fun updateWordsByNormalizedWord(
        normalizedWord: String,
        repetitions: Int,
        intervalDays: Int,
        easeFactor: Float,
        nextReviewTimestamp: Long,
        lastReviewedTimestamp: Long
    )

    @Query("UPDATE vocabulary_words SET nextReviewTimestamp = :timestamp WHERE id IN (:wordIds)")
    suspend fun markWordsUrgentReview(wordIds: List<Int>, timestamp: Long)

    @Query(
        "UPDATE vocabulary_words SET isStarred = :isStarred " +
            "WHERE LOWER(TRIM(word)) = :normalizedWord"
    )
    suspend fun updateStarredByNormalizedWord(normalizedWord: String, isStarred: Boolean)

    @Query(
        "UPDATE vocabulary_words SET " +
            "repetitions = 0, " +
            "intervalDays = 0, " +
            "easeFactor = :defaultEaseFactor, " +
            "nextReviewTimestamp = 0, " +
            "lastReviewedTimestamp = 0 " +
            "WHERE LOWER(TRIM(word)) = :normalizedWord"
    )
    suspend fun resetSm2ByNormalizedWord(
        normalizedWord: String,
        defaultEaseFactor: Float = 2.5f
    )

    @Query(
        "UPDATE vocabulary_words SET " +
            "word = :word, " +
            "phonetic = :phonetic, " +
            "partOfSpeech = :partOfSpeech, " +
            "meaningVi = :meaningVi, " +
            "topic = :topic, " +
            "exampleEn = :exampleEn, " +
            "exampleVi = :exampleVi " +
            "WHERE id = :id AND deckId = 'custom_user'"
    )
    suspend fun updateCustomWordContent(
        id: Int,
        word: String,
        phonetic: String,
        partOfSpeech: String,
        meaningVi: String,
        topic: String,
        exampleEn: String,
        exampleVi: String
    ): Int

    @Query("DELETE FROM vocabulary_words WHERE id = :id AND deckId = 'custom_user'")
    suspend fun deleteCustomWordById(id: Int): Int
}
