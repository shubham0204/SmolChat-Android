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

import CustomNavTypes
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import compose.icons.FeatherIcons
import compose.icons.feathericons.Menu
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Send
import compose.icons.feathericons.StopCircle
import compose.icons.feathericons.User
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.ChatMessage
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.MediumLabelText
import io.shubham0204.smollmandroid.ui.components.SelectModelsList
import io.shubham0204.smollmandroid.ui.components.TasksList
import io.shubham0204.smollmandroid.ui.components.TextFieldDialog
import io.shubham0204.smollmandroid.ui.preview.dummyChats
import io.shubham0204.smollmandroid.ui.preview.dummyFolders
import io.shubham0204.smollmandroid.ui.preview.dummyLLMModels
import io.shubham0204.smollmandroid.ui.preview.dummyTasksList
import io.shubham0204.smollmandroid.ui.screens.chat.ChatScreenViewModel.ModelLoadingState
import io.shubham0204.smollmandroid.ui.screens.chat.dialogs.ChangeFolderDialogUI
import io.shubham0204.smollmandroid.ui.screens.chat.dialogs.ChatMessageOptionsDialog
import io.shubham0204.smollmandroid.ui.screens.chat.dialogs.ChatMoreOptionsPopup
import io.shubham0204.smollmandroid.ui.screens.chat.dialogs.FolderOptionsDialog
import io.shubham0204.smollmandroid.ui.screens.chat.dialogs.createChatMessageOptionsDialog
import io.shubham0204.smollmandroid.ui.screens.manage_tasks.ManageTasksActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.reflect.typeOf

private const val LOGTAG = "[ChatActivity-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@Serializable
private object ChatRoute

@Serializable
private object BenchmarkModelRoute

@Serializable
private data class EditChatSettingsRoute(val chat: Chat, val modelContextSize: Int)

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatScreenViewModel by viewModel()
    private var modelUnloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        /**
         * Check if the activity was launched by an intent to share text with the app If yes, then,
         * extract the text and set its value to [viewModel.questionTextDefaultVal]
         */
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                val chatCount = viewModel.appDB.getChatsCount()
                val newChat = viewModel.appDB.addChat(chatName = "Untitled ${chatCount + 1}")
                viewModel.onEvent(ChatScreenUIEvent.ChatEvents.SwitchChat(newChat))
                viewModel.questionTextDefaultVal = text
            }
        }

        /**
         * Check if the activity was launched by an intent created by a dynamic shortcut. If yes,
         * get the corresponding task (task ID is stored within the intent) and create a new chat
         * instance with the task's parameters
         */
        if (intent?.action == Intent.ACTION_VIEW && intent.getLongExtra("task_id", 0L) != 0L) {
            val taskId = intent.getLongExtra("task_id", 0L)
            viewModel.appDB.getTask(taskId).let { task ->
                viewModel.onEvent(ChatScreenUIEvent.ChatEvents.OnTaskSelected(task))
            }
        }

        setContent {
            val navController = rememberNavController()
            Box(modifier = Modifier.safeDrawingPadding()) {
                NavHost(
                    navController = navController,
                    startDestination = ChatRoute,
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                ) {
                    composable<BenchmarkModelRoute> {
                        BenchmarkModelScreen(
                            onBackClicked = { navController.navigateUp() },
                            viewModel::onEvent,
                        )
                    }
                    composable<EditChatSettingsRoute>(
                        typeMap = mapOf(typeOf<Chat>() to CustomNavTypes.ChatNavType)
                    ) { backStackEntry ->
                        val route: EditChatSettingsRoute = backStackEntry.toRoute()
                        val settings = EditableChatSettings.fromChat(route.chat)
                        EditChatSettingsScreen(
                            settings,
                            route.modelContextSize,
                            onUpdateChat = { editableChatSettings ->
                                viewModel.onEvent(
                                    ChatScreenUIEvent.ChatEvents.UpdateChatSettings(
                                        editableChatSettings,
                                        route.chat,
                                    )
                                )
                            },
                            onBackClicked = { navController.navigateUp() },
                        )
                    }
                    composable<ChatRoute> {
                        val uiState by
                        viewModel.uiState.collectAsStateWithLifecycle(
                            LocalLifecycleOwner.current
                        )
                        ChatActivityScreenUI(
                            uiState,
                            onEditChatParamsClick = { chat, modelContextSize ->
                                navController.navigate(
                                    EditChatSettingsRoute(chat, modelContextSize)
                                )
                            },
                            onBenchmarkModelClick = { navController.navigate(BenchmarkModelRoute) },
                            viewModel::onEvent,
                        )
                    }
                }
            }
        }
    }

    /**
     * Load the model when the activity is visible to the user and unload the model when the
     * activity is not visible to the user. see
     * https://developer.android.com/guide/components/activities/activity-lifecycle
     */
    override fun onStart() {
        super.onStart()
        if (modelUnloaded) {
            viewModel.loadModel()
            LOGD("onStart() called - model loaded")
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            modelUnloaded = viewModel.unloadModel()
            LOGD("onStop() called - model unloaded result: $modelUnloaded")
        }
    }
}

