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

package io.shubham0204.hf_model_hub_api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

class HFModelInfo(
    private val client: HttpClient,
) {
    @Serializable
    data class ModelInfo(
        val _id: String,
        val id: String,
        val modelId: String,
        val author: String,
        val private: Boolean,
        val disabled: Boolean,
        val tags: List<String>,
        @SerialName(value = "downloads") val numDownloads: Long,
        @SerialName(value = "likes") val numLikes: Long,
        @Serializable(with = CustomDateSerializer::class) val lastModified: LocalDateTime,
        @Serializable(with = CustomDateSerializer::class) val createdAt: LocalDateTime,
    )

    suspend fun getModelInfo(modelId: String): ModelInfo {
        val response = client.get(urlString = HFEndpoints.getHFModelSpecsEndpoint(modelId))
        if (response.status.value != 200) {
            throw Exception("Invalid model ID")
        }
        return response.body()
    }
}
