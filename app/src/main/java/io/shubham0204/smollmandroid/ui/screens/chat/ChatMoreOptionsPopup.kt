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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.theme.AppFontFamily

@Composable
fun ChatMoreOptionsPopup(
    viewModel: ChatScreenViewModel,
    onEditChatSettingsClick: () -> Unit,
) {
    val expanded by viewModel.showMoreOptionsPopupState.collectAsStateWithLifecycle()
    val context = viewModel.context
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { viewModel.hideMoreOptionsPopup() },
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Edit Chat Name") },
            text = {
                Text(
                    stringResource(R.string.edit_chat_settings),
                    fontFamily = AppFontFamily,
                )
            },
            onClick = {
                onEditChatSettingsClick()
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Assistant, contentDescription = stringResource(R.string.change_model)) },
            text = { Text(stringResource(R.string.change_model), fontFamily = AppFontFamily) },
            onClick = {
                viewModel.showSelectModelListDialog()
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ShortText,
                    contentDescription = "Context Usage",
                )
            },
            text = {
                Text(
                    stringResource(R.string.context_length_usage),
                    fontFamily = AppFontFamily,
                )
            },
            onClick = {
                viewModel.showContextLengthUsageDialog()
                viewModel.hideMoreOptionsPopup()
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = context.getString(R.string.delete_chat),
                )
            },
            text = {
                Text(
                    context.getString(R.string.delete_chat),
                    fontFamily = AppFontFamily,
                )
            },
            onClick = {
                viewModel.currChatState.value?.let { chat ->
                    createAlertDialog(
                        dialogTitle = context.getString(R.string.delete_chat),
                        dialogText = context.getString(R.string.delete_chat_dialog_text, chat.name),
                        dialogPositiveButtonText = context.getString(R.string.delete),
                        dialogNegativeButtonText = context.getString(R.string.cancel),
                        onPositiveButtonClick = {
                            viewModel.deleteChat(chat)
                            Toast
                                .makeText(
                                    viewModel.context,
                                    context.getString(R.string.chat_deleted, chat.name),
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
