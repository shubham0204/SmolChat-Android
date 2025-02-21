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

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * This class interacts with the JNI binding and provides a Kotlin API
 * to infer a GGUF LLM model
 */
class SmolLM {
    companion object {
        init {
            val logTag = SmolLM::class.java.simpleName

            // check if the following CPU features are available,
            // and load the native library accordingly
            val cpuFeatures = getCPUFeatures()
            val hasFp16 = cpuFeatures.contains("fp16") || cpuFeatures.contains("fphp")
            val hasDotProd = cpuFeatures.contains("dotprod") || cpuFeatures.contains("asimddp")
            val hasSve = cpuFeatures.contains("sve")
            val hasI8mm = cpuFeatures.contains("i8mm")
            val isAtLeastArmV82 =
                cpuFeatures.contains("asimd") && cpuFeatures.contains("crc32") && cpuFeatures.contains("aes")
            val isAtLeastArmV84 = cpuFeatures.contains("dcpop") && cpuFeatures.contains("uscat")

            Log.d(logTag, "CPU features: $cpuFeatures")
            Log.d(logTag, "- hasFp16: $hasFp16")
            Log.d(logTag, "- hasDotProd: $hasDotProd")
            Log.d(logTag, "- hasSve: $hasSve")
            Log.d(logTag, "- hasI8mm: $hasI8mm")
            Log.d(logTag, "- isAtLeastArmV82: $isAtLeastArmV82")
            Log.d(logTag, "- isAtLeastArmV84: $isAtLeastArmV84")

            if (supportsArm64V8a()) {
                if (isAtLeastArmV84 && hasSve && hasI8mm && hasFp16 && hasDotProd) {
                    Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod_i8mm_sve.so")
                    System.loadLibrary("smollm_v8_4_fp16_dotprod_i8mm_sve")
                } else if (isAtLeastArmV84 && hasSve && hasFp16 && hasDotProd) {
                    Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod_sve.so")
                    System.loadLibrary("smollm_v8_4_fp16_dotprod_sve")
                } else if (isAtLeastArmV84 && hasI8mm && hasFp16 && hasDotProd) {
                    Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod_i8mm.so")
                    System.loadLibrary("smollm_v8_4_fp16_dotprod_i8mm")
                } else if (isAtLeastArmV84 && hasFp16 && hasDotProd) {
                    Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod.so")
                    System.loadLibrary("smollm_v8_4_fp16_dotprod")
                } else if (isAtLeastArmV82 && hasFp16 && hasDotProd) {
                    Log.d(logTag, "Loading libsmollm_v8_2_fp16_dotprod.so")
                    System.loadLibrary("smollm_v8_2_fp16_dotprod")
                } else if (isAtLeastArmV82 && hasFp16) {
                    Log.d(logTag, "Loading libsmollm_v8_2_fp16.so")
                    System.loadLibrary("smollm_v8_2_fp16")
                } else {
                    Log.d(logTag, "Loading libsmollm_v8.so")
                    System.loadLibrary("smollm_v8")
                }
            } else {
                Log.d(logTag, "Loading default libsmollm.so")
                System.loadLibrary("smollm")
            }
        }

        /**
         * Reads the /proc/cpuinfo file and returns the line
         * starting with 'Features :' that containing the available
         * CPU features
         */
        private fun getCPUFeatures(): String {
            val cpuInfo =
                try {
                    File("/proc/cpuinfo").readText()
                } catch (e: FileNotFoundException) {
                    ""
                }
            val cpuFeatures =
                cpuInfo
                    .substringAfter("Features")
                    .substringAfter(":")
                    .substringBefore("\n")
                    .trim()
            return cpuFeatures
        }

        private fun supportsArm64V8a(): Boolean = Build.SUPPORTED_ABIS[0].equals("arm64-v8a")
    }

    private var nativePtr = 0L

    suspend fun create(
        modelPath: String,
        minP: Float,
        temperature: Float,
        storeChats: Boolean,
        contextSize: Long,
        chatTemplate: String,
        nThreads: Int,
        useMmap: Boolean,
        useMlock: Boolean,
    ): Boolean =
        withContext(Dispatchers.IO) {
            nativePtr =
                loadModel(
                    modelPath,
                    minP,
                    temperature,
                    storeChats,
                    contextSize,
                    chatTemplate,
                    nThreads,
                    useMmap,
                    useMlock,
                )
            return@withContext nativePtr != 0L
        }

    fun addUserMessage(message: String) {
        verifyHandle()
        addChatMessage(nativePtr, message, "user")
    }

    /**
     * Adds the system prompt for the LLM
     */
    fun addSystemPrompt(prompt: String) {
        verifyHandle()
        addChatMessage(nativePtr, prompt, "system")
    }

    /**
     * Adds the assistant message for LLM inference
     * An assistant message is the response given by the LLM
     * for a previous query in the conversation
     */
    fun addAssistantMessage(message: String) {
        verifyHandle()
        addChatMessage(nativePtr, message, "assistant")
    }

    /**
     * Returns the rate (in tokens per second) at which the
     * LLM generated its last response via `getResponse()`
     */
    fun getResponseGenerationSpeed(): Float {
        verifyHandle()
        return getResponseGenerationSpeed(nativePtr)
    }

    /**
     * Returns the number of tokens consumed by the LLM's context
     * window
     * The context of the LLM is roughly the output of,
     * tokenize(apply_chat_template(messages_in_conversation))
     */
    fun getContextLengthUsed(): Int {
        verifyHandle()
        return getContextSizeUsed(nativePtr)
    }

    /**
     * Return the LLM response to the given query as an
     * async Flow
     */
    fun getResponse(query: String): Flow<String> =
        flow {
            verifyHandle()
            startCompletion(nativePtr, query)
            var piece = completionLoop(nativePtr)
            while (piece != "[EOG]") {
                emit(piece)
                piece = completionLoop(nativePtr)
            }
            stopCompletion(nativePtr)
        }

    fun close() {
        if (nativePtr != 0L) {
            close(nativePtr)
            nativePtr = 0L
        }
    }

    private fun verifyHandle() {
        assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
    }

    private external fun loadModel(
        modelPath: String,
        minP: Float,
        temperature: Float,
        storeChats: Boolean,
        contextSize: Long,
        chatTemplate: String,
        nThreads: Int,
        useMmap: Boolean,
        useMlock: Boolean,
    ): Long

    private external fun addChatMessage(
        modelPtr: Long,
        message: String,
        role: String,
    )

    private external fun getResponseGenerationSpeed(modelPtr: Long): Float

    private external fun getContextSizeUsed(modelPtr: Long): Int

    private external fun close(modelPtr: Long)

    private external fun startCompletion(
        modelPtr: Long,
        prompt: String,
    )

    private external fun completionLoop(modelPtr: Long): String

    private external fun stopCompletion(modelPtr: Long)
}
