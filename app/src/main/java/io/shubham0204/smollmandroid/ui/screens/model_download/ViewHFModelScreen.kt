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

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.smollmandroid.ui.components.SmallLabelText
import io.shubham0204.smollmandroid.ui.theme.AppAccentColor
import io.shubham0204.smollmandroid.ui.theme.AppFontFamily
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import java.time.ZoneId

@Composable
fun ViewHFModelScreen(viewModel: DownloadModelsViewModel) {
    viewModel.viewModelId?.let { modelId ->
        SmolLMAndroidTheme {
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                LaunchedEffect(0) {
                    viewModel.fetchModelInfoAndTree(modelId)
                }
                val modelInfoAndTree by viewModel.modelInfoAndTree.collectAsStateWithLifecycle(LocalLifecycleOwner.current)
                modelInfoAndTree?.let { modelInfoAndTree ->
                    val modelInfo = modelInfoAndTree.first
                    val modelFiles = modelInfoAndTree.second
                    ModelInfoCard(modelInfo)
                }
            }
        }
    }
}

@Composable
private fun ModelInfoCard(modelInfo: HFModelInfo.ModelInfo) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = modelInfo.modelId,
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
