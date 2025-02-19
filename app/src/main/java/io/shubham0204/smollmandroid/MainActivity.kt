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

package io.shubham0204.smollmandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import io.shubham0204.smollmandroid.data.models.ModelsRepository
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.screens.model_download.DownloadModelActivity
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val modelsRepository by inject<ModelsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Redirect user to the DownloadModelActivity if no models are available
        // as the app requires at least one model to function
        if (modelsRepository.getAvailableModelsList().isEmpty()) {
            Intent(this, DownloadModelActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        } else {
            Intent(this, ChatActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        }
    }
}
