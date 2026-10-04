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

package io.shubham0204.smollmandroid.llm

import android.content.Context
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ASRModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import java.io.File

import io.shubham0204.smollm.GGUFReader
import io.shubham0204.smollm.SmolLM

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Single
class ModelsRepository(private val context: Context, private val appDB: AppDB) {
    init {
        val registeredPaths = mutableSetOf<String>()
        // 1. Delete DB entries if file doesn't exist
        for (model in appDB.getModelsList()) {
            if (!File(model.path).exists()) {
                appDB.deleteModel(model.id)
            } else {
                registeredPaths.add(model.path)
            }
        }
        
        // 2. Auto-register orphaned .gguf files (e.g. from background downloads)
        CoroutineScope(Dispatchers.IO).launch {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".gguf") && !registeredPaths.contains(file.absolutePath)) {
                    try {
                        val ggufReader = GGUFReader()
                        ggufReader.load(file.absolutePath)
                        val contextSize = ggufReader.getContextSize() ?: SmolLM.DefaultInferenceParams.contextSize
                        val chatTemplate = ggufReader.getChatTemplate() ?: SmolLM.DefaultInferenceParams.chatTemplate
                        appDB.addModel(
                            file.name,
                            "",
                            file.absolutePath,
                            contextSize.toInt(),
                            chatTemplate
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        fun checkIfModelsDownloaded(context: Context): Boolean {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".gguf")) {
                    return true
                }
            }
            return false
        }
    }

    fun getModelFromId(id: Long): LLMModel = appDB.getModel(id)

    fun getAvailableModels(): Flow<List<LLMModel>> = appDB.getModels()

    fun getAvailableModelsList(): List<LLMModel> = appDB.getModelsList()

    fun deleteModel(id: Long) {
        appDB.getModel(id).also {
            File(it.path).delete()
            appDB.deleteModel(it.id)
        }
    }

    fun isSpeech2TextModelDownloaded(asrModel: ASRModel): Boolean {
        return File(context.filesDir, asrModel.name).exists()
    }
}
