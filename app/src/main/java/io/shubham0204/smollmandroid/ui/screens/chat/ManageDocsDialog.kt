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

package io.shubham0204.smollmandroid.ui.screens.chat

import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.docs.Document
import io.shubham0204.smollmandroid.llm.PDFProcessor
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.screens.model_download.DownloadModelActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ManageDocsDialog(
    viewModel: ChatScreenViewModel,
    onDocListItemClick: (Document) -> Unit,
    onDocDeleteClick: (Document) -> Unit
) {
    val context = LocalContext.current
    Surface {
        Dialog(onDismissRequest = { viewModel.hideManageDocsDialog() }) {
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                    activityResult.data?.let {
                        it.data?.let { uri ->
                            val pdfProcessor = PDFProcessor()
                            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                                CoroutineScope(Dispatchers.Default).launch {
                                    val text = pdfProcessor.readPDF(inputStream)
                                    inputStream.close()
                                }
                            }
                        }
                    }
                }
            Column {
                DocsList(
                    viewModel = viewModel,
                    onDocListItemClick = onDocListItemClick,
                    onDocDeleteClick = onDocDeleteClick,
                    onAddDocClick = {
                        val intent =
                            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                setType("application/pdf")
                                putExtra(
                                    DocumentsContract.EXTRA_INITIAL_URI,
                                    Environment
                                        .getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DOCUMENTS,
                                        ).toUri(),
                                )
                            }
                        launcher.launch(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun DocsList(
    viewModel: ChatScreenViewModel,
    onDocListItemClick: (Document) -> Unit,
    onDocDeleteClick: (Document) -> Unit,
    onAddDocClick: () -> Unit
) {
    val currChat by viewModel.currChatState.collectAsStateWithLifecycle()
    currChat?.let { chat ->
        var sortOrder by remember { mutableStateOf(SortOrder.NAME) }
        val documentsList by viewModel.getDocuments(chat.id).collectAsState(initial = emptyList())
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_model_list_screen_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.chat_model_list_desc),
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
                            sortOrder =
                                if (sortOrder == SortOrder.NAME) SortOrder.DATE_ADDED else SortOrder.NAME
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
                                text = stringResource(R.string.chat_model_list_sort_name),
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
                                text = stringResource(R.string.chat_model_list_sort_date),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                if (sortOrder == SortOrder.NAME) {
                    items(documentsList.sortedBy { it.name }) {
                        DocListItem(
                            document = it,
                            onDocListItemClick,
                            onDocDeleteClick,
                        )
                    }
                } else {
                    items(documentsList.reversed()) {
                        DocListItem(
                            document = it,
                            onDocListItemClick,
                            onDocDeleteClick,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddDocClick) {
                Text("Add Document")
            }
        }
    }
}

@Composable
private fun DocListItem(
    document: Document,
    onDocListItemClick: (Document) -> Unit,
    onDocDeleteClick: (Document) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier =
        Modifier
            .padding(4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onDocListItemClick(document) }
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = document.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Text(
                text = "%.1f GB".format(document.fileSize / (1e+9)),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        IconButton(
            modifier = Modifier.size(24.dp),
            onClick = {
                createAlertDialog(
                    dialogTitle = context.getString(R.string.dialog_title_delete_chat),
                    dialogText = context.getString(R.string.dialog_text_delete_chat),
                    dialogPositiveButtonText = context.getString(R.string.dialog_pos_delete),
                    dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                    onPositiveButtonClick = { onDocDeleteClick(document) },
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