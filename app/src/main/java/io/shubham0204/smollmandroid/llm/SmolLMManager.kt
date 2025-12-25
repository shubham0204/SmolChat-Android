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
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.Chat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.measureTime

private const val LOGTAG = "[SmolLMManager-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@Single
class SmolLMManager(private val appDB: AppDB) {
    private val instance = SmolLM()

    // Use ReentrantLock for thread-safe state management without suspending
    private val stateLock = ReentrantLock()

    @Volatile
    private var responseGenerationJob: Job? = null

    @Volatile
    private var modelInitJob: Job? = null

    @Volatile
    private var chat: Chat? = null

    // Use java.util.concurrent.atomic for better thread safety
    private val isInstanceLoaded = AtomicBoolean(false)

    @Volatile
    var isInferenceOn = false
        private set

    data class SmolLMResponse(
        val response: String,
        val generationSpeed: Float,
        val generationTimeSecs: Int,
        val contextLengthUsed: Int,
    )

    fun load(
        chat: Chat,
        modelPath: String,
        params: SmolLM.InferenceParams = SmolLM.InferenceParams(),
        onError: (Exception) -> Unit,
        onSuccess: () -> Unit,
    ) {
        stateLock.withLock {
            // Cancel any existing load operation
            modelInitJob?.cancel()

            try {
                this.chat = chat
                modelInitJob = CoroutineScope(Dispatchers.Default).launch {
                    try {
                        instance.load(modelPath, params)
                        LOGD("Model loaded")

                        if (chat.systemPrompt.isNotEmpty()) {
                            instance.addSystemPrompt(chat.systemPrompt)
                            LOGD("System prompt added")
                        }

                        if (!chat.isTask) {
                            appDB.getMessagesForModel(chat.id).forEach { message ->
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
                            isInstanceLoaded.set(true)
                            onSuccess()
                        }
                    } catch (e: CancellationException) {
                        LOGD("Model loading cancelled")
                        throw e
                    } catch (e: Exception) {
                        LOGD("Error loading model: ${e.message}")
                        withContext(Dispatchers.Main) {
                            onError(e)
                        }
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun unload() {
        stateLock.withLock {
            // Cancel jobs
            responseGenerationJob.safeCancelJobIfActive()
            modelInitJob.safeCancelJobIfActive()

            // Launch a separate coroutine to wait for jobs to complete and clean up
            // This is necessary because join() is suspending, but we can't make unload() suspend
            CoroutineScope(Dispatchers.Default).launch {
                responseGenerationJob?.join()
                modelInitJob?.join()

                try {
                    instance.close()
                } catch (e: Exception) {
                    LOGD("Error closing instance: ${e.message}")
                }
            }

            isInstanceLoaded.set(false)
            chat = null
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
        stateLock.withLock {
            // Check if model is loaded
            if (!isInstanceLoaded.get()) {
                onError(IllegalStateException("Model not loaded"))
                return
            }

            // Cancel any existing response generation
            responseGenerationJob?.cancel()

            responseGenerationJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    isInferenceOn = true
                    var response = ""

                    val duration = measureTime {
                        instance.getResponseAsFlow(query).collect { piece ->
                            response += piece
                            withContext(Dispatchers.Main) {
                                onPartialResponseGenerated(response)
                            }
                        }
                    }

                    response = responseTransform(response)

                    // Thread-safe access to chat
                    val currentChat = stateLock.withLock { chat }

                    if (currentChat != null) {
                        // Add response to database
                        appDB.addAssistantMessage(currentChat.id, response)
                    }

                    withContext(Dispatchers.Main) {
                        isInferenceOn = false
                        onSuccess(
                            SmolLMResponse(
                                response = response,
                                generationSpeed = instance.getResponseGenerationSpeed(),
                                generationTimeSecs = duration.inWholeSeconds.toInt(),
                                contextLengthUsed = instance.getContextLengthUsed(),
                            )
                        )
                    }
                } catch (e: CancellationException) {
                    isInferenceOn = false
                    withContext(Dispatchers.Main) {
                        onCancelled()
                    }
                } catch (e: Exception) {
                    isInferenceOn = false
                    withContext(Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        }
    }

    fun stopResponseGeneration() {
        stateLock.withLock {
            responseGenerationJob.safeCancelJobIfActive()
            isInferenceOn = false
        }
    }

    private fun Job?.safeCancelJobIfActive() {
        this?.cancel()
    }
}