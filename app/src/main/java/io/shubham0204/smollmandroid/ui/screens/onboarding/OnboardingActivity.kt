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

package io.shubham0204.smollmandroid.ui.screens.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.shubham0204.smollmandroid.data.KEY_HAS_SEEN_ONBOARDING
import io.shubham0204.smollmandroid.data.SharedPrefStore
import io.shubham0204.smollmandroid.llm.ModelsRepository
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.screens.model_download.DownloadModelActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.android.ext.android.inject

class OnboardingActivity : ComponentActivity() {
    private val sharedPrefStore by inject<SharedPrefStore>()
    private val modelsRepository by inject<ModelsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmolLMAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        OnboardingScreen(
                            onFinishOnboarding = { completeOnboarding() },
                        )
                    }
                }
            }
        }
    }

    private fun completeOnboarding() {
        sharedPrefStore.put(KEY_HAS_SEEN_ONBOARDING, true)
        val nextActivity =
            if (modelsRepository.getAvailableModelsList().isEmpty()) {
                DownloadModelActivity::class.java
            } else {
                ChatActivity::class.java
            }
        Intent(this, nextActivity).apply {
            startActivity(this)
            finish()
        }
    }
}
