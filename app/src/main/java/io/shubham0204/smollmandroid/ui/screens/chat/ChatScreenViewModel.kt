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

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.shubham0204.smollm.SmolLM
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.ChatMessage
import io.shubham0204.smollmandroid.data.ChatsDB
import io.shubham0204.smollmandroid.data.MessagesDB
import io.shubham0204.smollmandroid.data.TasksDB
import io.shubham0204.smollmandroid.llm.ModelsRepository
import io.shubham0204.smollmandroid.prism4j.PrismGrammarLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.util.Date

const val LOGTAG = "[SmolLMAndroid]"
val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@KoinViewModel
class ChatScreenViewModel(
    val context: Context,
    val messagesDB: MessagesDB,
    val chatsDB: ChatsDB,
    val modelsRepository: ModelsRepository,
    val tasksDB: TasksDB,
) : ViewModel() {
    val smolLM = SmolLM()

    val currChatState = mutableStateOf<Chat?>(null)

    val isGeneratingResponse = mutableStateOf(false)
    val partialResponse = mutableStateOf("")
    var responseGenerationsSpeed: Float? = null

    val showSelectModelListDialogState = mutableStateOf(false)
    val showMoreOptionsPopupState = mutableStateOf(false)
    val showTaskListBottomListState = mutableStateOf(false)

    val isInitializingModel = mutableStateOf(false)
    var responseGenerationJob: Job? = null

    val markwon: Markwon

    init {
        currChatState.value = chatsDB.loadDefaultChat()
        val prism4j = Prism4j(PrismGrammarLocator())
        markwon =
            Markwon
                .builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDarkula.create()))
                .usePlugin(
                    object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            val jetbrainsMonoFont =
                                ResourcesCompat.getFont(context, R.font.jetbrains_mono)!!
                            builder
                                .codeBlockTypeface(
                                    ResourcesCompat.getFont(context, R.font.jetbrains_mono)!!,
                                ).codeBlockTextColor(Color.WHITE)
                                .codeBlockTextSize(spToPx(10f))
                                .codeBlockBackgroundColor(Color.BLACK)
                                .codeTypeface(jetbrainsMonoFont)
                                .codeTextSize(spToPx(10f))
                                .codeTextColor(Color.WHITE)
                                .codeBackgroundColor(Color.BLACK)
                        }
                    },
                ).build()
    }

    private fun spToPx(sp: Float): Int =
        TypedValue
            .applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
            .toInt()

    fun getChats(): Flow<List<Chat>> = chatsDB.getChats()

    fun getChatMessages(): Flow<List<ChatMessage>>? {
        return currChatState.value?.let { chat ->
            return messagesDB.getMessages(chat.id)
        }
    }

    fun updateChatLLM(modelId: Long) {
        currChatState.value = currChatState.value?.copy(llmModelId = modelId)
        chatsDB.updateChat(currChatState.value!!)
    }

    fun updateChat(chat: Chat) {
        currChatState.value = chat
        chatsDB.updateChat(chat)
        loadModel()
    }

    fun sendUserQuery(query: String) {
        currChatState.value?.let { chat ->
            chat.dateUsed = Date()
            chatsDB.updateChat(chat)
            if (chat.isTask) {
                messagesDB.deleteMessages(chat.id)
            }
            messagesDB.addUserMessage(chat.id, query)
            isGeneratingResponse.value = true
            responseGenerationJob =
                CoroutineScope(Dispatchers.Default).launch {
                    partialResponse.value = ""
                    smolLM.getResponse(query).collect { partialResponse.value += it }
                    messagesDB.addAssistantMessage(chat.id, partialResponse.value)
                    withContext(Dispatchers.Main) {
                        isGeneratingResponse.value = false
                        responseGenerationsSpeed = smolLM.getResponseGenerationSpeed()
                    }
                }
        }
    }

    fun stopGeneration() {
        isGeneratingResponse.value = false
        responseGenerationJob?.let { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
    }

    fun switchChat(chat: Chat) {
        stopGeneration()
        currChatState.value = chat
    }

    fun deleteChat(chat: Chat) {
        stopGeneration()
        chatsDB.deleteChat(chat)
        messagesDB.deleteMessages(chat.id)
        currChatState.value = null
    }

    fun deleteModel(modelId: Long) {
        modelsRepository.deleteModel(modelId)
        if (currChatState.value?.llmModelId == modelId) {
            currChatState.value = currChatState.value?.copy(llmModelId = -1)
            smolLM.close()
        }
    }

    /**
     * Load the model for the current chat. If chat is configured with a LLM (i.e. chat.llModelId !=
     * -1), then load the model. If not, show the model list dialog. Once the model is finalized,
     * read the system prompt and user messages from the database and add them to the model.
     */
    fun loadModel() {
        currChatState.value?.let { chat ->
            if (chat.llmModelId == -1L) {
                showSelectModelListDialogState.value = true
            } else {
                val model = modelsRepository.getModelFromId(chat.llmModelId)
                if (model != null) {
                    isInitializingModel.value = true
                    CoroutineScope(Dispatchers.Default).launch {
                        smolLM.create(model.path, chat.minP, chat.temperature, !chat.isTask)
                        LOGD("Model loaded")
                        if (chat.systemPrompt.isNotEmpty()) {
                            smolLM.addSystemPrompt(chat.systemPrompt)
                            LOGD("System prompt added")
                        }
                        if (!chat.isTask) {
                            messagesDB.getMessagesForModel(chat.id).forEach { message ->
                                if (message.isUserMessage) {
                                    smolLM.addUserMessage(message.message)
                                    LOGD("User message added: ${message.message}")
                                } else {
                                    smolLM.addAssistantMessage(message.message)
                                    LOGD("Assistant message added: ${message.message}")
                                }
                            }
                        }
                        withContext(Dispatchers.Main) { isInitializingModel.value = false }
                    }
                } else {
                    showSelectModelListDialogState.value = true
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        smolLM.close()
    }
}
