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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Download
import compose.icons.feathericons.File
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Share
import compose.icons.feathericons.ThumbsUp
import compose.icons.feathericons.User
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelTree
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import java.time.LocalDateTime
import java.time.ZoneId

@Preview
@Composable
private fun ViewHFModelScreenPreview() {
    ViewHFModelScreen(
        modelId = "bartowski/Meta-Llama-3.1-8B-Instruct-GGUF",
        modelInfo =
            HFModelInfo.ModelInfo(
                _id = "",
                id = "",
                modelId = "bartowski/Meta-Llama-3.1-8B-Instruct-GGUF",
                author = "",
                private = false,
                disabled = false,
                tags = listOf("gguf", "vision"),
                numDownloads = 1000,
                numLikes = 340,
                lastModified = LocalDateTime.now(),
                createdAt = LocalDateTime.now(),
            ),
        modelFileTree =
            listOf(HFModelTree.HFModelFile(type = "", oid = "", size = 1200, path = "file.gguf")),
        onDownloadModel = {},
        onBackClicked = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewHFModelScreen(
    modelId: String,
    modelInfo: HFModelInfo.ModelInfo,
    modelFileTree: List<HFModelTree.HFModelFile>,
    onDownloadModel: (String) -> Unit,
    onBackClicked: () -> Unit,
) {
    val context = LocalContext.current
    SmolLMAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        AppBarTitleText(stringResource(R.string.download_model_hf_details_title))
                    },
                    navigationIcon = {
                        IconButton(onClick = { onBackClicked() }) {
                            Icon(
                                FeatherIcons.ArrowLeft,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = "https://huggingface.co/$modelId".toUri()
                                    context.startActivity(this)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Globe,
                                contentDescription = "Open in Browser",
                            )
                        }
                        IconButton(
                            onClick = {
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://huggingface.co/$modelId")
                                    context.startActivity(this)
                                }
                            }
                        ) {
                            Icon(imageVector = FeatherIcons.Share, contentDescription = "Share")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(innerPadding)
            ) {
                ModelInfoCard(modelInfo)

                Text(
                    text = "GGUF Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                GGUFModelsList(
                    modelFileTree,
                    onModelClick = { modelFile ->
                        createAlertDialog(
                            dialogTitle = "Download Model",
                            dialogText =
                                "The model will start downloading and will be stored in the Downloads " +
                                        "folder. Select the model file from the file explorer to load it in the app.",
                            dialogPositiveButtonText = "Download",
                            onPositiveButtonClick = {
                                onDownloadModel(
                                    "https://huggingface.co/${modelInfo.modelId}/resolve/main/${modelFile.path}"
                                )
                                onBackClicked()
                            },
                            dialogNegativeButtonText = "Cancel",
                            onNegativeButtonClick = {},
                        )
                    },
                )
            }
            AppAlertDialog()
        }
    }
}

@Composable
private fun GGUFModelsList(
    modelFiles: List<HFModelTree.HFModelFile>,
    onModelClick: (HFModelTree.HFModelFile) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onModelFileClick(modelFile) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    FeatherIcons.File,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelFile.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text =
                        if (fileSizeGB < 1) {
                            "${(fileSizeGB * 1000).toInt()} MB"
                        } else {
                            "%.2f GB".format(fileSizeGB)
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                FeatherIcons.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelInfoCard(modelInfo: HFModelInfo.ModelInfo) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val parts = modelInfo.modelId.split("/")
            val modelAuthor = if (parts.size > 1) parts[0] else "Hugging Face"
            val modelName = if (parts.size > 1) parts[1] else parts[0]

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    FeatherIcons.User,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = modelAuthor,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = modelName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModelInfoIconBubble(
                    icon = FeatherIcons.Download,
                    contentDescription = "Number of downloads",
                    text = modelInfo.numDownloads.toString(),
                )
                ModelInfoIconBubble(
                    icon = FeatherIcons.ThumbsUp,
                    contentDescription = "Number of likes",
                    text = modelInfo.numLikes.toString(),
                )
                ModelInfoIconBubble(
                    icon = FeatherIcons.Clock,
                    contentDescription = "Last updated",
                    text =
                        DateUtils.getRelativeTimeSpanString(
                                modelInfo.lastModified
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                        )
                            .toString(),
                )
            }

            if (modelInfo.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    modelInfo.tags.filter { !listOf("GGUF", "conversational").contains(it) }.take(8)
                        .forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun ModelInfoIconBubble(icon: ImageVector, contentDescription: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
