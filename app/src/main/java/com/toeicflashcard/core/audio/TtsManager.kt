package com.toeicflashcard.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed interface TtsFeedback {
    data object Spoken : TtsFeedback
    data object Initializing : TtsFeedback
    data class MissingVoiceData(val messageVi: String) : TtsFeedback
    data class Unavailable(val messageVi: String) : TtsFeedback
}

class TtsManager(
    context: Context,
    private val onDeferredFeedback: ((TtsFeedback) -> Unit)? = null,
    eagerInit: Boolean = false
) {
    private enum class State {
        IDLE,
        INITIALIZING,
        READY,
        MISSING_VOICE_DATA,
        ERROR,
        SHUT_DOWN
    }

    private val appContext = context.applicationContext
    private val lock = Any()

    @Volatile
    private var state: State = State.IDLE

    private var tts: TextToSpeech? = null
    private var pendingText: String? = null
    private var pendingRate: Float = DEFAULT_SPEECH_RATE
    private var hasActiveSpeech: Boolean = false

    init {
        if (eagerInit) {
            warmUp()
        }
    }

    /**
     * Triggers lazy TextToSpeech engine construction off the main thread.
     */
    fun warmUp() {
        synchronized(lock) {
            if (state != State.IDLE) return
            state = State.INITIALIZING
        }
        ttsExecutor.execute {
            initializeEngine()
        }
    }

    private fun initializeEngine() {
        synchronized(lock) {
            if (state == State.SHUT_DOWN) return
        }
        try {
            val engine = TextToSpeech(appContext) { status ->
                ttsExecutor.execute {
                    handleEngineInitialized(status)
                }
            }
            var shouldShutdown = false
            synchronized(lock) {
                if (state == State.SHUT_DOWN) {
                    shouldShutdown = true
                } else {
                    tts = engine
                }
            }
            if (shouldShutdown) {
                try {
                    engine.stop()
                    engine.shutdown()
                } catch (_: Exception) {
                    // Ignore OEM TTS teardown errors
                }
            }
        } catch (_: Exception) {
            val failure = synchronized(lock) {
                if (state != State.SHUT_DOWN) {
                    state = State.ERROR
                    hasActiveSpeech = false
                    consumePendingLocked(
                        TtsFeedback.Unavailable(
                            "Không thể khởi tạo trình đọc giọng nói trên thiết bị này."
                        )
                    )
                } else {
                    null
                }
            }
            failure?.let { onDeferredFeedback?.invoke(it) }
        }
    }

    private fun handleEngineInitialized(status: Int) {
        var engine: TextToSpeech? = null
        var immediateFailure: TtsFeedback? = null
        synchronized(lock) {
            if (state == State.SHUT_DOWN) return
            if (status != TextToSpeech.SUCCESS) {
                state = State.ERROR
                hasActiveSpeech = false
                immediateFailure = consumePendingLocked(
                    TtsFeedback.Unavailable(
                        "Không thể khởi tạo trình đọc giọng nói trên thiết bị này."
                    )
                )
                return@synchronized
            }
            engine = tts
            if (engine == null) {
                state = State.ERROR
                hasActiveSpeech = false
                immediateFailure = consumePendingLocked(
                    TtsFeedback.Unavailable(
                        "Trình đọc giọng nói hiện không khả dụng."
                    )
                )
            }
        }
        if (immediateFailure != null || status != TextToSpeech.SUCCESS) {
            immediateFailure?.let { onDeferredFeedback?.invoke(it) }
            return
        }
        val readyEngine = engine ?: return

        val usStatus = try {
            readyEngine.setLanguage(Locale.US)
        } catch (_: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }

        val resolvedStatus = if (
            usStatus == TextToSpeech.LANG_MISSING_DATA ||
            usStatus == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            try {
                readyEngine.setLanguage(Locale.ENGLISH)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
        } else {
            usStatus
        }

        val pendingToSpeak: Pair<String, Float>?
        var deferredFailure: TtsFeedback? = null
        synchronized(lock) {
            if (state == State.SHUT_DOWN) return
            if (
                resolvedStatus == TextToSpeech.LANG_MISSING_DATA ||
                resolvedStatus == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                state = State.MISSING_VOICE_DATA
                hasActiveSpeech = false
                deferredFailure = consumePendingLocked(
                    TtsFeedback.MissingVoiceData(
                        "Thiết bị chưa cài gói giọng nói tiếng Anh (US). Vui lòng tải trong Cài đặt Text-to-Speech."
                    )
                )
                pendingToSpeak = null
            } else {
                state = State.READY
                val textToSpeak = pendingText
                val rateToUse = pendingRate
                pendingText = null
                pendingToSpeak = if (!textToSpeak.isNullOrBlank()) {
                    hasActiveSpeech = true
                    textToSpeak to rateToUse
                } else {
                    null
                }
            }
        }
        deferredFailure?.let { onDeferredFeedback?.invoke(it) }
        pendingToSpeak?.let { (textToSpeak, rateToUse) ->
            val feedback = speakInternal(readyEngine, textToSpeak, rateToUse)
            if (feedback !is TtsFeedback.Spoken) {
                synchronized(lock) { hasActiveSpeech = false }
                onDeferredFeedback?.invoke(feedback)
            }
        }
    }

    fun speak(text: String, speechRate: Float = DEFAULT_SPEECH_RATE): TtsFeedback {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return TtsFeedback.Spoken

        var engineToSpeak: TextToSpeech? = null
        var needInit = false
        val clampedRate = speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)

        val immediateResult: TtsFeedback? = synchronized(lock) {
            when (state) {
                State.SHUT_DOWN -> TtsFeedback.Unavailable("Trình phát âm đã tạm dừng.")
                State.IDLE -> {
                    pendingText = cleanText
                    pendingRate = clampedRate
                    state = State.INITIALIZING
                    needInit = true
                    TtsFeedback.Initializing
                }
                State.INITIALIZING -> {
                    pendingText = cleanText
                    pendingRate = clampedRate
                    TtsFeedback.Initializing
                }
                State.MISSING_VOICE_DATA -> TtsFeedback.MissingVoiceData(
                    "Thiết bị chưa cài gói giọng nói tiếng Anh (US). Vui lòng tải trong Cài đặt Text-to-Speech."
                )
                State.ERROR -> TtsFeedback.Unavailable(
                    "Trình đọc giọng nói hiện không khả dụng trên thiết bị này."
                )
                State.READY -> {
                    engineToSpeak = tts
                    if (engineToSpeak == null) {
                        TtsFeedback.Unavailable("Trình đọc giọng nói hiện không khả dụng.")
                    } else {
                        hasActiveSpeech = true
                        null
                    }
                }
            }
        }

        if (needInit) {
            ttsExecutor.execute {
                initializeEngine()
            }
        }
        if (immediateResult != null) return immediateResult

        val readyEngine = requireNotNull(engineToSpeak)
        ttsExecutor.execute {
            val canSpeak = synchronized(lock) {
                state == State.READY && tts === readyEngine
            }
            if (!canSpeak) return@execute
            val feedback = speakInternal(readyEngine, cleanText, clampedRate)
            if (feedback !is TtsFeedback.Spoken) {
                synchronized(lock) { hasActiveSpeech = false }
                onDeferredFeedback?.invoke(feedback)
            }
        }
        return TtsFeedback.Spoken
    }

    private fun speakInternal(
        engine: TextToSpeech,
        text: String,
        speechRate: Float
    ): TtsFeedback {
        return try {
            engine.setSpeechRate(speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE))
            val result = engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "toeic_tts_${System.nanoTime()}"
            )
            if (result == TextToSpeech.SUCCESS) {
                TtsFeedback.Spoken
            } else {
                TtsFeedback.Unavailable("Không thể phát âm thanh lúc này.")
            }
        } catch (_: Exception) {
            TtsFeedback.Unavailable("Đã xảy ra lỗi khi phát âm thanh.")
        }
    }

    private fun consumePendingLocked(feedback: TtsFeedback): TtsFeedback? {
        return if (pendingText != null) {
            pendingText = null
            feedback
        } else {
            null
        }
    }

    fun stop() {
        val engineToStop = synchronized(lock) {
            pendingText = null
            if (!hasActiveSpeech) return
            hasActiveSpeech = false
            tts ?: return
        }
        ttsExecutor.execute {
            try {
                engineToStop.stop()
            } catch (_: Exception) {
                // Ignore shutdown/stop race conditions on OEM TTS engines
            }
        }
    }

    fun shutdown() {
        val engineToShutdown = synchronized(lock) {
            state = State.SHUT_DOWN
            pendingText = null
            hasActiveSpeech = false
            val current = tts
            tts = null
            current
        } ?: return

        ttsExecutor.execute {
            try {
                engineToShutdown.stop()
                engineToShutdown.shutdown()
            } catch (_: Exception) {
                // Ignore OEM TTS teardown errors
            }
        }
    }

    companion object {
        const val DEFAULT_SPEECH_RATE = 0.95f
        const val SLOW_SPEECH_RATE = 0.75f
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 1.5f

        private val ttsExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "ToeicTtsWorker").apply { isDaemon = true }
        }
    }
}
