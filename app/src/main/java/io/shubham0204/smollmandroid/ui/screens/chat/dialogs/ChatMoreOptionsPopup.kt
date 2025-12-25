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

package io.shubham0204.smollmandroid.ui.screens.chat.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.Delete
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Layout
import compose.icons.feathericons.Package
import compose.icons.feathericons.Settings
import compose.icons.feathericons.XCircle
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.ui.preview.dummyChats
import io.shubham0204.smollmandroid.ui.screens.chat.ChatScreenUIEvent

@Preview
@Composable
private fun PreviewChatMoreOptionsPopup() {
    ChatMoreOptionsPopup(
        chat = dummyChats[0],
        isExpanded = true,
        showRAMUsageLabel = true,
        onEditChatSettingsClick = {},
        onEvent = {}
    )
}

@Composable
fun ChatMoreOptionsPopup(
    chat: Chat,
    isExpanded: Boolean,
    showRAMUsageLabel: Boolean,
    onEditChatSettingsClick: () -> Unit,
    onEvent: (ChatScreenUIEvent) -> Unit,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = {
            onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.Settings,
                    contentDescription = "Edit Chat Settings",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.chat_options_edit_settings),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEditChatSettingsClick()
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.Folder,
                    contentDescription = "Change Folder",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.chat_options_change_folder),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleChangeFolderDialog(visible = true))
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.Package,
                    contentDescription = "Change Model",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.chat_options_change_model),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleSelectModelListDialog(visible = true))
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(4.dp))
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.Delete,
                    contentDescription = "Delete Chat",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.dialog_title_delete_chat),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEvent(ChatScreenUIEvent.ChatEvents.OnDeleteChat(chat))
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.XCircle,
                    contentDescription = "Clear Chat Messages",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.chat_options_clear_messages),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEvent(ChatScreenUIEvent.ChatEvents.OnDeleteChatMessages(chat))
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(4.dp))
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.Layout,
                    contentDescription = "Context Usage",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.chat_options_ctx_length_usage),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEvent(ChatScreenUIEvent.DialogEvents.ShowContextLengthUsageDialog(chat))
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    FeatherIcons.Cpu,
                    contentDescription = "RAM Usage",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            text = {
                Text(
                    if (showRAMUsageLabel) "Hide RAM usage" else "Show RAM usage",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            onClick = {
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleRAMUsageLabel)
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(visible = false))
            },
        )
    }
}
