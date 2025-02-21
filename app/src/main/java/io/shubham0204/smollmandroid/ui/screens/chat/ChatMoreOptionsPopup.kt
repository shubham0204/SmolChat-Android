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

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.createAlertDialog

@Composable
fun ChatMoreOptionsPopup(
    viewModel: ChatScreenViewModel,
    onEditChatSettingsClick: () -> Unit,
) {
    val expanded by viewModel.showMoreOptionsPopupState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { viewModel.hideMoreOptionsPopup() },
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Edit Chat Name") },
            text = { Text(stringResource(R.string.chat_options_edit_settings)) },
            onClick = {
                onEditChatSettingsClick()
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Assistant, contentDescription = "Change Model") },
            text = { Text(stringResource(R.string.chat_options_change_model)) },
            onClick = {
                viewModel.showSelectModelListDialog()
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShortText, contentDescription = "Context Usage") },
            text = { Text(stringResource(R.string.chat_options_ctx_length_usage)) },
            onClick = {
                viewModel.showContextLengthUsageDialog()
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete Chat") },
            text = { Text(stringResource(R.string.dialog_title_delete_chat)) },
            onClick = {
                viewModel.currChatState.value?.let { chat ->
                    createAlertDialog(
                        dialogTitle = context.getString(R.string.dialog_title_delete_chat),
                        dialogText = context.getString(R.string.dialog_text_delete_chat, chat.name),
                        dialogPositiveButtonText = context.getString(R.string.dialog_pos_delete),
                        dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                        onPositiveButtonClick = {
                            viewModel.deleteChat(chat)
                            Toast
                                .makeText(
                                    viewModel.context,
                                    "Chat '${chat.name}' deleted",
                                    Toast.LENGTH_LONG,
                                ).show()
                        },
                        onNegativeButtonClick = {},
                    )
                }
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Clear Chat Messages") },
            text = { Text(stringResource(R.string.chat_options_clear_messages)) },
            onClick = {
                viewModel.currChatState.value?.let { chat ->
                    createAlertDialog(
                        dialogTitle = context.getString(R.string.chat_options_clear_messages),
                        dialogText = context.getString(R.string.chat_options_clear_messages_text),
                        dialogPositiveButtonText = context.getString(R.string.dialog_pos_clear),
                        dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                        onPositiveButtonClick = {
                            viewModel.deleteChatMessages(chat)
                            Toast
                                .makeText(
                                    viewModel.context,
                                    "Chat '${chat.name}' cleared",
                                    Toast.LENGTH_LONG,
                                ).show()
                        },
                        onNegativeButtonClick = {},
                    )
                }
                viewModel.hideMoreOptionsPopup()
            },
        )
    }
}
