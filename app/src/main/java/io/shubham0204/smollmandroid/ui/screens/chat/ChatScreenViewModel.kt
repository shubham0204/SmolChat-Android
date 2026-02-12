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

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.Spanned
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shubham0204.smollmandroid.ui.screens.whisper_download.DownloadWhisperModelActivity
import io.shubham0204.smollm.SmolLM
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.ChatMessage
import io.shubham0204.smollmandroid.data.Folder
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.data.PreferencesManager
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.llm.ModelsRepository
import io.shubham0204.smollmandroid.llm.SmolLMManager
import io.shubham0204.smollmandroid.service.VoiceChatService
import io.shubham0204.smollmandroid.service.VoiceChatServiceManager
import io.shubham0204.smollmandroid.stt.SpeechToTextManager
import io.shubham0204.smollmandroid.stt.STTState
import io.shubham0204.smollmandroid.tts.TextToSpeechManager
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.util.Date
import kotlin.math.pow

private const val LOGTAG = "[SmolLMAndroid-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

sealed class ChatScreenUIEvent {
    sealed class ChatEvents {
        data class UpdateChatModel(val model: LLMModel) : ChatScreenUIEvent()

        data object LoadChatModel : ChatScreenUIEvent()

        data class DeleteModel(val model: LLMModel) : ChatScreenUIEvent()

        data class SendUserQuery(val query: String, val fromVoice: Boolean = false) :
            ChatScreenUIEvent()

        data object ToggleMicRecording : ChatScreenUIEvent()

        data object RecordingPermissionGranted : ChatScreenUIEvent()

        data object RecordingPermissionHandled : ChatScreenUIEvent()

        data object NotificationPermissionHandled : ChatScreenUIEvent()

        data object EnablePocketMode : ChatScreenUIEvent()

        data object DisablePocketMode : ChatScreenUIEvent()

        data object TrimOldMessages : ChatScreenUIEvent()

        data object DismissContextWarning : ChatScreenUIEvent()

        data object ContinueAnywayContextWarning : ChatScreenUIEvent()

        data object StopGeneration : ChatScreenUIEvent()

        data class OnTaskSelected(val task: Task) : ChatScreenUIEvent()

        data class OnMessageEdited(
            val chatId: Long,
            val oldMessage: ChatMessage,
            val lastMessage: ChatMessage,
            val newMessageText: String,
        ) : ChatScreenUIEvent()

        data class OnDeleteChat(val chat: Chat) : ChatScreenUIEvent()

        data class OnDeleteChatMessages(val chat: Chat) : ChatScreenUIEvent()

        data object NewChat : ChatScreenUIEvent()

        data class SwitchChat(val chat: Chat) : ChatScreenUIEvent()

        data class UpdateChatSettings(val settings: EditableChatSettings, val existingChat: Chat) :
            ChatScreenUIEvent()

        data class StartBenchmark(val onResult: (String) -> Unit) : ChatScreenUIEvent()
    }

    sealed class FolderEvents {
        data class UpdateChatFolder(val newFolderId: Long) : ChatScreenUIEvent()

        data class AddFolder(val folderName: String) : ChatScreenUIEvent()

        data class UpdateFolder(val folder: Folder) : ChatScreenUIEvent()

        data class DeleteFolder(val folderId: Long) : ChatScreenUIEvent()

        data class DeleteFolderWithChats(val folderId: Long) : ChatScreenUIEvent()
    }

    sealed class DialogEvents {
        data class ToggleChangeFolderDialog(val visible: Boolean) : ChatScreenUIEvent()

        data class ToggleSelectModelListDialog(val visible: Boolean) : ChatScreenUIEvent()

        data class ToggleMoreOptionsPopup(val visible: Boolean) : ChatScreenUIEvent()

        data class ToggleTaskListBottomList(val visible: Boolean) : ChatScreenUIEvent()

        data object ToggleRAMUsageLabel : ChatScreenUIEvent()

        data class ShowContextLengthUsageDialog(val chat: Chat) : ChatScreenUIEvent()
    }

    sealed class TTSEvents {
        data class ToggleTTS(val enabled: Boolean) : ChatScreenUIEvent()
    }

    sealed class AutoSubmitEvents {
        data class ToggleAutoSubmit(val enabled: Boolean) : ChatScreenUIEvent()
        data class UpdateAutoSubmitDelay(val delayMs: Long) : ChatScreenUIEvent()
    }

    sealed class ContextEvents {
        data class ToggleAutoContextTrim(val enabled: Boolean) : ChatScreenUIEvent()
    }

    sealed class STTEvents {
        data class UpdateSTTLanguage(val language: String) : ChatScreenUIEvent()
    }
}

