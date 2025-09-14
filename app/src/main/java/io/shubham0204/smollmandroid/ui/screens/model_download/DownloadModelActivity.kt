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
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Check
import compose.icons.feathericons.Download
import compose.icons.feathericons.Globe
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.llm.exampleModelsList
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.AppProgressDialog
import io.shubham0204.smollmandroid.ui.components.AppSpacer4W
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.android.ext.android.inject

class DownloadModelActivity : ComponentActivity() {
    private var openChatScreen: Boolean = true
    private val viewModel: DownloadModelsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            Box(modifier = Modifier.safeDrawingPadding()) {
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
                        AddNewModelScreen(
                            onHFModelSelectClick = { navController.navigate("hf-model-select") },
                            onBackClick = { finish() },
                        )
                    }
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

    private enum class AddNewModelStep {
        ImportModel,
        DownloadModel,
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddNewModelScreen(
        onHFModelSelectClick: () -> Unit,
        onBackClick: () -> Unit,
    ) {
        var addNewModelStep by remember { mutableStateOf(AddNewModelStep.DownloadModel) }
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { AppBarTitleText(stringResource(R.string.add_new_model_title)) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    FeatherIcons.ArrowLeft,
                                    contentDescription = "Navigate Back",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Surface(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                ) {
                    when (addNewModelStep) {
                        AddNewModelStep.ImportModel -> {
                            SelectModelScreen(
                                onPrevSectionClick = {
                                    addNewModelStep = AddNewModelStep.DownloadModel
                                },
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                            )
                        }

                        AddNewModelStep.DownloadModel -> {
                            DownloadModelScreen(
                                onHFModelSelectClick = onHFModelSelectClick,
                                onNextSectionClick = {
                                    addNewModelStep = AddNewModelStep.ImportModel
                                },
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                            )
                        }
                    }
                }
                AppProgressDialog()
                AppAlertDialog()
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
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            RoundedCornerShape(
                                8.dp,
                            ),
                        ).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (model == selectedModel) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        color =
                            if (model == selectedModel) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme
                                    .colorScheme.onSurface
                            },
                        text = model.name,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    @Composable
    private fun DownloadModelScreen(
        onHFModelSelectClick: () -> Unit,
        onNextSectionClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier = modifier) {
            Text(
                text = stringResource(R.string.download_model_step_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.download_model_step_des),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ModelsList(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                enabled =
                    viewModel.selectedModelState.value != null ||
                        viewModel.modelUrlState.value.isNotBlank(),
                onClick = { viewModel.downloadModel() },
                shape = RoundedCornerShape(4.dp),
            ) {
                Icon(FeatherIcons.Download, contentDescription = "Download Selected Model")
                AppSpacer4W()
                Text(stringResource(R.string.download_model_download))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "OR",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.download_model_step_hf_browse),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onHFModelSelectClick,
                shape = RoundedCornerShape(4.dp),
            ) {
                Icon(FeatherIcons.Globe, contentDescription = "Download Selected Model")
                AppSpacer4W()
                Text(stringResource(R.string.download_model_browse_hf))
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.Bottom) {
                Text(
                    text = stringResource(R.string.download_model_next_step_des),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = onNextSectionClick,
                ) {
                    Icon(FeatherIcons.ArrowRight, contentDescription = null)
                    Text(stringResource(R.string.button_text_next))
                }
            }
        }
    }

    @Composable
    private fun SelectModelScreen(
        onPrevSectionClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                activityResult.data?.let {
                    it.data?.let { uri ->
                        if (checkGGUFFile(uri)) {
                            viewModel.copyModelFile(uri, onComplete = { openChatActivity() })
                        } else {
                            createAlertDialog(
                                dialogTitle = getString(R.string.dialog_invalid_file_title),
                                dialogText = getString(R.string.dialog_invalid_file_text),
                                dialogPositiveButtonText = "OK",
                                onPositiveButtonClick = {},
                                dialogNegativeButtonText = null,
                                onNegativeButtonClick = null,
                            )
                        }
                    }
                }
            }
        Column(modifier = modifier) {
            Text(
                text = stringResource(R.string.import_model_step_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.import_model_step_des),
                style = MaterialTheme.typography.labelSmall,
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
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(stringResource(R.string.download_models_select_gguf_button))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                OutlinedButton(
                    onClick = onPrevSectionClick,
                ) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = null)
                    Text(stringResource(R.string.button_text_back))
                }
            }
        }
    }

    // check if the first four bytes of the file
    // represent the GGUF magic number
    // see:https://github.com/ggml-org/ggml/blob/master/docs/gguf.md#file-structure
    private fun checkGGUFFile(uri: Uri): Boolean {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val ggufMagicNumberBytes = ByteArray(4)
            inputStream.read(ggufMagicNumberBytes)
            return ggufMagicNumberBytes.contentEquals(byteArrayOf(71, 71, 85, 70))
        }
        return false
    }
}
