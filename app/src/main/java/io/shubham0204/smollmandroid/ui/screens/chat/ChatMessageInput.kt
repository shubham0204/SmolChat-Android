package io.shubham0204.smollmandroid.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Mic
import compose.icons.feathericons.MicOff
import compose.icons.feathericons.Send
import compose.icons.feathericons.StopCircle
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.ui.screens.chat.ChatScreenViewModel.ModelLoadingState

@Composable
fun MessageInput(
    currChat: Chat,
    modelLoadingState: ModelLoadingState,
    audioTranscriptionUIState: AudioTranscriptionUIState,
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
                    if (audioTranscriptionUIState.isAvailable) {
                        IconButton(
                            onClick = {
                                if (audioTranscriptionUIState.isRecording) {
                                    onEvent(ChatScreenUIEvent.ChatEvents.StopAudioTranscription)
                                } else {
                                    onEvent(ChatScreenUIEvent.ChatEvents.StartAudioTranscription {
                                        questionText = it
                                    })
                                }
                            }
                        ) {
                            if (audioTranscriptionUIState.isRecording) {
                                Icon(
                                    FeatherIcons.MicOff,
                                    contentDescription = "Stop Audio Transcription"
                                )
                            } else {
                                Icon(
                                    FeatherIcons.Mic,
                                    contentDescription = "Start Audio Transcription"
                                )
                            }
                        }
                    }
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
                        placeholder = {
                            Text(
                                text =
                                    if (audioTranscriptionUIState.isRecording) {
                                        stringResource(R.string.chat_listening)
                                    } else {
                                        stringResource(R.string.chat_ask_question)
                                    }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
