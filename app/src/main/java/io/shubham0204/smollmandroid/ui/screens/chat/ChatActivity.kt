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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.MediumLabelText
import io.shubham0204.smollmandroid.ui.screens.manage_tasks.ManageTasksActivity
import io.shubham0204.smollmandroid.ui.screens.manage_tasks.TasksList
import io.shubham0204.smollmandroid.ui.theme.AppAccentColor
import io.shubham0204.smollmandroid.ui.theme.AppFontFamily
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ChatActivity : ComponentActivity() {
    private val viewModel: ChatScreenViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "chat",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("edit-chat") {
                    EditChatSettingsScreen(
                        viewModel,
                        onBackClicked = { navController.navigateUp() },
                    )
                }
                composable("chat") {
                    ChatActivityScreenUI(
                        viewModel,
                        onEditChatParamsClick = { navController.navigate("edit-chat") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatActivityScreenUI(
    viewModel: ChatScreenViewModel,
    onEditChatParamsClick: () -> Unit,
) {
    val context = LocalContext.current
    val currChat by viewModel.currChatState.collectAsStateWithLifecycle(lifecycleOwner = LocalLifecycleOwner.current)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(currChat) { viewModel.loadModel() }
    SmolLMAndroidTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerUI(
                    viewModel,
                    onItemClick = { chat ->
                        viewModel.switchChat(chat)
                        scope.launch { drawerState.close() }
                    },
                    onManageTasksClick = {
                        scope.launch { drawerState.close() }
                        Intent(context, ManageTasksActivity::class.java).also {
                            context.startActivity(it)
                        }
                    },
                    onCreateTaskClick = {
                        scope.launch { drawerState.close() }
                        viewModel.showTaskListBottomList()
                    },
                )
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AppBarTitleText(
                                    currChat?.name ?: "Select a chat",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    if (currChat != null && currChat?.llmModelId != -1L) {
                                        viewModel.modelsRepository
                                            .getModelFromId(currChat!!.llmModelId)
                                            ?.name ?: ""
                                    } else {
                                        ""
                                    },
                                    fontFamily = AppFontFamily,
                                    fontSize = 12.sp,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "View Chats",
                                    tint = Color.DarkGray,
                                )
                            }
                        },
                        actions = {
                            if (currChat != null) {
                                Box {
                                    IconButton(
                                        onClick = { viewModel.showMoreOptionsPopup() },
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                        )
                                    }
                                    ChatMoreOptionsPopup(viewModel, onEditChatParamsClick)
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    if (currChat != null) {
                        ScreenUI(viewModel, currChat!!)
                    }
                }
            }
        }
        SelectModelsList(viewModel)
        TasksListBottomSheet(viewModel)
    }
}

@Composable
private fun ColumnScope.ScreenUI(
    viewModel: ChatScreenViewModel,
    currChat: Chat,
) {
    val isGeneratingResponse by viewModel.isGeneratingResponse.collectAsStateWithLifecycle()
    MessagesList(
        viewModel,
        isGeneratingResponse,
        currChat.id,
    )
    MessageInput(
        viewModel,
        isGeneratingResponse,
    )
}

