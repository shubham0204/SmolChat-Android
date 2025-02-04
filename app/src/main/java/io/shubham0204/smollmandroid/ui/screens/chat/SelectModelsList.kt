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

package io.shubham0204.smollmandroid.ui.screens.chat

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.screens.model_download.DownloadModelActivity
import java.io.File

enum class SortOrder {
    NAME,
    DATE_ADDED,
}

@Composable
fun SelectModelsList(
    onDismissRequest: () -> Unit,
    modelsList: List<LLMModel>,
    onModelListItemClick: (LLMModel) -> Unit,
    onModelDeleteClick: (LLMModel) -> Unit,
    showModelDeleteIcon: Boolean = true,
) {
    val context = LocalContext.current
    var sortOrder by remember { mutableStateOf(SortOrder.NAME) }
    Surface {
        Dialog(onDismissRequest = onDismissRequest) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                        .padding(16.dp),
            ) {
                Text(
                    text = "Choose Model",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a downloaded model from below to use as a 'default' model for this chat.",
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Animate switching between different types of content
                // See https://developer.android.com/develop/ui/compose/animation/quick-guide#switch-different
                AnimatedContent(
                    sortOrder,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(100),
                        ) togetherWith fadeOut(animationSpec = tween(100))
                    },
                    modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            sortOrder =
                                when (sortOrder) {
                                    SortOrder.NAME -> SortOrder.DATE_ADDED
                                    SortOrder.DATE_ADDED -> SortOrder.NAME
                                }
                        },
                    label = "change-sort-order-anim",
                ) { targetSortOrder: SortOrder ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .align(Alignment.End)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    sortOrder = if (sortOrder == SortOrder.NAME) SortOrder.DATE_ADDED else SortOrder.NAME
                                },
                    ) {
                        when (targetSortOrder) {
                            SortOrder.DATE_ADDED -> {
                                Icon(
                                    imageVector = Icons.Default.Title,
                                    contentDescription = "Sort by Model Name",
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sort by Name",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }

                            SortOrder.NAME -> {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Sort by Date Added",
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sort by Date Added",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (sortOrder == SortOrder.NAME) {
                        items(modelsList.sortedBy { it.name }) {
                            ModelListItem(
                                model = it,
                                onModelListItemClick,
                                onModelDeleteClick,
                                showModelDeleteIcon,
                            )
                        }
                    } else {
                        items(modelsList.reversed()) {
                            ModelListItem(
                                model = it,
                                onModelListItemClick,
                                onModelDeleteClick,
                                showModelDeleteIcon,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        Intent(context, DownloadModelActivity::class.java).also {
                            it.putExtra("openChatScreen", false)
                            context.startActivity(it)
                        }
                    },
                ) {
                    Text("Add model")
                }
            }
        }
    }
}

@Composable
private fun ModelListItem(
    model: LLMModel,
    onModelListItemClick: (LLMModel) -> Unit,
    onModelDeleteClick: (LLMModel) -> Unit,
    showModelDeleteIcon: Boolean,
) {
    Row(
        modifier =
            Modifier
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
            .clickable { onModelListItemClick(model) }
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = model.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Text(
                text = "%.1f GB".format(File(model.path).length() / (1e+9)),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (showModelDeleteIcon) {
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = {
                    createAlertDialog(
                        dialogTitle = "Delete Model",
                        dialogText = "Are you sure you want to delete the model '${model.name}'?",
                        dialogPositiveButtonText = "Delete",
                        dialogNegativeButtonText = "Cancel",
                        onPositiveButtonClick = { onModelDeleteClick(model) },
                        onNegativeButtonClick = {},
                    )
                },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Model",
                )
            }
        }
    }
}
