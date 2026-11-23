package com.toeicflashcard

import androidx.compose.ui.unit.dp
import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.core.ui.glass.AdaptiveCardMinWidth
import com.toeicflashcard.core.ui.glass.AdaptiveWideBreakpoint
import com.toeicflashcard.core.ui.glass.AppWindowMetrics
import com.toeicflashcard.core.ui.glass.MainContentMaxWidth
import com.toeicflashcard.core.ui.glass.adaptiveHorizontalPadding
import com.toeicflashcard.core.ui.glass.gridItemSpacing
import com.toeicflashcard.core.ui.glass.screenHorizontalPadding
import com.toeicflashcard.presentation.study.components.computeFlashcardLayoutMetrics
import com.toeicflashcard.data.model.SeedVocabularyDto
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.usecase.ObserveHomeSummaryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.floor

class ResponsiveAndIconJvmRenderTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun resolveAsset(fileName: String): File {
        return listOf(
            File("src/main/assets/$fileName"),
            File("app/src/main/assets/$fileName")
        ).first { it.exists() }
    }

    private fun resolveProjectRoot(): File {
        return if (File("src/main/assets").exists()) File("..") else File(".")
    }

    private fun loadSeededWords(): List<VocabularyWord> {
        val allDtos = listOf(
            "toeic_vocab.json",
            "toeic_vocab_600.json",
            "deck_essential_600.json",
            "deck_level_450_800.json"
        ).flatMap { fileName ->
            json.decodeFromString<List<SeedVocabularyDto>>(resolveAsset(fileName).readText())
        }
        return allDtos.map { dto ->
            VocabularyWord(
                id = dto.id,
                topic = dto.topic,
                word = dto.word,
                phonetic = dto.phonetic,
                partOfSpeech = dto.partOfSpeech,
                meaningVi = dto.meaningVi,
                exampleEn = dto.exampleEn,
                exampleVi = dto.exampleVi,
                deckId = dto.deckId,
                deckName = dto.deckName
            )
        }
    }

    @Test
    fun `dynamic summary updates without hardcoded counts and 2-axis grid columns match OPPO Pad SE and Redmi Pad 2`() = runTest {
        val domainWords = loadSeededWords()
        val wordsFlow = MutableStateFlow(domainWords)

        val repo = object : VocabularyRepository {
            override suspend fun ensureSeeded() = Unit
            override fun observeAllWords(): Flow<List<VocabularyWord>> = wordsFlow
            override suspend fun getAllWords(): List<VocabularyWord> = wordsFlow.value
            override suspend fun getDueWords(cutoffMillis: Long): List<VocabularyWord> = wordsFlow.value
            override suspend fun getWordsByTopic(topic: String): List<VocabularyWord> =
                wordsFlow.value.filter { it.topic == topic }
            override suspend fun updateWord(word: VocabularyWord) = Unit
            override suspend fun markWordsForUrgentReview(wordIds: List<Int>, nowMillis: Long) = Unit
        }
        val timeProvider = object : TimeProvider {
            override fun nowMillis(): Long = 1_700_000_000_000L
            override fun endOfTodayMillis(): Long = 1_700_086_400_000L
            override fun todayEpochDay(): Long = 19_675L
            override fun epochDayForMillis(timestampMillis: Long): Long = 19_675L
        }
        val observeSummary = ObserveHomeSummaryUseCase(
            vocabularyRepository = repo,
            timeProvider = timeProvider,
            defaultDispatcher = Dispatchers.Unconfined
        )

        val summary = observeSummary(flowOf(VocabularyDecks.ALL_ID)).first()
        assertEquals(domainWords.size, summary.totalCount)
        assertTrue(summary.decks.all { it.topicCount > 0 || it.deckId == VocabularyDecks.CUSTOM_USER_ID })

        // Adding a custom word in a brand-new topic must update totalCount and topics.size dynamically
        wordsFlow.value = domainWords + VocabularyWord(
            id = 3001,
            topic = "Từ vựng cá nhân",
            word = "benchmark",
            phonetic = "/ˈbentʃmɑːrk/",
            partOfSpeech = "n.",
            meaningVi = "chuẩn đối chiếu",
            exampleEn = "Set a clear benchmark.",
            exampleVi = "Đặt chuẩn đối chiếu rõ ràng.",
            deckId = VocabularyDecks.CUSTOM_USER_ID,
            deckName = VocabularyDecks.CUSTOM_USER_NAME
        )
        val summaryAfterAdd = observeSummary(flowOf(VocabularyDecks.ALL_ID)).first()
        assertEquals(domainWords.size + 1, summaryAfterAdd.totalCount)
        assertEquals(summary.topics.size + 1, summaryAfterAdd.topics.size)

        // OPPO Pad SE 7:5 portrait (1600x2240 @ 280dpi -> ~914.3dp x 1280dp) => centered 2 columns
        val oppoW = (1600f / (280f / 160f)).dp
        val oppoH = (2240f / (280f / 160f)).dp
        val oppoMetrics = AppWindowMetrics(oppoW, oppoH)
        assertTrue(oppoMetrics.isExpandedWidth)
        val oppoPad = adaptiveHorizontalPadding(oppoW, MainContentMaxWidth, 18.dp)
        val oppoContentW = oppoW.value - oppoPad.value * 2f
        val oppoCols = floor((oppoContentW + 12f) / (AdaptiveCardMinWidth.value + 12f)).toInt()
        assertEquals(2, oppoCols)

        // Redmi Pad 2 16:10 landscape (2560x1600 @ 274dpi -> ~1494.9dp x 934.3dp) => centered 3 columns
        val redmiW = (2560f / (274f / 160f)).dp
        val redmiH = (1600f / (274f / 160f)).dp
        val redmiMetrics = AppWindowMetrics(redmiW, redmiH)
        assertTrue(redmiMetrics.isExpandedWidth)
        val redmiPad = adaptiveHorizontalPadding(redmiW, MainContentMaxWidth, 18.dp)
        val redmiContentW = redmiW.value - redmiPad.value * 2f
        val redmiCols = floor((redmiContentW + 12f) / (AdaptiveCardMinWidth.value + 12f)).toInt()
        assertEquals(3, redmiCols)
    }

    @Test
    fun `verifies v3 icon preview and real emulator responsive screenshots exist`() {
        val root = resolveProjectRoot()
        val iconFile = File(root, "media/app_icon_preview_v3.png")
        assertTrue("Missing media/app_icon_preview_v3.png", iconFile.exists() && iconFile.length() > 50_000L)

        // Local-only verification: screenshots_responsive/ is gitignored in CI
        val screenshotsDir = File(root, "screenshots_responsive")
        if (screenshotsDir.exists()) {
            val capturedFiles = screenshotsDir.listFiles { file ->
                file.isFile && (file.extension.equals("png", true) || file.extension.equals("jpg", true))
            }.orEmpty()
            assertTrue(
                "Expected at least one captured responsive screenshot (> 100 KB) in screenshots_responsive/",
                capturedFiles.any { it.length() > 100_000L }
            )
        }
    }

    @Test
    fun `galaxyZFoldThreeScreenGroupsVerifyNarrowCoverShortCoverAndFoldInnerMetrics`() {
        // Group A: Galaxy Z Fold 5/6 narrow cover (904x2316 @ 420dpi -> ~344.4dp x 882.3dp)
        val fold5W = (904f / (420f / 160f)).dp
        val fold5H = (2316f / (420f / 160f)).dp
        val fold5Metrics = AppWindowMetrics(fold5W, fold5H)
        assertTrue(fold5Metrics.isNarrowCover)
        assertFalse(fold5Metrics.isExpandedWidth)
        assertFalse(fold5Metrics.isShortCover)
        assertFalse(fold5Metrics.isFoldInnerExpanded)
        assertEquals(12.dp, screenHorizontalPadding(fold5W))

        // Group B: Galaxy Z Fold 8 5.5" 10:16 short cover (1248x1972 @ 420dpi -> ~475.4dp x 751.2dp)
        val fold8CoverW = (1248f / (420f / 160f)).dp
        val fold8CoverH = (1972f / (420f / 160f)).dp
        val fold8CoverMetrics = AppWindowMetrics(fold8CoverW, fold8CoverH)
        assertFalse(fold8CoverMetrics.isNarrowCover)
        assertFalse(fold8CoverMetrics.isExpandedWidth)
        assertTrue(fold8CoverMetrics.isShortCover)
        assertFalse(fold8CoverMetrics.isFoldInnerExpanded)
        assertEquals(16.dp, screenHorizontalPadding(fold8CoverW))
        val fold8Card = computeFlashcardLayoutMetrics(fold8CoverW, fold8CoverH)
        assertEquals(20.dp, fold8Card.horizontalPadding)
        assertEquals(16.dp, fold8Card.verticalPadding)
        assertEquals(10.dp, fold8Card.sectionSpacing)

        // Group C: Galaxy Z Fold Inner 7.6" (1848x2448 @ 370dpi -> ~799.1dp x 1058.6dp)
        val foldInnerW = (1848f / (370f / 160f)).dp
        val foldInnerH = (2448f / (370f / 160f)).dp
        val foldInnerMetrics = AppWindowMetrics(foldInnerW, foldInnerH)
        assertFalse(foldInnerMetrics.isNarrowCover)
        assertFalse(foldInnerMetrics.isShortCover)
        assertTrue(foldInnerMetrics.isExpandedWidth)
        assertTrue(foldInnerMetrics.isFoldInnerExpanded)
        assertTrue(foldInnerMetrics.foldInnerScale > 1.0f)
        assertEquals(22.dp, screenHorizontalPadding(foldInnerW))
        assertEquals(14.dp, gridItemSpacing(foldInnerW))
        val innerPad = adaptiveHorizontalPadding(foldInnerW, MainContentMaxWidth)
        val innerGap = gridItemSpacing(foldInnerW).value
        val innerCols = floor((foldInnerW.value - innerPad.value * 2f + innerGap) / (AdaptiveCardMinWidth.value + innerGap)).toInt()
        assertEquals(2, innerCols)
    }
}
