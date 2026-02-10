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

import android.os.Process
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
    val isInstanceLoaded = AtomicBoolean(false)

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

            isInstanceLoaded.set(false)
            chat = null

            // Close the instance synchronously to prevent race conditions
            // with a subsequent load() call. Deferring close() to an async
            // coroutine caused the old close() to free the newly loaded model.
            try {
                instance.close()
            } catch (e: Exception) {
                LOGD("Error closing instance: ${e.message}")
            }
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
                // Boost thread priority to reduce CPU throttling when screen is locked
                // THREAD_PRIORITY_URGENT_AUDIO (-19) is the highest priority available
                // to regular apps and signals to the system this is time-sensitive work
                val originalPriority = Process.getThreadPriority(Process.myTid())
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                    LOGD(">>> Thread priority boosted from $originalPriority to URGENT_AUDIO")
                } catch (e: Exception) {
                    LOGD(">>> Failed to boost thread priority: ${e.message}")
                }

                try {
                    LOGD(">>> getResponse coroutine started on thread: ${Thread.currentThread().name}")
                    isInferenceOn = true
                    var response = ""

                    val duration = measureTime {
                        LOGD(">>> Starting response flow collection...")
                        instance.getResponseAsFlow(query).collect { piece ->
                            response += piece
                            // Don't use Main dispatcher - callbacks are thread-safe
                            // Using Main blocks when screen is locked
                            onPartialResponseGenerated(response)
                        }
                        LOGD(">>> Response flow collection complete")
                    }

                    response = responseTransform(response)
                    LOGD(">>> Response transformed, length=${response.length}")

                    // Thread-safe access to chat
                    val currentChat = stateLock.withLock { chat }

                    if (currentChat != null) {
                        // Add response to database
                        LOGD(">>> Adding assistant message to DB...")
                        appDB.addAssistantMessage(currentChat.id, response)
                        LOGD(">>> Assistant message added")
                    }

                    LOGD(">>> Calling onSuccess callback...")
                    isInferenceOn = false
                    onSuccess(
                        SmolLMResponse(
                            response = response,
                            generationSpeed = instance.getResponseGenerationSpeed(),
                            generationTimeSecs = duration.inWholeSeconds.toInt(),
                            contextLengthUsed = instance.getContextLengthUsed(),
                        )
                    )
                    LOGD(">>> onSuccess callback returned")
                } catch (e: CancellationException) {
                    LOGD(">>> Response generation cancelled")
                    isInferenceOn = false
                    onCancelled()
                } catch (e: Exception) {
                    LOGD(">>> Response generation error: ${e.message}")
                    isInferenceOn = false
                    onError(e)
                } finally {
                    // Restore original thread priority
                    try {
                        Process.setThreadPriority(originalPriority)
                        LOGD(">>> Thread priority restored to $originalPriority")
                    } catch (e: Exception) {
                        LOGD(">>> Failed to restore thread priority: ${e.message}")
                    }
                }
            }
        }
    }

    private val BENCH_PROMPT_PROCESSING_TOKENS = 512
    private val BENCH_TOKEN_GENERATION_TOKENS = 128
    private val BENCH_SEQUENCE = 1
    private val BENCH_REPETITION = 3

    fun benchmark(onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val result = instance.benchModel(
                BENCH_PROMPT_PROCESSING_TOKENS,
                BENCH_TOKEN_GENERATION_TOKENS,
                BENCH_SEQUENCE,
                BENCH_REPETITION
            )
            withContext(Dispatchers.Main) {
                onResult(result)
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