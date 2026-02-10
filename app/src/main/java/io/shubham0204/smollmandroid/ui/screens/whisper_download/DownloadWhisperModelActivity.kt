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

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.stt.SpeechToTextManager
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.android.ext.android.inject

class DownloadWhisperModelActivity : ComponentActivity() {

    private val sttManager: SpeechToTextManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val downloadedModels = remember { sttManager.getAvailableModels() }
            val selectedModel = remember { sttManager.getSelectedModelName() }

            Box(modifier = Modifier.safeDrawingPadding()) {
                DownloadWhisperModelScreen(
                    downloadedModels = downloadedModels,
                    selectedModel = selectedModel,
                    onBackClick = { finish() },
                    onDownloadModel = { model -> downloadWhisperModel(model) },
                    onSelectModel = { modelName ->
                        sttManager.setSelectedModel(modelName)
                        Toast.makeText(
                            this@DownloadWhisperModelActivity,
                            getString(R.string.whisper_model_selected, getWhisperModelDisplayName(modelName)),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun downloadWhisperModel(model: WhisperModel) {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(model.url)).apply {
            setTitle(model.name)
            setDescription(getString(R.string.whisper_download_notification_desc))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(
                this@DownloadWhisperModelActivity,
                Environment.DIRECTORY_DOWNLOADS,
                model.fileName
            )
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
        }
        downloadManager.enqueue(request)
        Toast.makeText(
            this,
            getString(R.string.whisper_download_started, model.name),
            Toast.LENGTH_LONG
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadWhisperModelScreen(
    downloadedModels: List<String>,
    selectedModel: String,
    onBackClick: () -> Unit,
    onDownloadModel: (WhisperModel) -> Unit,
    onSelectModel: (String) -> Unit,
) {
    var selectedModelIndex by remember { mutableStateOf<Int?>(1) } // Default to base.en model
    var currentSelectedModel by remember { mutableStateOf(selectedModel) }

    SmolLMAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { AppBarTitleText(stringResource(R.string.whisper_download_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.button_text_back)
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Downloaded Models Section
                    if (downloadedModels.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.whisper_downloaded_models),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.whisper_downloaded_models_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DownloadedModelsList(
                            models = downloadedModels,
                            selectedModel = currentSelectedModel,
                            onModelSelected = { modelName ->
                                currentSelectedModel = modelName
                                onSelectModel(modelName)
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Download New Models Section
                    Text(
                        text = stringResource(R.string.whisper_download_new_model),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.whisper_download_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.whisper_select_model),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PopularWhisperModelsList(
                        selectedModelIndex = selectedModelIndex,
                        onModelSelected = { selectedModelIndex = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            selectedModelIndex?.let { index ->
                                getPopularWhisperModel(index)?.let { model ->
                                    onDownloadModel(model)
                                }
                            }
                        },
                        enabled = selectedModelIndex != null,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.whisper_download_button))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.whisper_download_location_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedModelsList(
    models: List<String>,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.Center) {
        models.forEach { modelFileName ->
            val isSelected = modelFileName == selectedModel
            val displayName = getWhisperModelDisplayName(modelFileName)
            Row(
                Modifier
                    .clickable { onModelSelected(modelFileName) }
                    .fillMaxWidth()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
