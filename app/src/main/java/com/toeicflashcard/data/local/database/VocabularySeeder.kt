package com.toeicflashcard.data.local.database

import android.content.Context
import com.toeicflashcard.data.local.dao.VocabularyDao
import com.toeicflashcard.data.mapper.toEntity
import com.toeicflashcard.data.model.SeedVocabularyDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class VocabularySeeder(private val context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var seededVerified = false

    suspend fun seedIfEmpty(dao: VocabularyDao) {
        if (seededVerified) return
        mutex.withLock {
            if (seededVerified) return
            withContext(Dispatchers.IO) {
                val seededVersion = prefs.getInt(KEY_SEEDED_VERSION, 0)
                val currentCount = dao.getCount()
                if (seededVersion < SEED_VERSION || currentCount == 0) {
                    val allEntities = ASSET_FILES.flatMap { assetFile ->
                        val rawJson = appContext.assets
                            .open(assetFile)
                            .bufferedReader()
                            .use { it.readText() }
                        json.decodeFromString<List<SeedVocabularyDto>>(rawJson)
                            .map { it.toEntity() }
                    }
                    if (allEntities.isNotEmpty()) {
                        dao.insertAll(allEntities)
                    }
                    prefs.edit().putInt(KEY_SEEDED_VERSION, SEED_VERSION).apply()
                }
                seededVerified = true
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "toeic_seeder_meta"
        private const val KEY_SEEDED_VERSION = "seeded_version"
        private const val SEED_VERSION = 3

        val ASSET_FILES = listOf(
            "toeic_vocab.json",
            "toeic_vocab_600.json",
            "deck_essential_600.json",
            "deck_level_450_800.json"
        )
    }
}
