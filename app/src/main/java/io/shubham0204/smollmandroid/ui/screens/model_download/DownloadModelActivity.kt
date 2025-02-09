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

package io.shubham0204.smollmandroid.ui.screens.model_download

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.llm.exampleModelsList
import io.shubham0204.smollmandroid.ui.components.AppProgressDialog
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.android.ext.android.inject

class DownloadModelActivity : ComponentActivity() {
    private var openChatScreen: Boolean = true
    private val viewModel: DownloadModelsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "download-model",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("view-model") {
                    ViewHFModelScreen(
                        viewModel,
                        onBackClicked = { navController.navigateUp() },
                    )
                }
                composable("hf-model-select") {
                    HFModelDownloadScreen(
                        viewModel,
                        onBackClicked = { navController.navigateUp() },
                        onModelClick = { modelId ->
                            viewModel.viewModelId = modelId
                            navController.navigate("view-model")
                        },
                    )
                }
                composable("download-model") {
                    DownloadModelScreen(
                        viewModel,
                        onHFModelSelectClick = { navController.navigate("hf-model-select") },
                    )
                }
            }
        }
        openChatScreen = intent.extras?.getBoolean("openChatScreen") ?: true
    }

    private fun openChatActivity() {
        if (openChatScreen) {
            Intent(this, ChatActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        } else {
            finish()
        }
    }

    @Composable
    private fun DownloadModelScreen(
        viewModel: DownloadModelsViewModel,
        onHFModelSelectClick: () -> Unit,
    ) {
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                activityResult.data?.let {
                    it.data?.let { uri ->
                        viewModel.copyModelFile(uri, onComplete = { openChatActivity() })
                    }
                }
            }
        SmolLMAndroidTheme {
            Surface {
                Column(
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxSize()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.safeContent)
                            .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.download_model_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.download_model_popular_models),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ModelsList(viewModel)
                    Spacer(modifier = Modifier.height(4.dp))
                    ModelURLInput(viewModel)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onHFModelSelectClick,
                        ) {
                            Text(stringResource(R.string.download_model_browse_hf))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled =
                                viewModel.selectedModelState.value != null ||
                                    viewModel.modelUrlState.value.isNotBlank(),
                            onClick = { viewModel.downloadModel() },
                        ) {
                            Text(stringResource(R.string.download_model_download))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.download_model_desc_download_location),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                stringResource(R.string.download_model_select_gguf),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            setType("application/octet-stream")
                                            putExtra(
                                                DocumentsContract.EXTRA_INITIAL_URI,
                                                Environment
                                                    .getExternalStoragePublicDirectory(
                                                        Environment.DIRECTORY_DOWNLOADS,
                                                    ).toUri(),
                                            )
                                        }
                                    launcher.launch(intent)
                                },
                            ) {
                                Text(stringResource(R.string.download_models_select_gguf_button))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                AppProgressDialog()
            }
        }
    }

    @Composable
    private fun ModelsList(viewModel: DownloadModelsViewModel) {
        var selectedModel by remember { viewModel.selectedModelState }
        Column(verticalArrangement = Arrangement.Center) {
            exampleModelsList.forEach { model ->
                Row(
                    Modifier
                        .clickable { selectedModel = model }
                        .fillMaxWidth()
                        .background(
                            if (model == selectedModel) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            RoundedCornerShape(
                                8.dp,
                            ),
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (model == selectedModel) {
                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        color =
                            if (model == selectedModel) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme
                                    .colorScheme.onBackground
                            },
                        text = model.name,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    @Composable
    private fun ModelURLInput(viewModel: DownloadModelsViewModel) {
        var modelUrl by remember { viewModel.modelUrlState }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = modelUrl,
            onValueChange = { modelUrl = it },
            placeholder = {
                Text(text = stringResource(R.string.download_model_gguf_url))
            },
            keyboardOptions =
                KeyboardOptions.Default.copy(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                ),
        )
    }
}
