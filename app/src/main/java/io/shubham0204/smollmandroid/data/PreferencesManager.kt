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

package io.shubham0204.smollmandroid.data

import android.content.Context
import org.koin.core.annotation.Single

@Single
class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("smolchat_prefs", Context.MODE_PRIVATE)

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("tts_enabled", false)
        set(value) = prefs.edit().putBoolean("tts_enabled", value).apply()

    var autoSubmitEnabled: Boolean
        get() = prefs.getBoolean("auto_submit_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_submit_enabled", value).apply()

    var autoSubmitDelayMs: Long
        get() = prefs.getLong("auto_submit_delay_ms", 2000L)
        set(value) = prefs.edit().putLong("auto_submit_delay_ms", value).apply()

    var selectedWhisperModel: String
        get() = prefs.getString("selected_whisper_model", DEFAULT_WHISPER_MODEL) ?: DEFAULT_WHISPER_MODEL
        set(value) = prefs.edit().putString("selected_whisper_model", value).apply()

    var sttLanguage: String
        get() = prefs.getString("stt_language", DEFAULT_STT_LANGUAGE) ?: DEFAULT_STT_LANGUAGE
        set(value) = prefs.edit().putString("stt_language", value).apply()

    companion object {
        const val DEFAULT_WHISPER_MODEL = "ggml-base.en.bin"
        const val DEFAULT_STT_LANGUAGE = "en"

        // Whisper supported languages with their display names
        val SUPPORTED_LANGUAGES = listOf(
            "en" to "English",
            "de" to "German",
            "fr" to "French",
            "es" to "Spanish",
            "it" to "Italian",
            "pt" to "Portuguese",
            "nl" to "Dutch",
            "pl" to "Polish",
            "ru" to "Russian",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "ar" to "Arabic",
            "hi" to "Hindi",
            "tr" to "Turkish",
            "uk" to "Ukrainian",
            "cs" to "Czech",
            "sv" to "Swedish",
            "auto" to "Auto-detect",
        )
    }
}
