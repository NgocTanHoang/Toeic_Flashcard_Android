package com.toeicflashcard.data.model

import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.presentation.home.CUSTOM_TOPIC_CATEGORY
import com.toeicflashcard.presentation.home.topicCategoryFor
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedVocabularyParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun resolveAsset(fileName: String): File {
        val assetFile = listOf(
            File("src/main/assets/$fileName"),
            File("app/src/main/assets/$fileName")
        ).firstOrNull { it.exists() }

        return requireNotNull(assetFile) {
            "Could not locate $fileName in working directory: ${File(".").absolutePath}"
        }
    }

    @Test
    fun `toeic_vocab json contains 30 valid words across 3 topics with 10 words each`() {
        val items = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("toeic_vocab.json").readText()
        )

        assertEquals(30, items.size)

        val expectedTopics = setOf(
            "Business & Office",
            "Finance & Banking",
            "Travel & Logistics"
        )
        val groupedByTopic = items.groupBy { it.topic }
        assertEquals(expectedTopics, groupedByTopic.keys)
        expectedTopics.forEach { topic ->
            assertEquals("Topic $topic must have 10 words", 10, groupedByTopic[topic]?.size)
        }
    }

    @Test
    fun `toeic_vocab_600 json contains 33 topics over 700 words and combines cleanly with base seed`() {
        val baseItems = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("toeic_vocab.json").readText()
        )
        val extraItems = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("toeic_vocab_600.json").readText()
        )

        assertTrue("Expected at least 700 PDF vocabulary items", extraItems.size >= 700)
        assertEquals(33, extraItems.map { it.topic }.toSet().size)

        val combined = baseItems + extraItems
        assertEquals(combined.size, combined.map { it.id }.toSet().size)
        assertEquals(
            combined.size,
            combined.map { it.topic to it.word.trim().lowercase() }.toSet().size
        )

        combined.forEach { item ->
            assertTrue("id must be positive", item.id > 0)
            assertTrue("topic blank for id=${item.id}", item.topic.isNotBlank())
            assertTrue("word blank for id=${item.id}", item.word.isNotBlank())
            assertTrue(
                "phonetic invalid for id=${item.id}: ${item.phonetic}",
                item.phonetic.startsWith("/") && item.phonetic.endsWith("/") && item.phonetic.length > 2
            )
            assertTrue("partOfSpeech blank for id=${item.id}", item.partOfSpeech.isNotBlank())
            assertTrue("meaningVi blank for id=${item.id}", item.meaningVi.isNotBlank())
            assertTrue("exampleEn blank for id=${item.id}", item.exampleEn.isNotBlank())
            assertTrue("exampleVi blank for id=${item.id}", item.exampleVi.isNotBlank())
        }
    }

    @Test
    fun `all 4 seed asset JSON files combine cleanly into 2154 unique words with mapped categories`() {
        val coreBase = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("toeic_vocab.json").readText()
        )
        val coreExtra = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("toeic_vocab_600.json").readText()
        )
        val essential600 = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("deck_essential_600.json").readText()
        )
        val level450 = json.decodeFromString<List<SeedVocabularyDto>>(
            resolveAsset("deck_level_450_800.json").readText()
        )

        assertEquals(600, essential600.size)
        assertEquals(50, essential600.map { it.topic }.toSet().size)
        assertTrue(essential600.all { it.id in 1001..1600 && it.deckId == VocabularyDecks.ESSENTIAL_600_ID })

        assertEquals(779, level450.size)
        assertEquals(18, level450.map { it.topic }.toSet().size)
        assertTrue(level450.all { it.id in 2001..2779 && it.deckId == VocabularyDecks.LEVEL_450_800_ID })

        val allItems = coreBase + coreExtra + essential600 + level450
        assertEquals(2154, allItems.size)
        assertEquals(allItems.size, allItems.map { it.id }.toSet().size)
        assertEquals(
            allItems.size,
            allItems.map { it.topic to it.word.trim().lowercase() }.toSet().size
        )

        allItems.forEach { item ->
            assertTrue("deckId blank for id=${item.id}", item.deckId.isNotBlank())
            assertTrue("deckName blank for id=${item.id}", item.deckName.isNotBlank())
            assertTrue(
                "phonetic invalid for id=${item.id}: ${item.phonetic}",
                item.phonetic.startsWith("/") && item.phonetic.endsWith("/") && item.phonetic.length > 2
            )
            assertTrue("meaningVi blank for id=${item.id}", item.meaningVi.isNotBlank())
            assertTrue(
                "Unmapped topic category for '${item.topic}' (id=${item.id})",
                topicCategoryFor(item.topic) != CUSTOM_TOPIC_CATEGORY
            )
        }
    }
}
