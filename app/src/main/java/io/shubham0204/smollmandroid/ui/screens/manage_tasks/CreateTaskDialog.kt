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

package io.shubham0204.smollmandroid.ui.screens.manage_tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.components.SelectModelsList
import io.shubham0204.smollmandroid.ui.preview.dummyLLMModels
import kotlinx.collections.immutable.toImmutableList

@Composable
@Preview
private fun PreviewCreateTaskDialog() {
    CreateTaskDialog(onDismiss = {}, onAddTask = { _, _, _ -> }, dummyLLMModels)
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, String, Long) -> Unit,
    availableModelsList: List<LLMModel>,
) {
    var taskName by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf<LLMModel?>(null) }
    var isModelListDialogVisible by remember { mutableStateOf(false) }
    val (focusRequestor) = FocusRequester.createRefs()
    val keyboardController = LocalSoftwareKeyboardController.current
    Surface {
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier =
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.task_create_task_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text(stringResource(R.string.task_create_task_name)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions = KeyboardActions(onNext = { focusRequestor.requestFocus() }),
                )

                TextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .focusRequester(focusRequestor),
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(stringResource(R.string.task_create_task_sys_prompt)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                )

                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, Color.DarkGray)
                            .clickable { isModelListDialogVisible = true }
                            .padding(8.dp),
                    text =
                        if (selectedModel == null)
                            stringResource(R.string.task_create_task_select_model)
                        else selectedModel!!.name,
                )

                if (isModelListDialogVisible) {
                    SelectModelsList(
                        onDismissRequest = { isModelListDialogVisible = false },
                        modelsList = availableModelsList.toImmutableList(),
                        onModelListItemClick = { model ->
                            isModelListDialogVisible = false
                            selectedModel = model
                        },
                        onModelDeleteClick = { // Not applicable, as showModelDeleteIcon is set to
                            // false
                        },
                        showModelDeleteIcon = false,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    enabled =
                        taskName.isNotBlank() && systemPrompt.isNotBlank() && selectedModel != null,
                    onClick = {
                        onAddTask(taskName, systemPrompt, selectedModel?.id ?: -1L)
                        onDismiss()
                    },
                ) {
                    Icon(FeatherIcons.Plus, contentDescription = "Add")
                    Text(stringResource(R.string.task_create_task_add))
                }
            }
        }
    }
}
