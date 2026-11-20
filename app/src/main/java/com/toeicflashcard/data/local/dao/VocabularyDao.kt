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

    @Query("UPDATE vocabulary_words SET nextReviewTimestamp = :timestamp WHERE id IN (:wordIds)")
    suspend fun markWordsUrgentReview(wordIds: List<Int>, timestamp: Long)
}
