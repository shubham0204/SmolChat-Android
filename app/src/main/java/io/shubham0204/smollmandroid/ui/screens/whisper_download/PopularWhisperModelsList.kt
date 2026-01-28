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

package io.shubham0204.smollmandroid.ui.screens.whisper_download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check

data class WhisperModel(
    val name: String,
    val url: String,
    val fileName: String,
    val sizeDescription: String,
)

@Preview
@Composable
fun PreviewPopularWhisperModelsList() {
    PopularWhisperModelsList(selectedModelIndex = 0, onModelSelected = {})
}

@Composable
fun PopularWhisperModelsList(selectedModelIndex: Int?, onModelSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.Center) {
        popularWhisperModelsList.forEachIndexed { idx, model ->
            Row(
                Modifier
                    .clickable { onModelSelected(idx) }
                    .fillMaxWidth()
                    .background(
                        if (idx == selectedModelIndex) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (idx == selectedModelIndex) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            color = MaterialTheme.colorScheme.onSurface,
                            text = model.name,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = model.sizeDescription,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

fun getPopularWhisperModel(index: Int?): WhisperModel? =
    if (index != null) popularWhisperModelsList[index] else null

/**
 * A list of Whisper models for speech-to-text functionality.
 * Models are from the ggerganov/whisper.cpp repository.
 * See: https://huggingface.co/ggerganov/whisper.cpp
 */
val popularWhisperModelsList = listOf(
    WhisperModel(
        name = "Whisper Tiny (English)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
        fileName = "ggml-tiny.en.bin",
        sizeDescription = "~75 MB - Fastest, less accurate",
    ),
    WhisperModel(
        name = "Whisper Base (English)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
        fileName = "ggml-base.en.bin",
        sizeDescription = "~142 MB - Good balance of speed/accuracy",
    ),
    WhisperModel(
        name = "Whisper Small (English)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en.bin",
        fileName = "ggml-small.en.bin",
        sizeDescription = "~466 MB - Better accuracy, slower",
    ),
    WhisperModel(
        name = "Whisper Tiny (Multilingual)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
        fileName = "ggml-tiny.bin",
        sizeDescription = "~75 MB - Fastest, supports multiple languages",
    ),
    WhisperModel(
        name = "Whisper Base (Multilingual)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        fileName = "ggml-base.bin",
        sizeDescription = "~142 MB - Good balance, multilingual",
    ),
    WhisperModel(
        name = "Whisper Small (Multilingual)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
        fileName = "ggml-small.bin",
        sizeDescription = "~466 MB - Better accuracy, multilingual",
    ),
    WhisperModel(
        name = "Whisper Large v3-turbo (Multilingual)",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin",
        fileName = "ggml-large-v3-turbo.bin",
        sizeDescription = "~466 MB - Better accuracy, multilingual",
    ),
)
