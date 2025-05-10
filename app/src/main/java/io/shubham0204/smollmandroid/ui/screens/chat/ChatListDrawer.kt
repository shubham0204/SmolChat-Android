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

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog

@Composable
fun DrawerUI(
    viewModel: ChatScreenViewModel,
    onItemClick: (Chat) -> Unit,
    onManageTasksClick: () -> Unit,
    onCreateTaskClick: () -> Unit,
) {
    Surface {
        Column(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp)
                    .requiredWidth(300.dp)
                    .fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = {
                        val chatCount = viewModel.appDB.getChatsCount()
                        val newChat =
                            viewModel.appDB.addChat(chatName = "Untitled ${chatCount + 1}")
                        onItemClick(newChat)
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Text(
                        stringResource(R.string.chat_drawer_new_chat),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Button(
                    onClick = onCreateTaskClick,
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = "New Task")
                    Text(
                        stringResource(R.string.chat_drawer_new_task),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ChatsList(
                viewModel,
                onManageTasksClick,
                onItemClick,
            )
        }
        AppAlertDialog()
    }
}

@Composable
private fun ColumnScope.ChatsList(
    viewModel: ChatScreenViewModel,
    onManageTasksClick: () -> Unit,
    onItemClick: (Chat) -> Unit,
) {
    val chats by viewModel.getChats().collectAsState(emptyList())
    val currentChat by viewModel.currChatState.collectAsState(null)
    LazyColumn(modifier = Modifier.weight(1f)) {
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onManageTasksClick() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Quickreply,
                    contentDescription = "Manage Tasks",
                    tint = MaterialTheme.colorScheme.surfaceTint,
                )
                Text(
                    stringResource(R.string.chat_drawer_manage_tasks),
                    style = MaterialTheme.typography.labelLarge,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.chat_drawer_previous_chat),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(chats) { chat ->
            ChatListItem(
                chat,
                onItemClick,
                currentChat?.id == chat.id,
            )
        }
    }
}

@Composable
private fun LazyItemScope.ChatListItem(
    chat: Chat,
    onItemClick: (Chat) -> Unit,
    isCurrentlySelected: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onItemClick(chat) }
                .animateItem(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(chat.dateUsed.time).toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (isCurrentlySelected) {
                Box(
                    modifier =
                        Modifier
                            .padding(start = 4.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            .size(10.dp),
                ) { }
            }
        }
    }
}
