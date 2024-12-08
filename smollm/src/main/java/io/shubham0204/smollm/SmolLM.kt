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

package io.shubham0204.smollm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class SmolLM {
    private var nativePtr = 0L

    companion object {
        init {
            System.loadLibrary("smollm")
        }
    }

    suspend fun create(
        modelPath: String,
        minP: Float,
        temperature: Float,
        storeChats: Boolean,
    ) = withContext(Dispatchers.IO) {
        nativePtr = loadModel(modelPath, minP, temperature, storeChats)
    }

    fun addUserMessage(message: String) {
        assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
        addChatMessage(nativePtr, message, "user")
    }

    fun addSystemPrompt(prompt: String) {
        assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
        addChatMessage(nativePtr, prompt, "system")
    }

    fun addAssistantMessage(message: String) {
        assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
        addChatMessage(nativePtr, message, "assistant")
    }

    fun getResponseGenerationSpeed(): Float {
        assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
        return getResponseGenerationSpeed(nativePtr)
    }

    fun getResponse(query: String): Flow<String> =
        flow {
            assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
            startCompletion(nativePtr, query)
            var piece = completionLoop(nativePtr)
            while (piece != "[EOG]") {
                emit(piece)
                piece = completionLoop(nativePtr)
            }
            stopCompletion(nativePtr)
        }

    fun close() {
        close(nativePtr)
    }

    private external fun loadModel(
        modelPath: String,
        minP: Float,
        temperature: Float,
        storeChats: Boolean,
    ): Long

    private external fun addChatMessage(
        modelPtr: Long,
        message: String,
        role: String,
    )

    private external fun getResponseGenerationSpeed(modelPtr: Long): Float

    private external fun close(modelPtr: Long)

    private external fun startCompletion(
        modelPtr: Long,
        prompt: String,
    )

    private external fun completionLoop(modelPtr: Long): String

    private external fun stopCompletion(modelPtr: Long)
}