@Preview
@Composable
private fun PreviewChatActivityScreenUI() {
    ChatActivityScreenUI(
        uiState =
            ChatScreenUIState(
                chat = dummyChats[0].copy(llmModel = dummyLLMModels[1]),
                folders = dummyFolders.toImmutableList(),
                chats = dummyChats.toImmutableList(),
                models = dummyLLMModels.toImmutableList(),
                tasks = dummyTasksList.toImmutableList(),
            ),
        onEditChatParamsClick = { _, _ -> },
        onBenchmarkModelClick = {},
        onEvent = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatActivityScreenUI(
    uiState: ChatScreenUIState,
    onEditChatParamsClick: (Chat, Int) -> Unit,
    onBenchmarkModelClick: () -> Unit,
    onEvent: (ChatScreenUIEvent) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    SmolLMAndroidTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerUI(
                    uiState.chat,
                    uiState.chats,
                    uiState.folders,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onEvent = onEvent,
                )
                BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        modifier = Modifier.shadow(2.dp),
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AppBarTitleText(uiState.chat.name)
                                Text(
                                    if (uiState.chat.llmModelId != -1L) {
                                        uiState.chat.llmModel?.name ?: ""
                                    } else {
                                        ""
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    FeatherIcons.Menu,
                                    contentDescription = stringResource(R.string.chat_view_chats),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        },
                        actions = {
                            Box {
                                IconButton(
                                    onClick = {
                                        onEvent(
                                            ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup(
                                                visible = true
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        FeatherIcons.MoreVertical,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                ChatMoreOptionsPopup(
                                    uiState.chat,
                                    uiState.showMoreOptionsPopup,
                                    uiState.memoryUsage != null,
                                    onEditChatSettingsClick = {
                                        onEditChatParamsClick(
                                            uiState.chat,
                                            uiState.chat.llmModel?.contextSize ?: 0,
                                        )
                                    },
                                    onBenchmarkModelClick = { onBenchmarkModelClick() },
                                    onEvent = onEvent,
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.surface)
                ) {
                    ScreenUI(uiState, onEvent)
                }
            }

            if (uiState.showSelectModelListDialog) {
                SelectModelsList(
                    onDismissRequest = {
                        onEvent(
                            ChatScreenUIEvent.DialogEvents.ToggleSelectModelListDialog(
                                visible = false
                            )
                        )
                    },
                    uiState.models,
                    onModelListItemClick = { model ->
                        onEvent(ChatScreenUIEvent.ChatEvents.UpdateChatModel(model))
                    },
                    onModelDeleteClick = { model ->
                        onEvent(ChatScreenUIEvent.ChatEvents.DeleteModel(model))
                    },
                )
            }
            if (uiState.showTasksBottomSheet) {
                TasksListBottomSheet(uiState.tasks, onEvent)
            }
            if (uiState.showChangeFolderDialog) {
                ChangeFolderDialogUI(
                    onDismissRequest = {
                        onEvent(
                            ChatScreenUIEvent.DialogEvents.ToggleChangeFolderDialog(visible = false)
                        )
                    },
                    uiState.chat.folderId,
                    uiState.folders,
                    onUpdateFolderId = { folderId ->
                        onEvent(ChatScreenUIEvent.FolderEvents.UpdateChatFolder(folderId))
                    },
                )
            }
            FolderOptionsDialog()
            TextFieldDialog()
            ChatMessageOptionsDialog()
        }
    }
}

@Composable
private fun ColumnScope.ScreenUI(uiState: ChatScreenUIState, onEvent: (ChatScreenUIEvent) -> Unit) {
    if (uiState.memoryUsage != null) {
        RAMUsageLabel(uiState.memoryUsage)
    }
    Spacer(modifier = Modifier.height(4.dp))
    MessagesList(
        uiState.messages,
        uiState.isGeneratingResponse,
        uiState.renderedPartialResponse,
        uiState.chat.id,
        uiState.responseGenerationsSpeed,
        uiState.responseGenerationTimeSecs,
        onEvent,
    )
    MessageInput(uiState.chat, uiState.modelLoadingState, uiState.isGeneratingResponse, onEvent)
}

@Composable
private fun RAMUsageLabel(memoryUsage: Pair<Float, Float>) {
    val context = LocalContext.current
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        context.getString(R.string.label_device_ram).format(memoryUsage.first, memoryUsage.second),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ColumnScope.MessagesList(
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
    LazyColumn(state = listState, modifier = Modifier
        .fillMaxSize()
        .weight(1f)) {
        itemsIndexed(messages) { i, chatMessage ->
            MessageListItem(
                chatMessage.renderedMessage,
                responseGenerationSpeed =
                    if (i == messages.size - 1) responseGenerationsSpeed else null,
                responseGenerationTimeSecs =
                    if (i == messages.size - 1) responseGenerationTimeSecs else null,
                chatMessage.isUserMessage,
                onCopyClicked = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied message", chatMessage.message)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                            context,
                            context.getString(R.string.chat_message_copied),
                            Toast.LENGTH_SHORT,
                    )
                        .show()
                },
                onShareClicked = {
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, chatMessage.message)
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
            )
        }
        if (isGeneratingResponse) {
            item {
                if (renderedPartialResponse != null) {
                    MessageListItem(
                        renderedPartialResponse,
                        responseGenerationSpeed = null,
                        responseGenerationTimeSecs = null,
                        false,
                        {},
                        {},
                        onMessageEdited = {
                            // Not applicable as allowEditing is set to False
                        },
                        allowEditing = false,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.MessageListItem(
    messageStr: Spanned,
    responseGenerationSpeed: Float?,
    responseGenerationTimeSecs: Int?,
    isUserMessage: Boolean,
    onCopyClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onMessageEdited: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowEditing: Boolean,
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    if (!isUserMessage) {
        Row(modifier = modifier
            .fillMaxWidth()
            .animateItem()) {
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                ChatMessageText(
                    // to make pointerInput work in MarkdownText use disableLinkMovementMethod
                    // https://github.com/jeziellago/compose-markdown/issues/85#issuecomment-2184040304
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                            .fillMaxSize(),
                    textColor = MaterialTheme.colorScheme.onBackground.toArgb(),
                    textSize = 16f,
                    message = messageStr,
                    onLongClick = {
                        createChatMessageOptionsDialog(
                            showEditOption = false,
                            onEditClick = {
                                /** Not applicable as showEditOption is set to false * */
                            },
                            onCopyClick = { onCopyClicked() },
                            onShareClick = { onShareClicked() },
                        )
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    responseGenerationSpeed?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "%.2f tokens/s".format(it), fontSize = 8.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier =
                                Modifier
                                    .size(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "$responseGenerationTimeSecs s", fontSize = 8.sp)
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateItem(),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                var message by rememberSaveable { mutableStateOf(messageStr.toString()) }
                if (isEditing) {
                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(8.dp)
                                .widthIn(max = 250.dp),
                        colors =
                            TextFieldDefaults.colors(
                                errorContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                    )
                } else {
                    ChatMessageText(
                        modifier =
                            Modifier
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(8.dp)
                                .widthIn(max = 250.dp),
                        textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                        textSize = 16f,
                        message = messageStr,
                        onLongClick = {
                            createChatMessageOptionsDialog(
                                showEditOption = allowEditing,
                                onEditClick = { isEditing = true },
                                onCopyClick = { onCopyClicked() },
                                onShareClick = { onShareClicked() },
                            )
                        },
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allowEditing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isEditing) {
                            Text(
                                text = stringResource(R.string.edit_chat_message_done),
                                modifier =
                                    Modifier.clickable {
                                        isEditing = false
                                        onMessageEdited(message)
                                    },
                                fontSize = 6.sp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = context.getString(R.string.dialog_neg_cancel),
                                modifier =
                                    Modifier.clickable {
                                        isEditing = false
                                        message = messageStr.toString()
                                    },
                                fontSize = 6.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    currChat: Chat,
    modelLoadingState: ModelLoadingState,
    isGeneratingResponse: Boolean,
    onEvent: (ChatScreenUIEvent) -> Unit,
    defaultQuestion: String? = null,
) {
    if (currChat.llmModelId == -1L) {
        Text(modifier = Modifier.padding(8.dp), text = stringResource(R.string.chat_select_model))
    } else {
        var questionText by rememberSaveable { mutableStateOf(defaultQuestion ?: "") }
        val keyboardController = LocalSoftwareKeyboardController.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            AnimatedVisibility(modelLoadingState == ModelLoadingState.IN_PROGRESS) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.chat_loading_model),
                )
            }
            AnimatedVisibility(modelLoadingState == ModelLoadingState.FAILURE) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.chat_model_cannot_be_loaded),
                )
            }
            AnimatedVisibility(modelLoadingState == ModelLoadingState.SUCCESS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        value = questionText,
                        onValueChange = { questionText = it },
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            TextFieldDefaults.colors(
                                disabledTextColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        placeholder = { Text(text = stringResource(R.string.chat_ask_question)) },
                        keyboardOptions =
                            KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Go,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onGo = {
                                    keyboardController?.hide()
                                    onEvent(
                                        ChatScreenUIEvent.ChatEvents.SendUserQuery(questionText)
                                    )
                                    questionText = ""
                                }
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isGeneratingResponse) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                            IconButton(
                                onClick = { onEvent(ChatScreenUIEvent.ChatEvents.StopGeneration) }
                            ) {
                                Icon(FeatherIcons.StopCircle, contentDescription = "Stop")
                            }
                        }
                    } else {
                        IconButton(
                            enabled = questionText.isNotEmpty(),
                            modifier =
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape,
                                ),
                            onClick = {
                                keyboardController?.hide()
                                onEvent(ChatScreenUIEvent.ChatEvents.SendUserQuery(questionText))
                                questionText = ""
                            },
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Send,
                                contentDescription = "Send text",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksListBottomSheet(tasks: ImmutableList<Task>, onEvent: (ChatScreenUIEvent) -> Unit) {
    val context = LocalContext.current
    // adding bottom sheets in Compose
    // See https://developer.android.com/develop/ui/compose/components/bottom-sheets
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onDismissRequest = {
            onEvent(ChatScreenUIEvent.DialogEvents.ToggleTaskListBottomList(visible = false))
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (tasks.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(R.string.chat_no_task_created),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onEvent(
                            ChatScreenUIEvent.DialogEvents.ToggleTaskListBottomList(visible = true)
                        )
                        Intent(context, ManageTasksActivity::class.java).also {
                            context.startActivity(it)
                        }
                    }
                ) {
                    MediumLabelText(stringResource(R.string.chat_create_task))
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                AppBarTitleText(stringResource(R.string.chat_select_task))
                TasksList(
                    tasks,
                    onTaskSelected = { task ->
                        onEvent(ChatScreenUIEvent.ChatEvents.OnTaskSelected(task))
                    },
                    onUpdateTaskClick = { // Not applicable as showTaskOptions is set to `false`
                    },
                    onEditTaskClick = { // Not applicable as showTaskOptions is set to `false`
                    },
                    onDeleteTaskClick = { // Not applicable as showTaskOptions is set to `false`
                    },
                    enableTaskClick = true,
                    showTaskOptions = false,
                )
            }
        }
    }
}
