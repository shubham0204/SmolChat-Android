/*
 * Copyright (C) 2025 Shubham Panchal
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
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelTree
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.LargeLabelText
import io.shubham0204.smollmandroid.ui.components.SmallLabelText
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.theme.AppAccentColor
import io.shubham0204.smollmandroid.ui.theme.AppFontFamily
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewHFModelScreen(
    viewModel: DownloadModelsViewModel,
    onBackClicked: () -> Unit,
) {
    val context = LocalContext.current
    viewModel.viewModelId?.let { modelId ->
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { AppBarTitleText("Model Details") },
                        navigationIcon = {
                            IconButton(onClick = { onBackClicked() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate Back",
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = "https://huggingface.co/$modelId".toUri()
                                    context.startActivity(this)
                                }
                            }) {
                                Icon(imageVector = Icons.Default.ArrowOutward, contentDescription = "Open in Browser")
                            }
                            IconButton(onClick = {
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://huggingface.co/$modelId")
                                    context.startActivity(this)
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                ) {
                    LaunchedEffect(0) {
                        viewModel.fetchModelInfoAndTree(modelId)
                    }
                    val modelInfoAndTree by viewModel.modelInfoAndTree.collectAsStateWithLifecycle(LocalLifecycleOwner.current)
                    modelInfoAndTree?.let { modelInfoAndTree ->
                        val modelInfo = modelInfoAndTree.first
                        val modelFiles = modelInfoAndTree.second
                        ModelInfoCard(modelInfo)
                        Row(modifier = Modifier.padding(8.dp)) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = "Files")
                            Spacer(modifier = Modifier.width(4.dp))
                            LargeLabelText(text = "Files")
                        }
                        GGUFModelsList(modelFiles, onModelClick = { modelFile ->
                            createAlertDialog(
                                dialogTitle = "Download Model",
                                dialogText =
                                    "The model will start downloading and will be stored in the Downloads " +
                                        "folder. Select the model file from the file explorer to load it in the app.",
                                dialogPositiveButtonText = "Download",
                                onPositiveButtonClick = {
                                    val downloadUrl =
                                        "https://huggingface.co/${modelInfo.modelId}/resolve/main/${modelFile.path}"
                                    viewModel.modelUrlState.value = downloadUrl
                                    viewModel.downloadModel()
                                    onBackClicked()
                                },
                                dialogNegativeButtonText = "Cancel",
                                onNegativeButtonClick = {},
                            )
                        })
                    }
                }
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun GGUFModelsList(
    modelFiles: List<HFModelTree.HFModelFile>,
    onModelClick: (HFModelTree.HFModelFile) -> Unit,
) {
    LazyColumn {
        items(modelFiles) { modelFile ->
            GGUFModelListItem(modelFile, onModelClick)
        }
    }
}

@Composable
private fun GGUFModelListItem(
    modelFile: HFModelTree.HFModelFile,
    onModelFileClick: (HFModelTree.HFModelFile) -> Unit,
) {
    val fileSizeGB = modelFile.size / 1e+9
    Column(
        modifier =
            Modifier
                .clickable { onModelFileClick(modelFile) }
                .padding(8.dp)
        .fillMaxWidth()
        ) {
        Text(text = modelFile.path)
        if (fileSizeGB < 1) {
            Text(text = "${(fileSizeGB * 1000).toInt()} MB")
        } else {
            Text(text = "${fileSizeGB.toInt()} GB")
        }
    }
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ModelInfoCard(modelInfo: HFModelInfo.ModelInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppAccentColor),
        modifier = Modifier.padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val modelAuthor = modelInfo.modelId.split("/")[0]
            val modelName = modelInfo.modelId.split("/")[1]
            Text(
                text = modelAuthor,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = AppFontFamily,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = AppFontFamily,
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                ModelInfoIconBubble(
                    icon = Icons.Default.Download,
                    contentDescription = "Number of downloads",
                    text = modelInfo.numDownloads.toString(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                ModelInfoIconBubble(
                    icon = Icons.Default.ThumbUp,
                    contentDescription = "Number of likes",
                    text = modelInfo.numLikes.toString(),
                )
                ModelInfoIconBubble(
                    icon = Icons.Default.AccessTime,
                    contentDescription = "Last updated",
                    text =
                        DateUtils
                            .getRelativeTimeSpanString(
                                modelInfo.lastModified
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli(),
                            ).toString(),
                )
            }
        }
    }
}

@Composable
private fun ModelInfoIconBubble(
    icon: ImageVector,
    contentDescription: String,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .padding(4.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .padding(4.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppAccentColor,
        )
        Spacer(modifier = Modifier.width(2.dp))
        SmallLabelText(text = text)
    }
}