@Composable
private fun ColumnScope.MessagesList(
    viewModel: ChatScreenViewModel,
    isGeneratingResponse: Boolean,
    chatId: Long,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val messages by viewModel.getChatMessages(chatId).collectAsState(emptyList())
    val partialResponse by viewModel.partialResponse.collectAsStateWithLifecycle()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }
    LazyColumn(
        state = listState,
        modifier =
        Modifier
            .fillMaxSize()
            .weight(1f),
    ) {
        itemsIndexed(messages) { i, chatMessage ->
            MessageListItem(
                viewModel.markwon.render(viewModel.markwon.parse(chatMessage.message)),
                responseGenerationSpeed = if (i == messages.size - 1) viewModel.responseGenerationsSpeed else null,
                responseGenerationTimeSecs = if (i == messages.size - 1) viewModel.responseGenerationTimeSecs else null,
                chatMessage.isUserMessage,
                onCopyClicked = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied message", chatMessage.message)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                },
                onShareClicked = {
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, chatMessage.message)
                        },
                    )
                },
            )
        }
        if (isGeneratingResponse) {
            item {
                if (partialResponse.isNotEmpty()) {
                    MessageListItem(
                        viewModel.markwon.render(viewModel.markwon.parse(partialResponse)),
                        responseGenerationSpeed = null,
                        responseGenerationTimeSecs = null,
                        false,
                        {},
                        {},
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
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = AppAccentColor,
                        )
                        Text(
                            text = "Thinking ...",
                            modifier =
                            Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.MessageListItem(
    messageStr: Spanned,
    responseGenerationSpeed: Float?,
    responseGenerationTimeSecs: Int?,
    isUserMessage: Boolean,
    onCopyClicked: () -> Unit,
    onShareClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isUserMessage) {
        Row(
            modifier =
            modifier
                .fillMaxWidth()
                .animateItem(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    modifier = Modifier.padding(4.dp),
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = AppAccentColor,
                )
            }
            Column(modifier = Modifier) {
                ChatMessageText(
                    // to make pointerInput work in MarkdownText use disableLinkMovementMethod
                    // https://github.com/jeziellago/compose-markdown/issues/85#issuecomment-2184040304
                    modifier =
                    Modifier
                        .padding(4.dp)
                        .background(Color.Transparent)
                        .padding(4.dp)
                        .fillMaxSize(),
                    textColor = android.graphics.Color.BLACK,
                    textSize = 16f,
                    message = messageStr,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Copy",
                        modifier = Modifier.clickable { onCopyClicked() },
                        fontSize = 8.sp,
                        fontFamily = AppFontFamily,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share",
                        modifier = Modifier.clickable { onShareClicked() },
                        fontSize = 8.sp,
                        fontFamily = AppFontFamily,
                    )
                    responseGenerationSpeed?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier =
                            Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%.2f tokens/s".format(it),
                            fontSize = 8.sp,
                            fontFamily = AppFontFamily,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier =
                            Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$responseGenerationTimeSecs s",
                            fontSize = 8.sp,
                            fontFamily = AppFontFamily,
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .animateItem(),
            horizontalArrangement = Arrangement.End,
        ) {
            ChatMessageText(
                modifier =
                Modifier
                    .padding(8.dp)
                    .background(AppAccentColor, RoundedCornerShape(16.dp))
                    .padding(8.dp)
                    .widthIn(max = 250.dp),
                textColor = android.graphics.Color.WHITE,
                textSize = 16f,
                message = messageStr,
            )
        }
    }
}

@Composable
private fun MessageInput(
    viewModel: ChatScreenViewModel,
    isGeneratingResponse: Boolean,
) {
    val currChat by viewModel.currChatState.collectAsStateWithLifecycle()
    val modelLoadingState by viewModel.modelLoadState.collectAsStateWithLifecycle()
    if ((currChat?.llmModelId ?: -1L) == -1L) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = "Select a model ...",
            fontFamily = AppFontFamily,
        )
    } else {
        var questionText by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            when (modelLoadingState) {
                ChatScreenViewModel.ModelLoadingState.IN_PROGRESS -> {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Loading model...",
                        fontFamily = AppFontFamily,
                    )
                }

                ChatScreenViewModel.ModelLoadingState.FAILURE -> {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "The model selected for the chat cannot be loaded",
                        fontFamily = AppFontFamily,
                    )
                }

                ChatScreenViewModel.ModelLoadingState.SUCCESS -> {
                    TextField(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        value = questionText,
                        onValueChange = { questionText = it },
                        shape = RoundedCornerShape(16.dp),
                        colors =
                        TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            disabledTextColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        placeholder = {
                            Text(
                                text = "Ask a question",
                            )
                        },
                        keyboardOptions =
                        KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isGeneratingResponse) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppAccentColor)
                            IconButton(onClick = { viewModel.stopGeneration() }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.DarkGray)
                            }
                        }
                    } else {
                        IconButton(
                            enabled = questionText.isNotEmpty(),
                            modifier = Modifier.background(AppAccentColor, CircleShape),
                            onClick = {
                                keyboardController?.hide()
                                viewModel.sendUserQuery(questionText)
                                questionText = ""
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Send text",
                                tint = Color.White,
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksListBottomSheet(viewModel: ChatScreenViewModel) {
    val context = LocalContext.current
    val showTaskListBottomList by viewModel.showTaskListBottomListState.collectAsStateWithLifecycle()
    if (showTaskListBottomList) {
        // adding bottom sheets in Compose
        // See https://developer.android.com/develop/ui/compose/components/bottom-sheets
        ModalBottomSheet(
            containerColor = Color.White,
            onDismissRequest = { viewModel.hideTaskListBottomList() },
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val tasks by viewModel.tasksDB.getTasks().collectAsState(emptyList())
                if (tasks.isEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "No tasks created",
                        fontFamily = AppFontFamily,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.showTaskListBottomList()
                            Intent(context, ManageTasksActivity::class.java).also {
                                context.startActivity(it)
                            }
                        },
                    ) {
                        MediumLabelText("Create a task")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    AppBarTitleText("Select A Task")
                    TasksList(
                        tasks.map {
                            val modelName =
                                viewModel.modelsRepository.getModelFromId(it.modelId)?.name
                                    ?: return@map it
                            it.copy(modelName = modelName)
                        },
                        onTaskSelected = { task ->
                            val newChatId =
                                viewModel.chatsDB.addChat(
                                    chatName = task.name,
                                    systemPrompt = task.systemPrompt,
                                    llmModelId = task.modelId,
                                )
                            viewModel.switchChat(
                                Chat(
                                    id = newChatId,
                                    name = task.name,
                                    systemPrompt = task.systemPrompt,
                                    llmModelId = task.modelId,
                                    isTask = true,
                                ),
                            )
                            viewModel.showTaskListBottomList()
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
}

@Composable
private fun SelectModelsList(viewModel: ChatScreenViewModel) {
    val showSelectModelsListDialog by viewModel.showSelectModelListDialogState.collectAsStateWithLifecycle()
    if (showSelectModelsListDialog) {
        val modelsList by
        viewModel.modelsRepository.getAvailableModels().collectAsState(emptyList())
        SelectModelsList(
            onDismissRequest = { viewModel.hideSelectModelListDialog() },
            modelsList,
            onModelListItemClick = { model ->
                viewModel.updateChatLLM(model.id)
                viewModel.loadModel()
                viewModel.hideSelectModelListDialog()
            },
            onModelDeleteClick = { model ->
                viewModel.deleteModel(model.id)
                Toast
                    .makeText(
                        viewModel.context,
                        "Model '${model.name}' deleted",
                        Toast.LENGTH_LONG,
                    ).show()
            },
        )
    }
}
