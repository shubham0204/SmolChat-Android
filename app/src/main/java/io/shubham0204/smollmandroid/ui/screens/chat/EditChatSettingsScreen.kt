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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import kotlinx.serialization.Serializable

@Serializable
data class EditableChatSettings(
    val name: String,
    val systemPrompt: String,
    val minP: Float,
    val temperature: Float,
    val contextSize: Int,
    val nThreads: Int,
    val chatTemplate: String,
    val useMmap: Boolean,
    val useMlock: Boolean,
    val isTask: Boolean,
) {
    companion object {
        fun fromChat(chat: Chat): EditableChatSettings {
            return EditableChatSettings(
                name = chat.name,
                systemPrompt = chat.systemPrompt,
                minP = chat.minP,
                temperature = chat.temperature,
                contextSize = chat.contextSize,
                nThreads = chat.nThreads,
                chatTemplate = chat.chatTemplate,
                useMmap = chat.useMmap,
                useMlock = chat.useMlock,
                isTask = chat.isTask,
            )
        }
    }

    fun toChat(existingChat: Chat): Chat {
        existingChat.name = name
        existingChat.systemPrompt = systemPrompt
        existingChat.minP = minP
        existingChat.temperature = temperature
        existingChat.contextSize = contextSize
        existingChat.nThreads = nThreads
        existingChat.chatTemplate = chatTemplate
        existingChat.useMmap = useMmap
        existingChat.useMlock = useMlock
        return existingChat
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChatSettingsScreen(
    settings: EditableChatSettings,
    llmModelContextSize: Int,
    onUpdateChat: (EditableChatSettings) -> Unit,
    onBackClicked: () -> Unit,
) {
    var chatName by remember { mutableStateOf(settings.name) }
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }
    var minP by remember { mutableFloatStateOf(settings.minP) }
    var temperature by remember { mutableFloatStateOf(settings.temperature) }
    var contextSize by remember { mutableIntStateOf(settings.contextSize) }
    var nThreads by remember { mutableIntStateOf(settings.nThreads) }
    var takeContextSizeFromModel by remember { mutableStateOf(false) }
    var chatTemplate by remember { mutableStateOf(settings.chatTemplate) }
    var useMmap by remember { mutableStateOf(settings.useMmap) }
    var useMlock by remember { mutableStateOf(settings.useMlock) }
    val context = LocalContext.current
    val totalThreads = Runtime.getRuntime().availableProcessors()

    SmolLMAndroidTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { AppBarTitleText(stringResource(R.string.edit_chat_screen_title)) },
                    navigationIcon = {
                        IconButton(onClick = { onBackClicked() }) {
                            Icon(FeatherIcons.ArrowLeft, contentDescription = "Navigate Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val updatedChat =
                                    settings.copy(
                                        name = chatName,
                                        systemPrompt = systemPrompt,
                                        minP = minP,
                                        temperature = temperature,
                                        contextSize = contextSize,
                                        chatTemplate = chatTemplate,
                                        nThreads = nThreads,
                                        useMmap = useMmap,
                                        useMlock = useMlock,
                                    )
                                if (settings != updatedChat) {
                                    onUpdateChat(updatedChat)
                                    // viewModel.updateChat()
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.edit_chat_new_settings_applied
                                        ),
                                        Toast.LENGTH_LONG,
                                    )
                                        .show()
                                }
                                onBackClicked()
                            }
                        ) {
                            Icon(FeatherIcons.Check, contentDescription = "Save settings")
                        }
                    },
                )
            }
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text(stringResource(R.string.chat_settings_label_chat_name)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(stringResource(R.string.chat_settings_label_sys_prompt)) },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                    maxLines = 5,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = chatTemplate,
                    onValueChange = { chatTemplate = it },
                    label = { Text("Chat template") },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                    maxLines = 5,
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (settings.isTask) {
                    Text(
                        text = stringResource(R.string.chat_settings_desc_task_update),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(R.string.chat_settings_label_minp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.chat_settings_desc_minp),
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = minP,
                    onValueChange = { minP = it },
                    valueRange = 0.0f..1.0f,
                    steps = 100,
                )
                Text(text = "%.2f".format(minP), style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(R.string.chat_settings_label_temp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.chat_settings_desc_temp),
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..5.0f,
                    steps = 50,
                )
                Text(text = "%.1f".format(temperature), style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(R.string.chat_settings_label_ctx_size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.chat_settings_desc_ctx_length),
                    style = MaterialTheme.typography.labelSmall,
                )
                TextField(
                    enabled = !takeContextSizeFromModel,
                    modifier = Modifier.fillMaxWidth(),
                    value =
                        if (takeContextSizeFromModel) {
                            contextSize = llmModelContextSize
                            contextSize.toString()
                        } else {
                            contextSize.toString()
                        },
                    onValueChange = {
                        contextSize =
                            if (it.isNotEmpty()) {
                                it.toInt()
                            } else {
                                0
                            }
                    },
                    isError = contextSize == 0,
                    label = {
                        if (contextSize == 0) {
                            Text(stringResource(R.string.chat_settings_err_min_ctx_size))
                        } else {
                            if (takeContextSizeFromModel) {
                                Text(stringResource(R.string.context_size_taken_from_model))
                            } else {
                                Text(stringResource(R.string.chat_settings_title_num_tokens))
                            }
                        }
                    },
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = takeContextSizeFromModel,
                        onCheckedChange = { takeContextSizeFromModel = it },
                    )
                    Text(
                        text = stringResource(R.string.chat_settings_take_from_gguf),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Number of Threads", style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.chat_settings_desc_n_threads),
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = nThreads.toFloat(),
                    onValueChange = { nThreads = it.toInt() },
                    valueRange = 0.0f..(totalThreads).toFloat(),
                    steps = totalThreads,
                )
                Text(text = "$nThreads", style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = useMmap, onCheckedChange = { useMmap = it })
                    Column {
                        Text(text = "Use mmap", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text =
                                "Disable memory mapping (mmap) for potentially fewer pageouts and better performance on low-memory systems, but with slower load times and a risk of preventing loading large models.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = useMlock, onCheckedChange = { useMlock = it })
                    Column {
                        Text(text = "Use mlock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text =
                                "Keep the model loaded in RAM for faster performance, but uses more memory and may load slower.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
