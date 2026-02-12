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
}
