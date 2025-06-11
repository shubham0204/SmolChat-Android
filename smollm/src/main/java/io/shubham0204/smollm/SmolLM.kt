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
                cpuFeatures.contains("asimd") &&
                    cpuFeatures.contains("crc32") &&
                    cpuFeatures.contains(
                        "aes",
                    )
            val isAtLeastArmV84 = cpuFeatures.contains("dcpop") && cpuFeatures.contains("uscat")

            Log.d(logTag, "CPU features: $cpuFeatures")
            Log.d(logTag, "- hasFp16: $hasFp16")
            Log.d(logTag, "- hasDotProd: $hasDotProd")
            Log.d(logTag, "- hasSve: $hasSve")
            Log.d(logTag, "- hasI8mm: $hasI8mm")
            Log.d(logTag, "- isAtLeastArmV82: $isAtLeastArmV82")
            Log.d(logTag, "- isAtLeastArmV84: $isAtLeastArmV84")

            // Check if the app is running in an emulated device
            // Note, this is not the OFFICIAL way to check if the app is running
            // on an emulator
            val isEmulated =
                (Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu"))
            Log.d(logTag, "isEmulated: $isEmulated")

            if (!isEmulated) {
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
                } else if (Build.SUPPORTED_32_BIT_ABIS[0]?.equals("armeabi-v7a") == true) {
                    // armv7a (32bit) device
                    Log.d(logTag, "Loading libsmollm_v7a.so")
                    System.loadLibrary("smollm_v7a")
                } else {
                    Log.d(logTag, "Loading default libsmollm.so")
                    System.loadLibrary("smollm")
                }
            } else {
                // load the default native library with no ARM
                // specific instructions
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

    /**
     * Provides default values for inference parameters.
     * These values are used when the corresponding parameters are not provided
     * by the user or are not available in the GGUF model file.
     */
    object DefaultInferenceParams {
        val contextSize: Long = 1024L
        val chatTemplate: String =
            "{% for message in messages %}{% if loop.first and messages[0]['role'] != 'system' %}{{ '<|im_start|>system You are a helpful AI assistant named SmolLM, trained by Hugging Face<|im_end|> ' }}{% endif %}{{'<|im_start|>' + message['role'] + ' ' + message['content'] + '<|im_end|>' + ' '}}{% endfor %}{% if add_generation_prompt %}{{ '<|im_start|>assistant ' }}{% endif %}"
    }

    /**
     * Data class to hold the inference parameters for the LLM.
     *
     * @property minP The minimum probability for a token to be considered.
     *                Also known as top-P sampling. (Default: 0.01f)
     * @property temperature The temperature for sampling. Higher values make the output more random.
     *                       (Default: 1.0f)
     * @property storeChats Whether to store the chat history in memory. If true, the LLM will
     *                      remember previous interactions in the current session. (Default: true)
     * @property contextSize The context size (in tokens) for the LLM. This determines how much
     *                       of the previous conversation the LLM can "remember". If null, the
     *                       value from the GGUF model file will be used, or a default value if
     *                       not present in the model file. (Default: null)
     * @property chatTemplate The chat template to use for formatting the conversation. This
     *                        is a Jinja2 template string. If null, the value from the GGUF
     *                        model file will be used, or a default value if not present in the
     *                        model file. (Default: null)
     * @property numThreads The number of threads to use for inference. (Default: 4)
     * @property useMmap Whether to use memory-mapped file I/O for loading the model.
     *                   This can improve loading times and reduce memory usage. (Default: true)
     * @property useMlock Whether to lock the model in memory. This can prevent the model from
     *                    being swapped out to disk, potentially improving performance. (Default: false)
     */
    data class InferenceParams(
        val minP: Float = 0.01f,
        val temperature: Float = 1.0f,
        val storeChats: Boolean = true,
        val contextSize: Long? = null,
        val chatTemplate: String? = null,
        val numThreads: Int = 4,
        val useMmap: Boolean = true,
        val useMlock: Boolean = false,
    )

    /**
     * Loads the GGUF model from the given path.
     * This function will read the metadata from the GGUF model file,
     * such as the context size and chat template, and use them if they are not
     * explicitly provided in the `params`.
     *
     * @param modelPath The path to the GGUF model file.
     * @param params The inference parameters to use. If not provided, default values will be used.
     *               If `contextSize` or `chatTemplate` are not provided in `params`,
     *               the values from the GGUF model file will be used. If those are also
     *               not available in the model file, then default values from [DefaultInferenceParams]
     *               will be used.
     * @return `true` if the model was loaded successfully, `false` otherwise.
     * @throws FileNotFoundException if the model file is not found at the given path.
     */
    suspend fun load(
        modelPath: String,
        params: InferenceParams = InferenceParams(),
    ) = withContext(Dispatchers.IO) {
        val ggufReader = GGUFReader()
        ggufReader.load(modelPath)
        val modelContextSize = ggufReader.getContextSize() ?: DefaultInferenceParams.contextSize
        val modelChatTemplate =
            ggufReader.getChatTemplate() ?: DefaultInferenceParams.chatTemplate
        nativePtr =
            loadModel(
                modelPath,
                params.minP,
                params.temperature,
                params.storeChats,
                params.contextSize ?: modelContextSize,
                params.chatTemplate ?: modelChatTemplate,
                params.numThreads,
                params.useMmap,
                params.useMlock,
            )
    }

    /**
     * Adds a user message to the chat history.
     * This message will be considered as part of the conversation
     * when generating the next response.
     *
     * @param message The user's message.
     * @throws IllegalStateException if the model is not loaded.
     */
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
     * async Flow. This is useful for streaming the response
     * as it is generated by the LLM.
     *
     * @param query The query to ask the LLM.
     * @return A Flow of Strings, where each String is a piece of the response.
     *         The flow completes when the LLM has finished generating the response.
     *         The special token "[EOG]" (End Of Generation) indicates the end of the response.
     * @throws IllegalStateException if the model is not loaded.
     */
    fun getResponseAsFlow(query: String): Flow<String> =
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

    /**
     * Returns the LLM response to the given query as a String.
     * This function is blocking and will return the complete response.
     *
     * @param query The user's query/prompt for the LLM.
     * @return The complete response from the LLM.
     * @throws IllegalStateException if the model is not loaded.
     */
    fun getResponse(query: String): String {
        verifyHandle()
        startCompletion(nativePtr, query)
        var piece = completionLoop(nativePtr)
        var response = ""
        while (piece != "[EOG]") {
            response += piece
            piece = completionLoop(nativePtr)
        }
        stopCompletion(nativePtr)
        return response
    }

    /**
     * Unloads the LLM model and releases resources.
     * This method should be called when the SmolLM instance is no longer needed
     * to prevent memory leaks.
     */
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
