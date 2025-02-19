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

package io.shubham0204.smollmandroid.data.models

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import java.io.File

@Single
class ModelsRepository(
    private val context: Context,
    private val modelsDB: ModelsDB,
) {
    init {
        for (model in modelsDB.getModelsList()) {
            if (!File(model.path).exists()) {
                deleteModel(model.id)
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

    fun getModelFromId(id: Long): LLMModel? = modelsDB.getModel(id)

    fun getAvailableModels(): Flow<List<LLMModel>> = modelsDB.getModels()

    fun getAvailableModelsList(): List<LLMModel> = modelsDB.getModelsList()

    fun deleteModel(id: Long) {
        modelsDB.getModel(id)?.let {
            File(it.path).delete()
            modelsDB.deleteModel(it.id)
        }
    }
}
