package com.toeicflashcard

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import com.toeicflashcard.data.local.dao.VocabularyDao
import com.toeicflashcard.data.local.database.ToeicDatabase
import com.toeicflashcard.data.local.database.VocabularySeeder
import com.toeicflashcard.data.local.entity.VocabularyEntity
import com.toeicflashcard.data.mapper.toDomain
import com.toeicflashcard.data.repository.VocabularyRepositoryImpl
import com.toeicflashcard.domain.model.UserPreferences
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.UserPreferencesRepository
import com.toeicflashcard.presentation.vault.VaultFilter
import com.toeicflashcard.presentation.vault.VocabularyVaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class Phase2VaultAndSyncTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class InMemoryVocabularyDao(
        initialEntities: List<VocabularyEntity>
    ) : VocabularyDao {
        private val flow = MutableStateFlow(initialEntities)

        override fun observeAllWords(): Flow<List<VocabularyEntity>> = flow

        override suspend fun getAllWords(): List<VocabularyEntity> = flow.value

        override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyEntity> =
            flow.value.filter { it.nextReviewTimestamp <= cutoffMillis }

        override suspend fun getWordsByTopic(topic: String): List<VocabularyEntity> =
            flow.value.filter { it.topic == topic }

        override suspend fun getCount(): Int = flow.value.size

        override suspend fun getMaxWordId(): Int? = flow.value.maxOfOrNull { it.id }

        override suspend fun insertAll(words: List<VocabularyEntity>) {
            val existingIds = flow.value.map { it.id }.toSet()
            flow.value = flow.value + words.filterNot { it.id in existingIds }
        }

        override suspend fun insertCustomWord(word: VocabularyEntity): Long {
            if (flow.value.any { it.id == word.id }) return -1L
            flow.value = flow.value + word
            return word.id.toLong()
        }

        override suspend fun insertAllWords(words: List<VocabularyEntity>): List<Long> {
            val results = mutableListOf<Long>()
            for (word in words) {
                results += insertCustomWord(word)
            }
            return results
        }

        override suspend fun updateWord(word: VocabularyEntity) {
            flow.value = flow.value.map { if (it.id == word.id) word else it }
        }

        override suspend fun updateWordsByNormalizedWord(
            normalizedWord: String,
            repetitions: Int,
            intervalDays: Int,
            easeFactor: Float,
            nextReviewTimestamp: Long,
            lastReviewedTimestamp: Long
        ) {
            flow.value = flow.value.map { entity ->
                if (entity.word.trim().lowercase(Locale.ROOT) == normalizedWord) {
                    entity.copy(
                        repetitions = repetitions,
                        intervalDays = intervalDays,
                        easeFactor = easeFactor,
                        nextReviewTimestamp = nextReviewTimestamp,
                        lastReviewedTimestamp = lastReviewedTimestamp
                    )
                } else {
                    entity
                }
            }
        }

        override suspend fun markWordsUrgentReview(wordIds: List<Int>, timestamp: Long) {
            val idSet = wordIds.toSet()
            flow.value = flow.value.map {
                if (it.id in idSet) it.copy(nextReviewTimestamp = timestamp) else it
            }
        }

        override suspend fun updateStarredByNormalizedWord(normalizedWord: String, isStarred: Boolean) {
            flow.value = flow.value.map { entity ->
                if (entity.word.trim().lowercase(Locale.ROOT) == normalizedWord) {
                    entity.copy(isStarred = isStarred)
                } else {
                    entity
                }
            }
        }

        override suspend fun resetSm2ByNormalizedWord(normalizedWord: String, defaultEaseFactor: Float) {
            flow.value = flow.value.map { entity ->
                if (entity.word.trim().lowercase(Locale.ROOT) == normalizedWord) {
                    entity.copy(
                        repetitions = 0,
                        intervalDays = 0,
                        easeFactor = defaultEaseFactor,
                        nextReviewTimestamp = 0L,
                        lastReviewedTimestamp = 0L
                    )
                } else {
                    entity
                }
            }
        }

        override suspend fun updateCustomWordContent(
            id: Int,
            word: String,
            phonetic: String,
            partOfSpeech: String,
            meaningVi: String,
            topic: String,
            exampleEn: String,
            exampleVi: String
        ): Int {
            var updated = 0
            flow.value = flow.value.map { entity ->
                if (entity.id == id && entity.deckId == VocabularyDecks.CUSTOM_USER_ID) {
                    updated++
                    entity.copy(
                        word = word,
                        phonetic = phonetic,
                        partOfSpeech = partOfSpeech,
                        meaningVi = meaningVi,
                        topic = topic,
                        exampleEn = exampleEn,
                        exampleVi = exampleVi
                    )
                } else {
                    entity
                }
            }
            return updated
        }

        override suspend fun deleteCustomWordById(id: Int): Int {
            val before = flow.value.size
            flow.value = flow.value.filterNot {
                it.id == id && it.deckId == VocabularyDecks.CUSTOM_USER_ID
            }
            return before - flow.value.size
        }
    }

    private class FakePrefsRepo(
        initial: UserPreferences = UserPreferences(selectedDeckId = VocabularyDecks.ALL_ID)
    ) : UserPreferencesRepository {
        private val flow = MutableStateFlow(initial)
        override val userPreferences: Flow<UserPreferences> = flow
        override suspend fun setThemeMode(mode: com.toeicflashcard.domain.model.ThemeMode) = Unit
        override suspend fun setDarkTone(tone: com.toeicflashcard.domain.model.DarkTone, forceDarkMode: Boolean) = Unit
        override suspend fun setGlassOpacity(opacity: Float) = Unit
        override suspend fun resetGlassOpacity() = Unit
        override fun previewGlassOpacity(opacity: Float?) = Unit
        override suspend fun setSpeechRate(rate: Float) = Unit
        override suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) = Unit
        override suspend fun setDailyGoal(goal: Int) = Unit
        override suspend fun setSelectedDeckId(deckId: String) {
            flow.value = flow.value.copy(selectedDeckId = deckId)
        }
        override suspend fun setAutoPlayTts(enabled: Boolean) = Unit
        override suspend fun setEnableHapticFeedback(enabled: Boolean) = Unit
        override suspend fun recordStudyReview(
            todayEpochDay: Long,
            calculator: com.toeicflashcard.domain.streak.StreakCalculator
        ) = Unit
    }

    private fun stubSeeder(): VocabularySeeder {
        val stubContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
        }
        return VocabularySeeder(stubContext)
    }

    private fun entity(
        id: Int,
        word: String,
        deckId: String,
        deckName: String,
        repetitions: Int = 0,
        intervalDays: Int = 0,
        easeFactor: Float = 2.5f,
        nextReviewTimestamp: Long = 0L,
        lastReviewedTimestamp: Long = 0L,
        isStarred: Boolean = false
    ) = VocabularyEntity(
        id = id,
        topic = "Office",
        word = word,
        phonetic = "/test/",
        partOfSpeech = "n.",
        meaningVi = "nghia $word",
        exampleEn = "Example $word",
        exampleVi = "Vi du $word",
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        nextReviewTimestamp = nextReviewTimestamp,
        lastReviewedTimestamp = lastReviewedTimestamp,
        deckId = deckId,
        deckName = deckName,
        isStarred = isStarred
    )

    @Test
    fun `migration 3 to 4 adds isStarred column with default 0`() {
        val executedSql = mutableListOf<String>()
        val fakeDb = object : SupportSQLiteDatabase {
            override fun execSQL(sql: String) {
                executedSql += sql
            }
            override fun execSQL(sql: String, bindArgs: Array<out Any?>) = Unit
            override fun compileStatement(sql: String): SupportSQLiteStatement = throw UnsupportedOperationException()
            override fun beginTransaction() = Unit
            override fun beginTransactionNonExclusive() = Unit
            override fun beginTransactionWithListener(transactionListener: android.database.sqlite.SQLiteTransactionListener) = Unit
            override fun beginTransactionWithListenerNonExclusive(transactionListener: android.database.sqlite.SQLiteTransactionListener) = Unit
            override fun endTransaction() = Unit
            override fun setTransactionSuccessful() = Unit
            override fun inTransaction(): Boolean = false
            override val isDbLockedByCurrentThread: Boolean = false
            override fun yieldIfContendedSafely(): Boolean = false
            override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = false
            override var version: Int = 3
            override val maximumSize: Long = 0L
            override fun setMaximumSize(numBytes: Long): Long = 0L
            override var pageSize: Long = 0L
            override fun query(query: String): Cursor = throw UnsupportedOperationException()
            override fun query(query: String, bindArgs: Array<out Any?>): Cursor = throw UnsupportedOperationException()
            override fun query(query: SupportSQLiteQuery): Cursor = throw UnsupportedOperationException()
            override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor = throw UnsupportedOperationException()
            override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long = 0L
            override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = 0
            override fun update(table: String, conflictAlgorithm: Int, values: ContentValues, whereClause: String?, whereArgs: Array<out Any?>?): Int = 0
            override fun needUpgrade(newVersion: Int): Boolean = false
            override val path: String? = null
            override val isReadOnly: Boolean = false
            override val isOpen: Boolean = true
            override fun setLocale(locale: Locale) = Unit
            override fun setMaxSqlCacheSize(cacheSize: Int) = Unit
            override fun setForeignKeyConstraintsEnabled(enabled: Boolean) = Unit
            override fun enableWriteAheadLogging(): Boolean = false
            override fun disableWriteAheadLogging() = Unit
            override val isWriteAheadLoggingEnabled: Boolean = false
            override val attachedDbs: List<android.util.Pair<String, String>>? = null
            override val isDatabaseIntegrityOk: Boolean = true
            override fun close() = Unit
        }

        assertEquals(3, ToeicDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, ToeicDatabase.MIGRATION_3_4.endVersion)
        ToeicDatabase.MIGRATION_3_4.migrate(fakeDb)

        assertEquals(1, executedSql.size)
        assertTrue(executedSql[0].contains("ALTER TABLE `vocabulary_words` ADD COLUMN `isStarred` INTEGER NOT NULL DEFAULT 0"))
    }

    @Test
    fun `starring a word syncs isStarred across all decks sharing normalized word`() = runTest {
        val dao = InMemoryVocabularyDao(
            listOf(
                entity(1, "agenda", VocabularyDecks.ESSENTIAL_600_ID, VocabularyDecks.ESSENTIAL_600_NAME),
                entity(2, " Agenda ", VocabularyDecks.CORE_775_ID, VocabularyDecks.CORE_775_NAME),
                entity(3, "budget", VocabularyDecks.LEVEL_450_800_ID, VocabularyDecks.LEVEL_450_800_NAME)
            )
        )
        val repo = VocabularyRepositoryImpl(dao, stubSeeder(), testDispatcher)
        val initial = repo.getAllWords()

        repo.setWordStarred(initial.first { it.id == 1 }, isStarred = true)

        val updatedDb = dao.getAllWords()
        assertTrue(updatedDb.first { it.id == 1 }.isStarred)
        assertTrue(updatedDb.first { it.id == 2 }.isStarred)
        assertFalse(updatedDb.first { it.id == 3 }.isStarred)

        val cached = repo.getCachedAllWords()!!
        assertTrue(cached.first { it.id == 1 }.isStarred)
        assertTrue(cached.first { it.id == 2 }.isStarred)
        assertFalse(cached.first { it.id == 3 }.isStarred)
    }

    @Test
    fun `resetting SM-2 progress resets all matching deck records and preserves isStarred`() = runTest {
        val dao = InMemoryVocabularyDao(
            listOf(
                entity(
                    id = 10,
                    word = "contract",
                    deckId = VocabularyDecks.ESSENTIAL_600_ID,
                    deckName = VocabularyDecks.ESSENTIAL_600_NAME,
                    repetitions = 4,
                    intervalDays = 14,
                    easeFactor = 2.6f,
                    nextReviewTimestamp = 999_999L,
                    lastReviewedTimestamp = 888_888L,
                    isStarred = true
                ),
                entity(
                    id = 11,
                    word = "Contract",
                    deckId = VocabularyDecks.CORE_775_ID,
                    deckName = VocabularyDecks.CORE_775_NAME,
                    repetitions = 4,
                    intervalDays = 14,
                    easeFactor = 2.6f,
                    nextReviewTimestamp = 999_999L,
                    lastReviewedTimestamp = 888_888L,
                    isStarred = true
                )
            )
        )
        val repo = VocabularyRepositoryImpl(dao, stubSeeder(), testDispatcher)
        val initial = repo.getAllWords()

        repo.resetWordSm2Progress(initial.first { it.id == 10 })

        val afterReset = repo.getAllWords()
        afterReset.forEach { w ->
            assertEquals(0, w.repetitions)
            assertEquals(0, w.intervalDays)
            assertEquals(2.5f, w.easeFactor, 0.001f)
            assertEquals(0L, w.nextReviewTimestamp)
            assertEquals(0L, w.lastReviewedTimestamp)
            assertTrue("isStarred must be preserved on SM-2 reset", w.isStarred)
        }
    }

    @Test
    fun `editing and deleting custom word succeeds only for custom_user and never affects default decks`() = runTest {
        val dao = InMemoryVocabularyDao(
            listOf(
                entity(1, "abide", VocabularyDecks.ESSENTIAL_600_ID, VocabularyDecks.ESSENTIAL_600_NAME),
                entity(2001, "customterm", VocabularyDecks.CUSTOM_USER_ID, VocabularyDecks.CUSTOM_USER_NAME)
            )
        )
        val repo = VocabularyRepositoryImpl(dao, stubSeeder(), testDispatcher)
        val initial = repo.getAllWords()

        // Attempt to edit default deck word -> must fail
        val defaultEdited = repo.updateCustomWord(
            initial.first { it.id == 1 }.copy(meaningVi = "hacked")
        )
        assertFalse(defaultEdited)
        assertEquals("nghia abide", dao.getAllWords().first { it.id == 1 }.meaningVi)

        // Attempt to delete default deck word -> must fail
        val defaultDeleted = repo.deleteCustomWord(1)
        assertFalse(defaultDeleted)
        assertEquals(2, dao.getAllWords().size)

        // Edit custom_user word -> must succeed
        val customEdited = repo.updateCustomWord(
            initial.first { it.id == 2001 }.copy(
                word = "updatedterm",
                meaningVi = "nghia da cap nhat",
                topic = "Custom Topic"
            )
        )
        assertTrue(customEdited)
        val afterEdit = repo.getCachedAllWords()!!.first { it.id == 2001 }
        assertEquals("updatedterm", afterEdit.word)
        assertEquals("nghia da cap nhat", afterEdit.meaningVi)
        assertEquals("Custom Topic", afterEdit.topic)

        // Delete custom_user word -> must succeed
        val customDeleted = repo.deleteCustomWord(2001)
        assertTrue(customDeleted)
        assertEquals(1, repo.getAllWords().size)
        assertEquals(1, repo.getAllWords().first().id)
    }

    @Test
    fun `VaultFilter STARRED filters starred words and updates counts`() = runTest {
        val dao = InMemoryVocabularyDao(
            listOf(
                entity(1, "alpha", VocabularyDecks.ESSENTIAL_600_ID, VocabularyDecks.ESSENTIAL_600_NAME, isStarred = false),
                entity(2, "beta", VocabularyDecks.ESSENTIAL_600_ID, VocabularyDecks.ESSENTIAL_600_NAME, isStarred = true)
            )
        )
        val repo = VocabularyRepositoryImpl(dao, stubSeeder(), testDispatcher)
        val vm = VocabularyVaultViewModel(
            vocabularyRepository = repo,
            userPreferencesRepository = FakePrefsRepo(),
            createTtsManager = { null },
            defaultDispatcher = testDispatcher
        )

        val collectJob = launch { vm.uiState.collect() }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.counts[VaultFilter.STARRED])
        vm.selectFilter(VaultFilter.STARRED)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.words.size)
        assertEquals("beta", vm.uiState.value.words.first().word)

        // Star alpha too -> starred count becomes 2
        vm.toggleStarred(vm.uiState.value.allScopedWords.first { it.id == 1 })
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.counts[VaultFilter.STARRED])
        assertEquals(2, vm.uiState.value.words.size)

        collectJob.cancel()
    }
}
