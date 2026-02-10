/*
 * Copyright (C) 2024 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

private const val LOGTAG = "[TextToSpeechManager-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@Single
class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage: String = "en"

    private val sentenceQueue = ConcurrentLinkedQueue<String>()
    private var isSpeaking = false
    private var utteranceCounter = 0

    private var previousText = ""
    private var pendingBuffer = ""

    private val sentenceEndRegex = Regex("[.!?](?:\\s+|$)")

    // Flow to signal when all speech has finished (queue is empty)
    // extraBufferCapacity = 1 ensures tryEmit succeeds even if collector is busy
    private val _allSpeechFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val allSpeechFinished: SharedFlow<Unit> = _allSpeechFinished

    // StateFlow to expose whether TTS is currently speaking
    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow

    // Flag to track if we're in a speech session (from speakChunk to speakRemainingBuffer)
    private var isSpeechSessionActive = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                LOGD("TTS initialized successfully")
                setupUtteranceListener()
                // Set default language
                setLanguage(currentLanguage)
            } else {
                LOGD("TTS initialization failed")
                isInitialized = false
            }
        }
    }

    /**
     * Set the TTS language using a language code (e.g., "en", "de", "fr").
     * Returns true if the language is supported, false otherwise.
     */
    fun setLanguage(languageCode: String): Boolean {
        if (!isInitialized) return false

        currentLanguage = languageCode
        val locale = when (languageCode) {
            "en" -> Locale.US
            "de" -> Locale.GERMAN
            "fr" -> Locale.FRENCH
            "es" -> Locale("es", "ES")
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "PT")
            "nl" -> Locale("nl", "NL")
            "pl" -> Locale("pl", "PL")
            "ru" -> Locale("ru", "RU")
            "zh" -> Locale.CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "ar" -> Locale("ar", "SA")
            "hi" -> Locale("hi", "IN")
            "tr" -> Locale("tr", "TR")
            "uk" -> Locale("uk", "UA")
            "cs" -> Locale("cs", "CZ")
            "sv" -> Locale("sv", "SE")
            else -> Locale.US
        }

        val result = tts?.setLanguage(locale)
        val isSupported = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED

        if (isSupported) {
            LOGD("TTS language set to: $languageCode ($locale)")
        } else {
            LOGD("TTS language not supported: $languageCode, falling back to default")
            tts?.setLanguage(Locale.US)
        }

        return isSupported
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                _isSpeakingFlow.value = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                _isSpeakingFlow.value = sentenceQueue.isNotEmpty() // Still speaking if queue has more
                speakNextInQueue()
                checkAndEmitSpeechFinished()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                _isSpeakingFlow.value = sentenceQueue.isNotEmpty()
                speakNextInQueue()
                checkAndEmitSpeechFinished()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
                _isSpeakingFlow.value = sentenceQueue.isNotEmpty()
                speakNextInQueue()
                checkAndEmitSpeechFinished()
            }
        })
    }

    private fun checkAndEmitSpeechFinished() {
        LOGD("checkAndEmitSpeechFinished: queueEmpty=${sentenceQueue.isEmpty()}, isSpeaking=$isSpeaking, sessionActive=$isSpeechSessionActive")
        // If the queue is empty and we're not speaking, the session is finished
        if (sentenceQueue.isEmpty() && !isSpeaking && isSpeechSessionActive) {
            isSpeechSessionActive = false
            LOGD("All speech finished, emitting signal")
            val emitted = _allSpeechFinished.tryEmit(Unit)
            LOGD("tryEmit result: $emitted")
        }
    }

    fun speakChunk(fullText: String) {
        if (!isInitialized) return

        // Mark speech session as active when we start receiving chunks
        isSpeechSessionActive = true

        val newContent = if (fullText.startsWith(previousText)) {
            fullText.removePrefix(previousText)
        } else {
            fullText
        }
        previousText = fullText

        val textToProcess = pendingBuffer + newContent
        val sentences = extractCompleteSentences(textToProcess)

        pendingBuffer = sentences.remaining

        sentences.completed.forEach { sentence ->
            if (sentence.isNotBlank()) {
                queueSentence(sentence.trim())
            }
        }
    }

    fun speakRemainingBuffer() {
        LOGD("speakRemainingBuffer called, isInitialized=$isInitialized, pendingBuffer='$pendingBuffer', isSpeaking=$isSpeaking")
        if (!isInitialized) return

        // Re-activate the speech session in case it was prematurely marked as finished
        // (e.g., when TTS finished speaking queued sentences while generation was still ongoing)
        isSpeechSessionActive = true

        if (pendingBuffer.isNotBlank()) {
            LOGD("Queueing remaining buffer: '$pendingBuffer'")
            queueSentence(pendingBuffer.trim())
            pendingBuffer = ""
        } else {
            // If there's no pending buffer and no ongoing speech, the session is done
            LOGD("No pending buffer, checking if speech finished")
            checkAndEmitSpeechFinished()
        }
    }

    private fun queueSentence(sentence: String) {
        sentenceQueue.add(sentence)
        if (!isSpeaking) {
            speakNextInQueue()
        }
    }

    private fun speakNextInQueue() {
        val nextSentence = sentenceQueue.poll() ?: return

        utteranceCounter++
        val utteranceId = "tts_utterance_$utteranceCounter"

        // Set isSpeaking before speak() to avoid race condition where
        // checkAndEmitSpeechFinished() is called before onStart callback
        isSpeaking = true
        _isSpeakingFlow.value = true
        tts?.speak(
            nextSentence,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    private data class SentenceExtraction(
        val completed: List<String>,
        val remaining: String
    )

    private fun extractCompleteSentences(text: String): SentenceExtraction {
        val completed = mutableListOf<String>()
        var remaining = text

        var match = sentenceEndRegex.find(remaining)
        while (match != null) {
            val endIndex = match.range.last + 1
            val sentence = remaining.substring(0, endIndex)
            completed.add(sentence)
            remaining = remaining.substring(endIndex)
            match = sentenceEndRegex.find(remaining)
        }

        return SentenceExtraction(completed, remaining)
    }

    fun stop() {
        sentenceQueue.clear()
        pendingBuffer = ""
        previousText = ""
        isSpeaking = false
        _isSpeakingFlow.value = false
        isSpeechSessionActive = false
        tts?.stop()
    }

    fun resetState() {
        stop()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
