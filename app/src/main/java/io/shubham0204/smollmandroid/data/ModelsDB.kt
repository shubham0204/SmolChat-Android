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

package io.shubham0204.smollmandroid.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Single

@Entity
data class LLMModel(
    @Id var id: Long = 0,
    var name: String = "",
    var url: String = "",
    var path: String = "",
    var contextSize: Int = 0,
)

@Single
class ModelsDB {
    private val modelsBox = ObjectBoxStore.store.boxFor(LLMModel::class.java)

    fun addModel(
        name: String,
        url: String,
        path: String,
        contextSize: Int,
    ) {
        modelsBox.put(LLMModel(name = name, url = url, path = path, contextSize = contextSize))
    }

    fun getModel(id: Long): LLMModel? = modelsBox.get(id)

    fun getModels(): Flow<List<LLMModel>> =
        modelsBox
            .query()
            .build()
            .flow()
            .flowOn(Dispatchers.IO)

    fun getModelsList(): List<LLMModel> = modelsBox.all

    fun deleteModel(id: Long) {
        modelsBox.remove(id)
    }
}
