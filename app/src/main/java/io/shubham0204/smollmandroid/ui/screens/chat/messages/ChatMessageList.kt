package io.shubham0204.smollmandroid.ui.screens.chat.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.Spanned
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.User
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.ChatMessage
import io.shubham0204.smollmandroid.ui.screens.chat.ChatScreenUIEvent
import io.shubham0204.smollmandroid.ui.screens.chat.dialogs.showCodeSnippetsListDialog
import io.shubham0204.smollmandroid.ui.screens.chat.stripThinkingForClipboard
import kotlinx.collections.immutable.ImmutableList


@Composable
fun ColumnScope.MessagesList(
    messages: ImmutableList<ChatMessage>,
    isGeneratingResponse: Boolean,
    renderedPartialResponse: Spanned?,
    chatId: Long,
    responseGenerationsSpeed: Float? = null,
    responseGenerationTimeSecs: Int? = null,
    onEvent: (ChatScreenUIEvent) -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val lastUserMessageIndex = messages.indexOfLast { it.isUserMessage }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }
    LazyColumn(
        state = listState, modifier = Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        itemsIndexed(messages) { i, chatMessage ->
            MessageListItem(
                chatMessage,
                responseGenerationSpeed =
                    if (i == messages.size - 1) responseGenerationsSpeed else null,
                responseGenerationTimeSecs =
                    if (i == messages.size - 1) responseGenerationTimeSecs else null,
                chatMessage.isUserMessage,
                onCopyClicked = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val plain =
                        if (chatMessage.isUserMessage) {
                            chatMessage.message
                        } else {
                            chatMessage.message.stripThinkingForClipboard()
                        }
                    val clip = ClipData.newPlainText("Copied message", plain)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        context,
                        context.getString(R.string.chat_message_copied),
                        Toast.LENGTH_SHORT,
                    )
                        .show()
                },
                onShareClicked = {
                    val plain =
                        if (chatMessage.isUserMessage) {
                            chatMessage.message
                        } else {
                            chatMessage.message.stripThinkingForClipboard()
                        }
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, plain)
                        }
                    )
                },
                onMessageEdited = { newMessage ->
                    onEvent(
                        ChatScreenUIEvent.ChatEvents.OnMessageEdited(
                            chatId,
                            chatMessage,
                            messages.last(),
                            newMessage,
                        )
                    )
                },
                // allow editing the message only if it is the last message in the list
                allowEditing = (i == lastUserMessageIndex),
                onCodeSnippetCopyClicked = {
                    showCodeSnippetsListDialog(it)
                }
            )
        }
        if (isGeneratingResponse) {
            item {
                if (renderedPartialResponse != null) {
                    MessageListItem(
                        ChatMessage(renderedMessage = renderedPartialResponse),
                        responseGenerationSpeed = null,
                        responseGenerationTimeSecs = null,
                        false,
                        {},
                        {},
                        onMessageEdited = {
                            // Not applicable as allowEditing is set to False
                        },
                        allowEditing = false,
                        onCodeSnippetCopyClicked = {
                            // Not applicable as partial messages (may) not have code snippets
                        }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .animateItem(),
                    ) {
                        Icon(
                            modifier = Modifier.padding(8.dp),
                            imageVector = FeatherIcons.User,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.chat_thinking),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
