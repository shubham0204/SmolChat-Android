/*
 * Copyright (C) 2025 Shubham Panchal
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

package io.shubham0204.smollmandroid.llm

import android.util.Log
import io.shubham0204.smollm.SmolLM
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.MessagesDB
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.measureTime

private const val LOGTAG = "[SmolLMManager-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@Single
class SmolLMManager(
    private val messagesDB: MessagesDB,
) {
    private val instance = SmolLM()
    private var responseGenerationJob: Job? = null
    private var chat: Chat? = null
    var isInstanceLoaded = false

    data class SmolLMInitParams(
        val chat: Chat,
        val modelPath: String,
        val minP: Float,
        val temperature: Float,
        val storeChats: Boolean,
        val contextSize: Long,
    )

    data class SmolLMResponse(
        val response: String,
        val generationSpeed: Float,
        val generationTimeSecs: Int,
        val contextLengthUsed: Int,
    )

    fun create(
        initParams: SmolLMInitParams,
        onError: (Exception) -> Unit,
        onSuccess: () -> Unit,
    ) {
        try {
            CoroutineScope(Dispatchers.Default).launch {
                chat = initParams.chat
                if (isInstanceLoaded) {
                    close()
                }
                instance.create(
                    initParams.modelPath,
                    initParams.minP,
                    initParams.temperature,
                    initParams.storeChats,
                    initParams.contextSize,
                )
                LOGD("Model loaded")
                if (initParams.chat.systemPrompt.isNotEmpty()) {
                    instance.addSystemPrompt(initParams.chat.systemPrompt)
                    LOGD("System prompt added")
                }
                if (!initParams.chat.isTask) {
                    messagesDB.getMessagesForModel(initParams.chat.id).forEach { message ->
                        if (message.isUserMessage) {
                            instance.addUserMessage(message.message)
                            LOGD("User message added: ${message.message}")
                        } else {
                            instance.addAssistantMessage(message.message)
                            LOGD("Assistant message added: ${message.message}")
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    isInstanceLoaded = true
                    onSuccess()
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun getResponse(
        query: String,
        responseTransform: (String) -> String,
        onPartialResponseGenerated: (String) -> Unit,
        onSuccess: (SmolLMResponse) -> Unit,
        onCancelled: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            assert(chat != null) { "Please call SmolLMManager.create() first." }
            responseGenerationJob =
                CoroutineScope(Dispatchers.Default).launch {
                    var response = ""
                    val duration =
                        measureTime {
                            instance.getResponse(query).collect { piece ->
                                response += responseTransform(piece)
                                withContext(Dispatchers.Main) {
                                    onPartialResponseGenerated(response)
                                }
                            }
                        }
                    // once the response is generated
                    // add it to the messages database
                    messagesDB.addAssistantMessage(chat!!.id, response)
                    withContext(Dispatchers.Main) {
                        onSuccess(
                            SmolLMResponse(
                                response = response,
                                generationSpeed = instance.getResponseGenerationSpeed(),
                                generationTimeSecs = duration.inWholeSeconds.toInt(),
                                contextLengthUsed = instance.getContextLengthUsed(),
                            ),
                        )
                    }
                }
        } catch (e: CancellationException) {
            onCancelled()
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopResponseGeneration() {
        responseGenerationJob?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }

    fun close() {
        instance.close()
        isInstanceLoaded = false
    }
}