data class ChatScreenUIState(
    val chat: Chat = Chat(),
    val isGeneratingResponse: Boolean = false,
    val renderedPartialResponse: Spanned? = null,
    val modelLoadingState: ChatScreenViewModel.ModelLoadingState =
        ChatScreenViewModel.ModelLoadingState.NOT_LOADED,
    val responseGenerationsSpeed: Float? = null,
    val responseGenerationTimeSecs: Int? = null,
    val memoryUsage: Pair<Float, Float>? = null,
    val folders: ImmutableList<Folder> = emptyList<Folder>().toImmutableList(),
    val chats: ImmutableList<Chat> = emptyList<Chat>().toImmutableList(),
    val models: ImmutableList<LLMModel> = emptyList<LLMModel>().toImmutableList(),
    val messages: ImmutableList<ChatMessage> = emptyList<ChatMessage>().toImmutableList(),
    val tasks: ImmutableList<Task> = emptyList<Task>().toImmutableList(),
    val benchmarkResult: String? = null,
    val showChangeFolderDialog: Boolean = false,
    val showSelectModelListDialog: Boolean = false,
    val showMoreOptionsPopup: Boolean = false,
    val showTasksBottomSheet: Boolean = false,
    val ttsEnabled: Boolean = false,
    val autoSubmitEnabled: Boolean = false,
    val autoSubmitDelayMs: Long = 2000L,
    val sttState: STTState = STTState.Idle,
    val pendingTranscribedText: String? = null,
    val requestRecordingPermission: Boolean = false,
    val requestNotificationPermission: Boolean = false,
    val triggerAutoSubmit: Boolean = false,
    val sttLanguage: String = "en",
    val lastInputWasVoice: Boolean = false,
    val isVoiceModeActive: Boolean = false,
    val isPocketModeEnabled: Boolean = false,
    val shouldClearInput: Boolean = false,
    val isTTSSpeaking: Boolean = false,
    val showContextWarningDialog: Boolean = false,
    val contextWarningShownForThisChat: Boolean = false,
    val contextTrimLevel: Int = 0, // 0=none, 1=light, 2=medium, 3=aggressive
    val pendingRetryQuery: String? = null, // Query to retry after context trim
    val pendingRetryFromVoice: Boolean = false,
    val autoContextTrimEnabled: Boolean = false, // Auto-trim context without prompting
)

