package com.toeicflashcard.domain.usecase

import com.toeicflashcard.core.time.TimeProvider
import com.toeicflashcard.domain.model.VocabularyDecks
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.repository.VocabularyRepository
import com.toeicflashcard.domain.scheduler.Sm2Scheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

data class CustomWordInput(
    val word: String,
    val phonetic: String = "",
    val partOfSpeech: String = "n.",
    val meaningVi: String,
    val topic: String = AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC,
    val exampleEn: String = "",
    val exampleVi: String = ""
)

sealed interface AddWordResult {
    data class Success(val addedWord: VocabularyWord) : AddWordResult
    data class ValidationError(val messageVi: String) : AddWordResult
    data class DuplicateWord(val existingWord: String) : AddWordResult
}

data class ImportJsonResult(
    val addedCount: Int,
    val skippedDuplicateCount: Int,
    val skippedInvalidCount: Int,
    val errorMessageVi: String? = null
)

@Serializable
data class CustomVocabularyImportDto(
    val word: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "n.",
    val meaningVi: String = "",
    val topic: String = AddCustomVocabularyUseCase.DEFAULT_CUSTOM_TOPIC,
    val exampleEn: String = "",
    val exampleVi: String = ""
)

class AddCustomVocabularyUseCase(
    private val vocabularyRepository: VocabularyRepository,
    private val timeProvider: TimeProvider,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun addSingleWord(input: CustomWordInput): AddWordResult = withContext(defaultDispatcher) {
        val cleanWord = input.word.trim()
        if (cleanWord.isEmpty()) {
            return@withContext AddWordResult.ValidationError("Vui lòng nhập từ tiếng Anh.")
        }
        val cleanMeaning = input.meaningVi.trim()
        if (cleanMeaning.isEmpty()) {
            return@withContext AddWordResult.ValidationError("Vui lòng nhập nghĩa tiếng Việt.")
        }

        vocabularyRepository.ensureSeeded()
        val existingWords = vocabularyRepository.getAllWords()
        val key = normalizeWordKey(cleanWord)
        if (existingWords.any { normalizeWordKey(it.word) == key }) {
            return@withContext AddWordResult.DuplicateWord(cleanWord)
        }

        val nowMillis = timeProvider.nowMillis()
        var candidateId = nextStartingWordId()
        val domainWord = buildCustomWord(
            id = candidateId,
            cleanWord = cleanWord,
            phonetic = input.phonetic.trim(),
            partOfSpeech = input.partOfSpeech.trim().ifEmpty { DEFAULT_PART_OF_SPEECH },
            cleanMeaning = cleanMeaning,
            topic = input.topic.trim().ifEmpty { DEFAULT_CUSTOM_TOPIC },
            exampleEn = input.exampleEn.trim(),
            exampleVi = input.exampleVi.trim(),
            nowMillis = nowMillis
        )

        val inserted = vocabularyRepository.insertCustomWord(domainWord)
        if (inserted) {
            return@withContext AddWordResult.Success(domainWord)
        }

        // Retry once with refreshed max ID if a concurrent insert collided on primary key.
        candidateId = nextStartingWordId()
        val retriedWord = domainWord.copy(id = candidateId)
        return@withContext if (vocabularyRepository.insertCustomWord(retriedWord)) {
            AddWordResult.Success(retriedWord)
        } else {
            AddWordResult.ValidationError("Không thể lưu từ mới lúc này. Vui lòng thử lại.")
        }
    }

    suspend fun importFromJson(rawJson: String): ImportJsonResult = withContext(defaultDispatcher) {
        val sanitized = rawJson.trimStart('\uFEFF').trim()
        if (sanitized.isEmpty()) {
            return@withContext ImportJsonResult(
                addedCount = 0,
                skippedDuplicateCount = 0,
                skippedInvalidCount = 0,
                errorMessageVi = "File JSON trống."
            )
        }

        val items = runCatching {
            jsonParser.decodeFromString<List<CustomVocabularyImportDto>>(sanitized)
        }.getOrElse {
            return@withContext ImportJsonResult(
                addedCount = 0,
                skippedDuplicateCount = 0,
                skippedInvalidCount = 0,
                errorMessageVi = "File JSON không đúng định dạng mảng từ vựng. Vui lòng xem mẫu."
            )
        }

        if (items.isEmpty()) {
            return@withContext ImportJsonResult(
                addedCount = 0,
                skippedDuplicateCount = 0,
                skippedInvalidCount = 0,
                errorMessageVi = "Danh sách từ vựng trong file JSON đang trống."
            )
        }

        vocabularyRepository.ensureSeeded()
        val seenKeys = vocabularyRepository.getAllWords()
            .mapTo(HashSet()) { normalizeWordKey(it.word) }

        var nextId = nextStartingWordId()
        val nowMillis = timeProvider.nowMillis()
        val toInsert = ArrayList<VocabularyWord>(items.size)
        var skippedDuplicate = 0
        var skippedInvalid = 0

        for (dto in items) {
            val cleanWord = dto.word.trim()
            val cleanMeaning = dto.meaningVi.trim()
            if (cleanWord.isEmpty() || cleanMeaning.isEmpty()) {
                skippedInvalid++
                continue
            }

            val key = normalizeWordKey(cleanWord)
            if (!seenKeys.add(key)) {
                skippedDuplicate++
                continue
            }

            toInsert += buildCustomWord(
                id = nextId++,
                cleanWord = cleanWord,
                phonetic = dto.phonetic.trim(),
                partOfSpeech = dto.partOfSpeech.trim().ifEmpty { DEFAULT_PART_OF_SPEECH },
                cleanMeaning = cleanMeaning,
                topic = dto.topic.trim().ifEmpty { DEFAULT_CUSTOM_TOPIC },
                exampleEn = dto.exampleEn.trim(),
                exampleVi = dto.exampleVi.trim(),
                nowMillis = nowMillis
            )
        }

        val addedCount = if (toInsert.isNotEmpty()) {
            vocabularyRepository.insertCustomWords(toInsert)
        } else {
            0
        }

        ImportJsonResult(
            addedCount = addedCount,
            skippedDuplicateCount = skippedDuplicate,
            skippedInvalidCount = skippedInvalid,
            errorMessageVi = null
        )
    }

    private suspend fun nextStartingWordId(): Int {
        val currentMax = vocabularyRepository.getMaxWordId() ?: MIN_CUSTOM_ID_BASE
        return maxOf(currentMax, MIN_CUSTOM_ID_BASE) + 1
    }

    private fun buildCustomWord(
        id: Int,
        cleanWord: String,
        phonetic: String,
        partOfSpeech: String,
        cleanMeaning: String,
        topic: String,
        exampleEn: String,
        exampleVi: String,
        nowMillis: Long
    ): VocabularyWord {
        return VocabularyWord(
            id = id,
            topic = topic,
            word = cleanWord,
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            meaningVi = cleanMeaning,
            exampleEn = exampleEn,
            exampleVi = exampleVi,
            repetitions = 0,
            intervalDays = 0,
            easeFactor = Sm2Scheduler.DEFAULT_EASE_FACTOR,
            nextReviewTimestamp = nowMillis,
            lastReviewedTimestamp = 0L,
            deckId = VocabularyDecks.CUSTOM_USER_ID,
            deckName = VocabularyDecks.CUSTOM_USER_NAME
        )
    }

    private fun normalizeWordKey(word: String): String =
        word.trim().lowercase(Locale.ROOT)

    companion object {
        const val MIN_CUSTOM_ID_BASE = 3000
        const val DEFAULT_CUSTOM_TOPIC = "Từ vựng cá nhân"
        private const val DEFAULT_PART_OF_SPEECH = "n."

        val SAMPLE_JSON_TEMPLATE: String = """
            [
              {
                "word": "milestone",
                "phonetic": "/ˈmaɪlstoʊn/",
                "partOfSpeech": "n.",
                "meaningVi": "cột mốc quan trọng",
                "topic": "Từ vựng cá nhân",
                "exampleEn": "Completing the audit ahead of schedule was a major milestone.",
                "exampleVi": "Hoàn thành kiểm toán trước thời hạn là một cột mốc quan trọng."
              }
            ]
        """.trimIndent()
    }
}
