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
import android.text.util.Linkify
import android.util.Log
import android.util.TypedValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
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
import io.shubham0204.smollmandroid.ui.theme.AppAccentColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.util.Date
import kotlin.time.measureTime

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

    // UI state variables
    private val _currChatState = MutableStateFlow<Chat?>(null)
    val currChatState: StateFlow<Chat?> = _currChatState

    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse

    private val _isInitializingModel = MutableStateFlow(false)
    val isInitializingModel: StateFlow<Boolean> = _isInitializingModel

    private val _partialResponse = MutableStateFlow("")
    val partialResponse: StateFlow<String> = _partialResponse

    private val _showSelectModelListDialogState = MutableStateFlow(false)
    val showSelectModelListDialogState: StateFlow<Boolean> = _showSelectModelListDialogState

    private val _showMoreOptionsPopupState = MutableStateFlow(false)
    val showMoreOptionsPopupState: StateFlow<Boolean> = _showMoreOptionsPopupState

    private val _showTaskListBottomListState = MutableStateFlow(false)
    val showTaskListBottomListState: StateFlow<Boolean> = _showTaskListBottomListState


    private var responseGenerationJob: Job? = null
    private val smolLM = SmolLM()
    var responseGenerationsSpeed: Float? = null
    var responseGenerationTimeSecs: Int? = null
    val markwon: Markwon


    init {
        _currChatState.value = chatsDB.loadDefaultChat()
        val prism4j = Prism4j(PrismGrammarLocator())
        markwon =
            Markwon
                .builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDarkula.create()))
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(12f, JLatexMathPlugin.BuilderConfigure {
                    it.inlinesEnabled(true)
                    it.blocksEnabled(true)
                }))
                .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
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
                                .linkColor(AppAccentColor.toArgb())
                                .isLinkUnderlined(true)
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
        return _currChatState.value?.let { chat ->
            return messagesDB.getMessages(chat.id)
        }
    }

    fun updateChatLLM(modelId: Long) {
        _currChatState.value = _currChatState.value?.copy(llmModelId = modelId)
        chatsDB.updateChat(_currChatState.value!!)
    }

    fun updateChat(chat: Chat) {
        _currChatState.value = chat
        chatsDB.updateChat(chat)
        loadModel()
    }

    fun sendUserQuery(query: String) {
        _currChatState.value?.let { chat ->
            chat.dateUsed = Date()
            chatsDB.updateChat(chat)
            if (chat.isTask) {
                messagesDB.deleteMessages(chat.id)
            }
            messagesDB.addUserMessage(chat.id, query)
            _isGeneratingResponse.value = true
            responseGenerationJob =
                CoroutineScope(Dispatchers.Default).launch {
                    _partialResponse.value = ""
                    val responseDuration =
                        measureTime {
                            smolLM.getResponse(query).collect { _partialResponse.value += it }
                        }
                    messagesDB.addAssistantMessage(chat.id, _partialResponse.value)
                    withContext(Dispatchers.Main) {
                        _isGeneratingResponse.value = false
                        responseGenerationsSpeed = smolLM.getResponseGenerationSpeed()
                        responseGenerationTimeSecs = responseDuration.inWholeSeconds.toInt()
                    }
                }
        }
    }

    fun stopGeneration() {
        _isGeneratingResponse.value = false
        responseGenerationJob?.let { job ->
            if (job.isActive) {
                job.cancel()
            }
        }
    }

    fun switchChat(chat: Chat) {
        stopGeneration()
        _currChatState.value = chat
    }

    fun deleteChat(chat: Chat) {
        stopGeneration()
        chatsDB.deleteChat(chat)
        messagesDB.deleteMessages(chat.id)
        _currChatState.value = null
    }

    fun deleteModel(modelId: Long) {
        modelsRepository.deleteModel(modelId)
        if (_currChatState.value?.llmModelId == modelId) {
            _currChatState.value = _currChatState.value?.copy(llmModelId = -1)
            smolLM.close()
        }
    }

    /**
     * Load the model for the current chat. If chat is configured with a LLM (i.e. chat.llModelId !=
     * -1), then load the model. If not, show the model list dialog. Once the model is finalized,
     * read the system prompt and user messages from the database and add them to the model.
     */
    fun loadModel() {
        _currChatState.value?.let { chat ->
            if (chat.llmModelId == -1L) {
                _showSelectModelListDialogState.value = true
            } else {
                val model = modelsRepository.getModelFromId(chat.llmModelId)
                if (model != null) {
                    _isInitializingModel.value = true
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
                        withContext(Dispatchers.Main) { _isInitializingModel.value = false }
                    }
                } else {
                    _showSelectModelListDialogState.value = true
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        smolLM.close()
    }

    fun showSelectModelListDialog() {
        _showSelectModelListDialogState.value = true
    }

    fun hideSelectModelListDialog() {
        _showSelectModelListDialogState.value = false
    }

    fun showMoreOptionsPopup() {
        _showMoreOptionsPopupState.value = true
    }

    fun hideMoreOptionsPopup() {
        _showMoreOptionsPopupState.value = false
    }

    fun showTaskListBottomList() {
        _showTaskListBottomListState.value = true
    }

    fun hideTaskListBottomList() {
        _showTaskListBottomListState.value = false
    }
}