@KoinViewModel
class ChatScreenViewModel(
    val context: Context,
    val appDB: AppDB,
    val modelsRepository: ModelsRepository,
    val smolLMManager: SmolLMManager,
    val mdRenderer: MDRenderer,
    val preferencesManager: PreferencesManager,
    val ttsManager: TextToSpeechManager,
    val sttManager: SpeechToTextManager,
    val voiceChatServiceManager: VoiceChatServiceManager,
) : ViewModel() {
    enum class ModelLoadingState {
        NOT_LOADED, // model loading not started
        IN_PROGRESS, // model loading in-progress
        SUCCESS, // model loading finished successfully
        FAILURE, // model loading failed
    }

    private val _uiState = MutableStateFlow(ChatScreenUIState())
    val uiState: StateFlow<ChatScreenUIState> = _uiState

    // Used to pre-set a value in the query text-field of the chat screen
    // It is set when a query comes from a 'share-text' intent in ChatActivity
    var questionTextDefaultVal: String? = null

    // regex to replace <think> tags with <blockquote>
    // to render them correctly in Markdown
    private val findThinkTagRegex = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
    private var activityManager: ActivityManager

    init {
        setupCollectors()
        loadModel()
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        _uiState.update {
            it.copy(
                ttsEnabled = preferencesManager.ttsEnabled,
                autoSubmitEnabled = preferencesManager.autoSubmitEnabled,
                autoSubmitDelayMs = preferencesManager.autoSubmitDelayMs,
                sttLanguage = preferencesManager.sttLanguage,
                autoContextTrimEnabled = preferencesManager.autoContextTrimEnabled
            )
        }
        // Set TTS language from saved preference
        ttsManager.setLanguage(preferencesManager.sttLanguage)
        // Collect STT state changes
        viewModelScope.launch {
            sttManager.state.collect { sttState ->
                _uiState.update { it.copy(sttState = sttState) }
            }
        }
        // Collect streaming transcription - this now emits the FULL transcription
        viewModelScope.launch {
            sttManager.streamingTranscription.collect { fullTranscription ->
                if (fullTranscription.isNotBlank()) {
                    _uiState.update { it.copy(pendingTranscribedText = fullTranscription) }
                }
            }
        }
        // Set up direct callback for silence detection
        // This callback is called directly from SpeechToTextManager's IO coroutine scope
        // which bypasses the frozen ViewModel coroutines on Samsung devices
        sttManager.setOnSilenceDetectedCallback { finalText ->
            LOGD(">>> onSilenceDetectedCallback called on thread: ${Thread.currentThread().name}")
            LOGD(">>> finalText='$finalText', autoSubmitEnabled=${_uiState.value.autoSubmitEnabled}")

            if (_uiState.value.autoSubmitEnabled && finalText.isNotBlank()) {
                LOGD(">>> Calling sendUserQuery from callback...")
                _uiState.update { it.copy(pendingTranscribedText = null, shouldClearInput = true) }
                sendUserQuery(finalText, fromVoice = true)
                LOGD(">>> sendUserQuery returned from callback")
            } else {
                LOGD(">>> Not auto-submitting: autoSubmitEnabled=${_uiState.value.autoSubmitEnabled}, finalText.isNotBlank=${finalText.isNotBlank()}")
            }
        }

        // Keep the flow collector as fallback (for manual stop recording)
        viewModelScope.launch(Dispatchers.Default) {
            LOGD(">>> Starting silence detection flow collector (fallback)")
            sttManager.silenceDetected.collect {
                LOGD(">>> silenceDetected flow RECEIVED (fallback path)")
                // This is now only used as fallback when callback is not set
            }
        }
        // Collect TTS speaking state to disable mic while speaking
        viewModelScope.launch {
            ttsManager.isSpeakingFlow.collect { isSpeaking ->
                _uiState.update { it.copy(isTTSSpeaking = isSpeaking) }
            }
        }
        // Collect TTS completion events to resume recording if the input was from voice
        viewModelScope.launch {
            ttsManager.allSpeechFinished.collect {
                LOGD("TTS finished. lastInputWasVoice=${_uiState.value.lastInputWasVoice}, ttsEnabled=${_uiState.value.ttsEnabled}, isGeneratingResponse=${_uiState.value.isGeneratingResponse}, isTTSSpeaking=${_uiState.value.isTTSSpeaking}")

                // Check context usage after TTS finishes
                checkContextUsage()

                // Resume recording if:
                // - Voice mode is active
                // - The last input was from voice
                // - TTS is enabled (indicating voice conversation mode)
                // - We're not currently generating a response
                // - Context warning dialog is not showing
                // - TTS is not currently speaking (e.g., context warning message)
                if (_uiState.value.isVoiceModeActive &&
                    _uiState.value.lastInputWasVoice &&
                    _uiState.value.ttsEnabled &&
                    !_uiState.value.isGeneratingResponse &&
                    !_uiState.value.showContextWarningDialog &&
                    !_uiState.value.isTTSSpeaking
                ) {
                    LOGD("Resuming recording after TTS finished")
                    _uiState.update { it.copy(lastInputWasVoice = false) }
                    // Use toggleMicRecording which handles all state checks properly
                    toggleMicRecording()
                } else {
                    LOGD("NOT resuming recording: isVoiceModeActive=${_uiState.value.isVoiceModeActive}, lastInputWasVoice=${_uiState.value.lastInputWasVoice}, isTTSSpeaking=${_uiState.value.isTTSSpeaking}")
                }
            }
        }
        // Collect stop requests from the notification action
        viewModelScope.launch {
            voiceChatServiceManager.stopServiceRequest.collect {
                LOGD("Stop voice mode requested from notification")
                stopVoiceMode()
            }
        }
    }

    /**
     * Load the model for the current chat. If chat is configured with a LLM (i.e. chat.llModelId !=
     * -1), then load the model. If not, show the model list dialog. Once the model is finalized,
     * read the system prompt and user messages from the database and add them to the model.
     */
    fun loadModel(onComplete: (ModelLoadingState) -> Unit = {}) {
        val chat = _uiState.value.chat
        val model = modelsRepository.getModelFromId(chat.llmModelId)
        if (chat.llmModelId == -1L) {
            _uiState.update { it.copy(showSelectModelListDialog = true) }
        } else {
            _uiState.update { it.copy(modelLoadingState = ModelLoadingState.IN_PROGRESS) }
            smolLMManager.load(
                chat,
                model.path,
                SmolLM.InferenceParams(
                    chat.minP,
                    chat.temperature,
                    !chat.isTask,
                    chat.contextSize.toLong(),
                    chat.chatTemplate.takeIf { it.isNotBlank() && ("{%" in it || "{{" in it) },
                    chat.nThreads,
                    chat.useMmap,
                    chat.useMlock,
                ),
                onError = { e ->
                    _uiState.update { it.copy(modelLoadingState = ModelLoadingState.FAILURE) }
                    onComplete(ModelLoadingState.FAILURE)
                    createAlertDialog(
                        dialogTitle = context.getString(R.string.dialog_err_title),
                        dialogText = context.getString(R.string.dialog_err_text, e.message),
                        dialogPositiveButtonText =
                            context.getString(R.string.dialog_err_change_model),
                        onPositiveButtonClick = {
                            onEvent(
                                ChatScreenUIEvent.DialogEvents.ToggleSelectModelListDialog(
                                    visible = true
                                )
                            )
                        },
                        dialogNegativeButtonText = context.getString(R.string.dialog_err_close),
                        onNegativeButtonClick = {},
                    )
                },
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            modelLoadingState = ModelLoadingState.SUCCESS,
                            memoryUsage = if (it.memoryUsage != null) {
                                getCurrentMemoryUsage()
                            } else {
                                null
                            }
                        )
                    }
                    onComplete(ModelLoadingState.SUCCESS)

                    // Check if there's a pending retry query after context trim
                    val pendingQuery = _uiState.value.pendingRetryQuery
                    val pendingFromVoice = _uiState.value.pendingRetryFromVoice
                    if (pendingQuery != null && _uiState.value.isPocketModeEnabled) {
                        LOGD("Retrying pending query after context trim: '$pendingQuery'")
                        _uiState.update { it.copy(pendingRetryQuery = null, pendingRetryFromVoice = false) }
                        // Small delay to let TTS finish announcing the trim
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(500)
                            sendUserQuery(pendingQuery, addMessageToDB = false, fromVoice = pendingFromVoice)
                        }
                    }
                },
            )
        }
    }

    /** Clears the resources occupied by the model only if the inference is not in progress. */
    fun unloadModel(): Boolean =
        if (!smolLMManager.isInferenceOn) {
            smolLMManager.unload()
            _uiState.update { it.copy(modelLoadingState = ModelLoadingState.NOT_LOADED) }
            true
        } else {
            false
        }

    @SuppressLint("StringFormatMatches")
    fun onEvent(event: ChatScreenUIEvent) {
        when (event) {
            is ChatScreenUIEvent.DialogEvents.ToggleSelectModelListDialog -> {
                _uiState.update { it.copy(showSelectModelListDialog = event.visible) }
            }

            is ChatScreenUIEvent.DialogEvents.ToggleMoreOptionsPopup -> {
                _uiState.update { it.copy(showMoreOptionsPopup = event.visible) }
            }

            is ChatScreenUIEvent.DialogEvents.ToggleTaskListBottomList -> {
                _uiState.update { it.copy(showTasksBottomSheet = event.visible) }
            }

            is ChatScreenUIEvent.DialogEvents.ToggleChangeFolderDialog -> {
                _uiState.update { it.copy(showChangeFolderDialog = event.visible) }
            }

            ChatScreenUIEvent.DialogEvents.ToggleRAMUsageLabel -> {
                _uiState.update {
                    it.copy(
                        memoryUsage =
                            if (it.memoryUsage != null) {
                                null
                            } else {
                                getCurrentMemoryUsage()
                            }
                    )
                }
            }

            is ChatScreenUIEvent.DialogEvents.ShowContextLengthUsageDialog -> {
                createAlertDialog(
                    dialogTitle = context.getString(R.string.dialog_ctx_usage_title),
                    dialogText =
                        context.getString(
                            R.string.dialog_ctx_usage_text,
                            event.chat.contextSizeConsumed,
                            event.chat.contextSize,
                        ),
                    dialogPositiveButtonText = context.getString(R.string.dialog_ctx_usage_close),
                    onPositiveButtonClick = {},
                    dialogNegativeButtonText = null,
                    onNegativeButtonClick = null,
                )
            }

            is ChatScreenUIEvent.FolderEvents.UpdateChatFolder -> {
                appDB.updateChat(_uiState.value.chat.copy(folderId = event.newFolderId))
            }

            is ChatScreenUIEvent.FolderEvents.AddFolder -> {
                appDB.addFolder(event.folderName)
            }

            is ChatScreenUIEvent.FolderEvents.UpdateFolder -> {
                appDB.updateFolder(event.folder)
            }

            is ChatScreenUIEvent.FolderEvents.DeleteFolder -> {
                appDB.deleteFolder(event.folderId)
            }

            is ChatScreenUIEvent.FolderEvents.DeleteFolderWithChats -> {
                appDB.deleteFolderWithChats(event.folderId)
            }

            is ChatScreenUIEvent.ChatEvents.UpdateChatModel -> {
                updateChatLLMParams(event.model.id, event.model.chatTemplate)
                loadModel()
                onEvent(ChatScreenUIEvent.DialogEvents.ToggleSelectModelListDialog(visible = false))
            }

            is ChatScreenUIEvent.ChatEvents.DeleteModel -> {
                deleteModel(event.model.id)
                Toast.makeText(
                    context,
                    context.getString(R.string.chat_model_deleted, event.model.name),
                    Toast.LENGTH_LONG,
                )
                    .show()
            }

            ChatScreenUIEvent.ChatEvents.LoadChatModel -> {}

            is ChatScreenUIEvent.ChatEvents.SendUserQuery -> {
                sendUserQuery(event.query, fromVoice = event.fromVoice)
            }

            ChatScreenUIEvent.ChatEvents.StopGeneration -> {
                stopGeneration()
            }

            is ChatScreenUIEvent.ChatEvents.OnTaskSelected -> {
                // Using parameters from the `task`
                // create a `Chat` instance and switch to it
                modelsRepository.getModelFromId(event.task.modelId).let { model ->
                    val newTask =
                        appDB.addChat(
                            chatName = event.task.name,
                            chatTemplate = model.chatTemplate,
                            systemPrompt = event.task.systemPrompt,
                            llmModelId = event.task.modelId,
                            isTask = true,
                        )
                    switchChat(newTask)
                    onEvent(
                        ChatScreenUIEvent.DialogEvents.ToggleTaskListBottomList(visible = false)
                    )
                }
            }

            is ChatScreenUIEvent.ChatEvents.OnMessageEdited -> {
                // viewModel.sendUserQuery will add a new message to the chat
                // hence we delete the old message and the corresponding LLM
                // response if there exists one
                // TODO: There should be no need to unload/load the model again
                //       as only the conversation messages have changed.
                //       Currently there's no native function to edit the conversation
                // messages
                //       so unload (remove all messages) and load (add all messages) the
                // model.
                deleteMessage(event.oldMessage.id)
                if (!event.lastMessage.isUserMessage) {
                    deleteMessage(event.lastMessage.id)
                }
                appDB.addUserMessage(event.chatId, event.newMessageText)
                unloadModel()
                loadModel(
                    onComplete = {
                        if (it == ModelLoadingState.SUCCESS) {
                            sendUserQuery(event.newMessageText, addMessageToDB = false)
                        }
                    }
                )
            }

            is ChatScreenUIEvent.ChatEvents.OnDeleteChat -> {
                createAlertDialog(
                    dialogTitle = context.getString(R.string.dialog_title_delete_chat),
                    dialogText =
                        context.getString(R.string.dialog_text_delete_chat, event.chat.name),
                    dialogPositiveButtonText = context.getString(R.string.dialog_pos_delete),
                    dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                    onPositiveButtonClick = {
                        deleteChat(event.chat)
                        Toast.makeText(
                            context,
                            "Chat '${event.chat.name}' deleted",
                            Toast.LENGTH_LONG,
                        )
                            .show()
                    },
                    onNegativeButtonClick = {},
                )
            }

            is ChatScreenUIEvent.ChatEvents.OnDeleteChatMessages -> {
                createAlertDialog(
                    dialogTitle = context.getString(R.string.chat_options_clear_messages),
                    dialogText = context.getString(R.string.chat_options_clear_messages_text),
                    dialogPositiveButtonText = context.getString(R.string.dialog_pos_clear),
                    dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                    onPositiveButtonClick = {
                        deleteChatMessages(event.chat)
                        unloadModel()
                        loadModel(onComplete = {
                            if (it == ModelLoadingState.SUCCESS) {
                                Toast.makeText(
                                    context,
                                    "Chat '${event.chat.name}' cleared",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        })

                    },
                    onNegativeButtonClick = {},
                )
            }

            ChatScreenUIEvent.ChatEvents.NewChat -> {
                val chatCount = appDB.getChatsCount()
                val newChat = appDB.addChat(chatName = "Untitled ${chatCount + 1}")
                switchChat(newChat)
            }

            ChatScreenUIEvent.ChatEvents.ToggleMicRecording -> {
                toggleMicRecording()
            }

            ChatScreenUIEvent.ChatEvents.RecordingPermissionGranted -> {
                // Permission was granted, now start recording
                startRecordingAfterPermission()
            }

            ChatScreenUIEvent.ChatEvents.RecordingPermissionHandled -> {
                // Reset the permission request flag
                _uiState.update { it.copy(requestRecordingPermission = false) }
            }

            ChatScreenUIEvent.ChatEvents.NotificationPermissionHandled -> {
                // Reset the permission request flag and continue with voice mode
                _uiState.update { it.copy(requestNotificationPermission = false) }
                // Now continue with the voice mode start
                startVoiceModeAfterPermissions()
            }

            ChatScreenUIEvent.ChatEvents.EnablePocketMode -> {
                LOGD("Enabling pocket mode")
                _uiState.update { it.copy(isPocketModeEnabled = true) }
            }

            ChatScreenUIEvent.ChatEvents.DisablePocketMode -> {
                LOGD("Disabling pocket mode")
                _uiState.update { it.copy(isPocketModeEnabled = false) }
            }

            ChatScreenUIEvent.ChatEvents.TrimOldMessages -> {
                LOGD("Trimming old messages to free context")
                val wasVoiceModeActive = _uiState.value.isVoiceModeActive
                trimOldMessages()
                // Reset the warning flag so it can show again when context fills up
                _uiState.update { it.copy(showContextWarningDialog = false, contextWarningShownForThisChat = false) }
                // Resume recording if voice mode was active
                if (wasVoiceModeActive && _uiState.value.ttsEnabled) {
                    LOGD("Resuming recording after context trim")
                    toggleMicRecording()
                }
            }

            ChatScreenUIEvent.ChatEvents.DismissContextWarning -> {
                _uiState.update { it.copy(showContextWarningDialog = false) }
            }

            ChatScreenUIEvent.ChatEvents.ContinueAnywayContextWarning -> {
                val wasVoiceModeActive = _uiState.value.isVoiceModeActive
                _uiState.update { it.copy(showContextWarningDialog = false, contextWarningShownForThisChat = true) }
                // Resume recording if voice mode was active
                if (wasVoiceModeActive && _uiState.value.ttsEnabled) {
                    LOGD("Resuming recording after context warning dismissed")
                    toggleMicRecording()
                }
            }

            is ChatScreenUIEvent.ChatEvents.SwitchChat -> {
                switchChat(event.chat)
            }

            is ChatScreenUIEvent.ChatEvents.UpdateChatSettings -> {
                val newChat = event.settings.toChat(event.existingChat)
                _uiState.update { it.copy(chat = newChat) }
                appDB.updateChat(newChat)
                unloadModel()
                loadModel()
            }

            is ChatScreenUIEvent.ChatEvents.StartBenchmark -> {
                smolLMManager.benchmark { result ->
                    event.onResult(result)
                }
            }

            is ChatScreenUIEvent.TTSEvents.ToggleTTS -> {
                preferencesManager.ttsEnabled = event.enabled
                _uiState.update { it.copy(ttsEnabled = event.enabled) }
                if (!event.enabled) {
                    ttsManager.stop()
                }
            }

            is ChatScreenUIEvent.AutoSubmitEvents.ToggleAutoSubmit -> {
                preferencesManager.autoSubmitEnabled = event.enabled
                _uiState.update { it.copy(autoSubmitEnabled = event.enabled) }
            }

            is ChatScreenUIEvent.AutoSubmitEvents.UpdateAutoSubmitDelay -> {
                preferencesManager.autoSubmitDelayMs = event.delayMs
                _uiState.update { it.copy(autoSubmitDelayMs = event.delayMs) }
            }

            is ChatScreenUIEvent.STTEvents.UpdateSTTLanguage -> {
                preferencesManager.sttLanguage = event.language
                _uiState.update { it.copy(sttLanguage = event.language) }
                // Update TTS language to match
                ttsManager.setLanguage(event.language)
            }

            is ChatScreenUIEvent.ContextEvents.ToggleAutoContextTrim -> {
                preferencesManager.autoContextTrimEnabled = event.enabled
                _uiState.update { it.copy(autoContextTrimEnabled = event.enabled) }
            }
        }
    }

    private fun setupCollectors() {
        _uiState.update { it.copy(chat = appDB.loadDefaultChat()) }
        viewModelScope.launch {
            launch {
                appDB.getChats().collect { chats ->
                    _uiState.update { it.copy(chats = chats.toImmutableList()) }
                }
            }
            launch {
                appDB.getFolders().collect { folders ->
                    _uiState.update { it.copy(folders = folders.toImmutableList()) }
                }
            }
            launch {
                appDB.getTasks().collect { tasks ->
                    _uiState.update {
                        it.copy(
                            tasks =
                                tasks
                                    .map { task ->
                                        task.copy(
                                            modelName =
                                                modelsRepository.getModelFromId(task.modelId).name
                                        )
                                    }
                                    .toImmutableList()
                        )
                    }
                }
            }
            launch {
                appDB.getModels().collect { models ->
                    _uiState.update { it.copy(models = models.toImmutableList()) }
                }
            }
            launch {
                _uiState
                    .map { it.chat }
                    .distinctUntilChanged()
                    .collectLatest { chat ->
                        appDB.getMessages(chat.id).collect { chatMessages ->
                            _uiState.update {
                                it.copy(
                                    messages =
                                        chatMessages
                                            .map { chatMessage ->
                                                chatMessage.renderedMessage =
                                                    mdRenderer.render(chatMessage.message)
                                                chatMessage
                                            }
                                            .toImmutableList()
                                )
                            }
                        }
                    }
            }
            launch {
                _uiState
                    .map { it.chat }
                    .distinctUntilChanged()
                    .collectLatest { chat ->
                        _uiState.update { uiState ->
                            uiState.copy(
                                chat =
                                    uiState.chat.copy(
                                        llmModel =
                                            modelsRepository.getModelFromId(uiState.chat.llmModelId)
                                    )
                            )
                        }
                    }
            }
        }
    }

    private fun updateChatLLMParams(modelId: Long, chatTemplate: String) {
        val newChat = _uiState.value.chat.copy(llmModelId = modelId, chatTemplate = chatTemplate)
        _uiState.update { it.copy(chat = newChat) }
        appDB.updateChat(newChat)
    }

    private fun deleteMessage(messageId: Long) {
        appDB.deleteMessage(messageId)
    }

    private fun sendUserQuery(
        query: String,
        addMessageToDB: Boolean = true,
        fromVoice: Boolean = false
    ) {
        LOGD(">>> sendUserQuery START on thread: ${Thread.currentThread().name}")
        LOGD(">>> query='$query', fromVoice=$fromVoice")

        val chat = uiState.value.chat

        // Pre-query context check in pocket mode - trim before sending if needed
        if (_uiState.value.isPocketModeEnabled && chat.contextSize > 0) {
            val usagePercent = (chat.contextSizeConsumed.toFloat() / chat.contextSize.toFloat()) * 100
            LOGD(">>> Pre-query context check: ${usagePercent.toInt()}%")
            if (usagePercent >= 70) {
                LOGD(">>> Pocket mode: pre-emptive context trim before query")
                val message = context.getString(R.string.context_auto_trim_voice)
                ttsManager.speakChunk(message)
                ttsManager.speakRemainingBuffer()
                trimOldMessages()
                // Reset context consumed to prevent immediate re-triggering
                val updatedChat = _uiState.value.chat.copy(contextSizeConsumed = 0)
                _uiState.update { it.copy(chat = updatedChat) }
                appDB.updateChat(updatedChat)
                LOGD(">>> Context consumed reset to 0 after pre-query trim")
            }
        }

        // Update the 'dateUsed' attribute of the current Chat instance
        // when a query is sent by the user
        chat.dateUsed = Date()
        LOGD(">>> Updating chat in DB...")
        appDB.updateChat(chat)
        LOGD(">>> Chat updated")

        if (chat.isTask) {
            // If the chat is a 'task', delete all existing messages
            // to maintain the 'stateless' nature of the task
            appDB.deleteMessages(chat.id)
        }

        if (addMessageToDB) {
            LOGD(">>> Adding user message to DB...")
            appDB.addUserMessage(chat.id, query)
            LOGD(">>> User message added")
        }

        // Stop any ongoing TTS before starting new response
        LOGD(">>> Resetting TTS state...")
        ttsManager.resetState()
        LOGD(">>> TTS state reset")

        // Track if this input came from voice for resuming recording after TTS
        LOGD(">>> Updating UI state...")
        _uiState.update {
            it.copy(
                isGeneratingResponse = true,
                renderedPartialResponse = null,
                lastInputWasVoice = fromVoice
            )
        }
        LOGD(">>> UI state updated, calling smolLMManager.getResponse...")
        smolLMManager.getResponse(
            query,
            responseTransform = {
                // Replace <think> tags with <blockquote> tags
                // to get a neat Markdown rendering
                findThinkTagRegex.replace(it) { matchResult ->
                    "<blockquote><i><h6>${matchResult.groupValues[1].trim()}</i></h6></blockquote>"
                }
            },
            onPartialResponseGenerated = { resp ->
                _uiState.update { it.copy(renderedPartialResponse = mdRenderer.render(resp)) }
                // Speak the response chunk if TTS is enabled
                if (_uiState.value.ttsEnabled) {
                    ttsManager.speakChunk(resp)
                }
            },
            onSuccess = { response ->
                val updatedChat = chat.copy(contextSizeConsumed = response.contextLengthUsed)
                _uiState.update {
                    it.copy(
                        chat = updatedChat,
                        isGeneratingResponse = false,
                        responseGenerationsSpeed = response.generationSpeed,
                        responseGenerationTimeSecs = response.generationTimeSecs,
                        memoryUsage = if (it.memoryUsage != null) {
                            getCurrentMemoryUsage()
                        } else {
                            null
                        },
                        // Reset trim level on success - context is healthy
                        contextTrimLevel = 0,
                        pendingRetryQuery = null,
                        pendingRetryFromVoice = false,
                    )
                }
                appDB.updateChat(updatedChat)
                // Speak any remaining buffered text
                if (_uiState.value.ttsEnabled) {
                    ttsManager.speakRemainingBuffer()
                } else {
                    // If TTS is disabled, check context usage now
                    checkContextUsage()
                }
            },
            onCancelled = {
                // ignore CancellationException, as it was called because
                // `responseGenerationJob` was cancelled in the `stopGeneration` method
            },
            onError = { exception ->
                _uiState.update { it.copy(isGeneratingResponse = false) }

                // Check if this is a context overflow error in pocket mode
                val isContextError = exception.message?.contains("context", ignoreCase = true) == true
                if (isContextError && _uiState.value.isPocketModeEnabled) {
                    val currentLevel = _uiState.value.contextTrimLevel
                    val nextLevel = (currentLevel + 1).coerceAtMost(3)
                    LOGD("Context error in pocket mode. Current trim level: $currentLevel, next: $nextLevel")

                    if (nextLevel <= 3 && currentLevel < 3) {
                        // Store the query for retry after trim
                        _uiState.update {
                            it.copy(
                                contextTrimLevel = nextLevel,
                                pendingRetryQuery = query,
                                pendingRetryFromVoice = fromVoice
                            )
                        }

                        // Announce the trimming via TTS
                        val message = if (nextLevel == 3) {
                            "Context full. Clearing most of the conversation to continue."
                        } else {
                            context.getString(R.string.context_auto_trim_voice)
                        }
                        ttsManager.speakChunk(message)
                        ttsManager.speakRemainingBuffer()

                        // Trim with the new level - this calls loadModel which will trigger retry
                        trimOldMessages(nextLevel)

                        // Reset context consumed
                        val updatedChat = _uiState.value.chat.copy(contextSizeConsumed = 0)
                        _uiState.update { it.copy(chat = updatedChat) }
                        appDB.updateChat(updatedChat)

                        LOGD("Trimmed at level $nextLevel, will retry after model reload")
                    } else {
                        // Already at max trim level, give up
                        LOGD("Already at max trim level, cannot recover")
                        _uiState.update { it.copy(contextTrimLevel = 0, pendingRetryQuery = null) }
                        ttsManager.speakChunk("Unable to process. Context too limited.")
                        ttsManager.speakRemainingBuffer()
                    }
                } else {
                    // Non-context error or not in pocket mode: show dialog
                    createAlertDialog(
                        dialogTitle = "An error occurred",
                        dialogText =
                            "The app is unable to process the query. The error message is: ${exception.message}",
                        dialogPositiveButtonText = "Change model",
                        onPositiveButtonClick = {},
                        dialogNegativeButtonText = "",
                        onNegativeButtonClick = {},
                    )
                }
            },
        )
    }

    private fun stopGeneration() {
        smolLMManager.stopResponseGeneration()
        ttsManager.stop()
        _uiState.update { it.copy(isGeneratingResponse = false, renderedPartialResponse = null) }
    }

    private fun switchChat(chat: Chat) {
        stopGeneration()
        _uiState.update { it.copy(chat = chat) }
        loadModel()
    }

    private fun deleteChat(chat: Chat) {
        stopGeneration()
        appDB.deleteChat(chat)
        appDB.deleteMessages(chat.id)
        switchChat(appDB.loadDefaultChat())
    }

    private fun deleteChatMessages(chat: Chat) {
        stopGeneration()
        appDB.deleteMessages(chat.id)
    }

    private fun deleteModel(modelId: Long) {
        modelsRepository.deleteModel(modelId)
        val newChat = _uiState.value.chat.copy(llmModelId = -1)
        _uiState.update { it.copy(chat = newChat) }
    }

    private fun toggleMicRecording() {
        when (_uiState.value.sttState) {
            is STTState.Idle -> {
                // Check if Whisper model is available
                if (!sttManager.isModelAvailable()) {
                    createAlertDialog(
                        dialogTitle = context.getString(R.string.stt_model_not_found_title),
                        dialogText = context.getString(R.string.stt_model_not_found_message),
                        dialogPositiveButtonText = context.getString(R.string.stt_download_model),
                        onPositiveButtonClick = {
                            Intent(context, DownloadWhisperModelActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(this)
                            }
                        },
                        dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                        onNegativeButtonClick = {},
                    )
                    return
                }

                // Check recording permission - request it if not granted
                if (!sttManager.hasRecordingPermission()) {
                    _uiState.update { it.copy(requestRecordingPermission = true) }
                    return
                }

                // Check notification permission on Android 13+ (required for foreground service notification)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasNotificationPermission) {
                        _uiState.update { it.copy(requestNotificationPermission = true) }
                        return
                    }
                }

                // All permissions granted, start voice mode
                startVoiceModeAfterPermissions()
            }

            is STTState.Recording -> {
                // Stop streaming recording - final transcription will be emitted via Flow
                sttManager.stopStreamingRecording(language = _uiState.value.sttLanguage) { /* final result handled via Flow */ }
            }

            is STTState.Transcribing -> {
                // Already transcribing, do nothing
            }

            is STTState.Error -> {
                // Reset to idle state
                _uiState.update { it.copy(sttState = STTState.Idle) }
            }
        }
    }

    /**
     * Starts voice mode after all permissions have been granted.
     */
    private fun startVoiceModeAfterPermissions() {
        // Request battery optimization exemption (required for Samsung and other OEMs)
        // This shows a dialog to the user on first use
        if (!VoiceChatService.isIgnoringBatteryOptimizations(context)) {
            LOGD(">>> Requesting battery optimization exemption")
            VoiceChatService.requestBatteryOptimizationExemption(context)
        }

        // Start foreground service for locked screen support (if not already running)
        if (!voiceChatServiceManager.isServiceRunning.value) {
            LOGD(">>> Starting VoiceChatService")
            VoiceChatService.start(context)
        }
        _uiState.update { it.copy(isVoiceModeActive = true) }

        // Load model if not loaded, then start streaming recording
        sttManager.loadModel { success ->
            if (success) {
                sttManager.startStreamingRecording(
                    language = _uiState.value.sttLanguage,
                    autoSubmitDelayMs = _uiState.value.autoSubmitDelayMs
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.stt_model_load_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startRecordingAfterPermission() {
        // Check again if model is available (user might have navigated away)
        if (!sttManager.isModelAvailable()) {
            return
        }

        // Check notification permission on Android 13+ before starting service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                _uiState.update { it.copy(requestNotificationPermission = true) }
                return
            }
        }

        // All permissions granted, start voice mode
        startVoiceModeAfterPermissions()
    }

    /**
     * Starts recording for voice conversation mode (after TTS finishes).
     * This assumes the Whisper model is already loaded and permission is granted.
     */
    private fun startRecordingForVoiceConversation() {
        if (!sttManager.isModelAvailable()) {
            LOGD("Cannot resume recording: Whisper model not available")
            return
        }

        if (!sttManager.hasRecordingPermission()) {
            LOGD("Cannot resume recording: no recording permission")
            return
        }

        // Model should already be loaded, but load just in case
        sttManager.loadModel { success ->
            if (success) {
                LOGD("Starting streaming recording for voice conversation")
                sttManager.startStreamingRecording(
                    language = _uiState.value.sttLanguage,
                    autoSubmitDelayMs = _uiState.value.autoSubmitDelayMs
                )
            } else {
                LOGD("Failed to load Whisper model for voice conversation")
            }
        }
    }

    fun consumePendingTranscribedText(): String? {
        val text = _uiState.value.pendingTranscribedText
        _uiState.update { it.copy(pendingTranscribedText = null) }
        return text
    }

    fun resetAutoSubmitTrigger() {
        _uiState.update { it.copy(triggerAutoSubmit = false) }
    }

    fun resetClearInputFlag() {
        _uiState.update { it.copy(shouldClearInput = false) }
    }

    /**
     * Stops voice mode completely - stops recording, TTS, and the foreground service.
     */
    fun stopVoiceMode() {
        LOGD("Stopping voice mode")
        sttManager.cancelRecording()
        ttsManager.stop()
        VoiceChatService.stop(context)
        _uiState.update {
            it.copy(
                isVoiceModeActive = false,
                lastInputWasVoice = false,
                sttState = STTState.Idle
            )
        }
    }

    /**
     * Trim old messages to free up context space.
     * Keeps the system prompt and the most recent messages.
     *
     * @param level Trim aggressiveness: 1=light (remove 2), 2=medium (remove 4), 3=aggressive (keep only 2)
     */
    private fun trimOldMessages(level: Int = 1) {
        val chatId = _uiState.value.chat.id
        val messages = appDB.getMessagesForModel(chatId)

        val messagesToKeep = when (level) {
            1 -> messages.size - 2  // Light: remove 2 oldest messages
            2 -> messages.size - 4  // Medium: remove 4 oldest messages
            else -> 2               // Aggressive: keep only last 2 messages (1 exchange)
        }.coerceAtLeast(2)          // Always keep at least 2 messages

        LOGD("Trimming context at level $level: keeping $messagesToKeep of ${messages.size} messages")

        if (messages.size > messagesToKeep) {
            val messagesToDelete = messages.dropLast(messagesToKeep)
            messagesToDelete.forEach { message ->
                appDB.deleteMessage(message.id)
            }
            LOGD("Trimmed ${messagesToDelete.size} old messages")

            // Reload model to apply the trimmed context
            loadModel()
        }
    }

    /**
     * Check if context usage is high and show warning if needed.
     * Called after each response is generated.
     * In pocket mode or with auto-trim enabled, automatically trims old messages.
     */
    private fun checkContextUsage() {
        val chat = _uiState.value.chat
        val contextUsed = chat.contextSizeConsumed
        val contextMax = chat.contextSize

        if (contextMax > 0) {
            val usagePercent = (contextUsed.toFloat() / contextMax.toFloat()) * 100
            LOGD("Context usage: $contextUsed / $contextMax (${usagePercent.toInt()}%)")

            // Check at 75% usage if not already handled for this chat
            if (usagePercent >= 75 && !_uiState.value.contextWarningShownForThisChat) {
                val shouldAutoTrim = _uiState.value.isPocketModeEnabled || _uiState.value.autoContextTrimEnabled

                if (shouldAutoTrim) {
                    // Auto-trim mode: trim and notify via voice if TTS enabled
                    LOGD("Auto-trim: trimming context (pocket=${_uiState.value.isPocketModeEnabled}, autoTrim=${_uiState.value.autoContextTrimEnabled})")
                    if (_uiState.value.ttsEnabled) {
                        val message = context.getString(R.string.context_auto_trim_voice)
                        ttsManager.speakChunk(message)
                        ttsManager.speakRemainingBuffer()
                    }
                    trimOldMessages()
                    // Reset context consumed to prevent immediate re-triggering
                    // The actual value will be updated on next response
                    val updatedChat = _uiState.value.chat.copy(contextSizeConsumed = 0)
                    _uiState.update { it.copy(chat = updatedChat) }
                    appDB.updateChat(updatedChat)
                    LOGD("Context consumed reset to 0 after trim")
                } else {
                    // Normal mode: show dialog
                    _uiState.update { it.copy(showContextWarningDialog = true) }
                }
            }
        }
    }

    /**
     * Get the current memory usage of the device. This method returns the memory consumed (in GBs)
     * and the total memory available on the device (in GBs)
     */
    private fun getCurrentMemoryUsage(): Pair<Float, Float> {
        val memoryInfo = MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalMemory = (memoryInfo.totalMem) / 1024.0.pow(3.0)
        val usedMemory = (memoryInfo.availMem) / 1024.0.pow(3.0)
        return Pair(usedMemory.toFloat(), totalMemory.toFloat())
    }
}
