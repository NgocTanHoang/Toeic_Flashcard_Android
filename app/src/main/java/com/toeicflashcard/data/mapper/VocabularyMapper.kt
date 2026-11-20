package com.toeicflashcard.data.mapper

import com.toeicflashcard.data.local.entity.VocabularyEntity
import com.toeicflashcard.data.model.SeedVocabularyDto
import com.toeicflashcard.domain.model.VocabularyWord
import com.toeicflashcard.domain.scheduler.Sm2Scheduler

fun SeedVocabularyDto.toEntity(): VocabularyEntity {
    return VocabularyEntity(
        id = id,
        topic = topic,
        word = word,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        meaningVi = meaningVi,
        exampleEn = exampleEn,
        exampleVi = exampleVi,
        repetitions = 0,
        intervalDays = 0,
        easeFactor = Sm2Scheduler.DEFAULT_EASE_FACTOR,
        nextReviewTimestamp = 0L,
        lastReviewedTimestamp = 0L,
        deckId = deckId,
        deckName = deckName
    )
}

fun VocabularyEntity.toDomain(): VocabularyWord {
    return VocabularyWord(
        id = id,
        topic = topic,
        word = word,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        meaningVi = meaningVi,
        exampleEn = exampleEn,
        exampleVi = exampleVi,
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        nextReviewTimestamp = nextReviewTimestamp,
        lastReviewedTimestamp = lastReviewedTimestamp,
        deckId = deckId,
        deckName = deckName
    )
}

fun VocabularyWord.toEntity(): VocabularyEntity {
    return VocabularyEntity(
        id = id,
        topic = topic,
        word = word,
        phonetic = phonetic,
        partOfSpeech = partOfSpeech,
        meaningVi = meaningVi,
        exampleEn = exampleEn,
        exampleVi = exampleVi,
        repetitions = repetitions,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        nextReviewTimestamp = nextReviewTimestamp,
        lastReviewedTimestamp = lastReviewedTimestamp,
        deckId = deckId,
        deckName = deckName
    )
}
