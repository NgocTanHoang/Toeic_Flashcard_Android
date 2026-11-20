package com.toeicflashcard.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.VocabularyDecks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DarkTonePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun withRepository(
        block: suspend (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>, UserPreferencesRepositoryImpl) -> Unit
    ) {
        val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = ioScope,
                produceFile = { tempFolder.newFile("toeic_test_prefs_${System.nanoTime()}.preferences_pb") }
            )
            val repo = UserPreferencesRepositoryImpl(dataStore)
            runBlocking {
                block(dataStore, repo)
            }
        } finally {
            ioScope.cancel()
        }
    }

    @Test
    fun `darkTone contains all 7 presets, defaults to OLED_OBSIDIAN, and persists across all presets`() = withRepository { _, repo ->
        assertEquals(7, DarkTone.entries.size)
        assertEquals(DarkTone.OLED_OBSIDIAN, repo.userPreferences.first().darkTone)

        for (tone in DarkTone.entries) {
            repo.setDarkTone(tone)
            assertEquals(tone, repo.userPreferences.first().darkTone)
        }
    }

    @Test
    fun `setDarkTone with forceDarkMode atomically updates both darkTone and themeMode`() = withRepository { _, repo ->
        repo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repo.userPreferences.first().themeMode)

        repo.setDarkTone(DarkTone.GRUVBOX_DARK, forceDarkMode = true)
        val updated = repo.userPreferences.first()
        assertEquals(DarkTone.GRUVBOX_DARK, updated.darkTone)
        assertEquals(ThemeMode.DARK, updated.themeMode)
    }

    @Test
    fun `legacy and unknown dark_tone preference values resolve safely`() = withRepository { dataStore, repo ->
        dataStore.edit { prefs -> prefs[stringPreferencesKey("dark_tone")] = "oled_black" }
        assertEquals(DarkTone.OLED_OBSIDIAN, repo.userPreferences.first().darkTone)

        dataStore.edit { prefs -> prefs[stringPreferencesKey("dark_tone")] = "midnight_slate" }
        assertEquals(DarkTone.GITHUB_DARK, repo.userPreferences.first().darkTone)

        dataStore.edit { prefs -> prefs[stringPreferencesKey("dark_tone")] = "warm_night" }
        assertEquals(DarkTone.GRUVBOX_DARK, repo.userPreferences.first().darkTone)

        dataStore.edit { prefs -> prefs[stringPreferencesKey("dark_tone")] = "non_existent_tone" }
        assertEquals(DarkTone.OLED_OBSIDIAN, repo.userPreferences.first().darkTone)
    }

    @Test
    fun `selectedDeckId defaults to ALL, persists selectable decks, and falls back safely`() = withRepository { dataStore, repo ->
        assertEquals(VocabularyDecks.ALL_ID, repo.userPreferences.first().selectedDeckId)

        for (deckId in VocabularyDecks.orderedSelectableIds) {
            repo.setSelectedDeckId(deckId)
            assertEquals(deckId, repo.userPreferences.first().selectedDeckId)
        }

        repo.setSelectedDeckId("unknown_deck_id")
        assertEquals(VocabularyDecks.ALL_ID, repo.userPreferences.first().selectedDeckId)

        dataStore.edit { prefs -> prefs[stringPreferencesKey("selected_deck_id")] = "stale_deck" }
        assertEquals(VocabularyDecks.ALL_ID, repo.userPreferences.first().selectedDeckId)
    }
}
