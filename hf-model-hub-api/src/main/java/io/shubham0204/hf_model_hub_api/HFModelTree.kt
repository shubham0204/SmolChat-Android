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
import kotlinx.serialization.Serializable

class HFModelTree(
    private val client: HttpClient,
) {
    @Serializable
    data class HFModelFile(
        val type: String,
        val oid: String,
        val size: Long,
        val path: String,
    )

    suspend fun getModelFileTree(modelId: String): List<HFModelFile> {
        val response = client.get(urlString = HFEndpoints.getHFModelTreeEndpoint(modelId))
        if (response.status.value != 200) {
            throw Exception("Invalid model ID")
        }
        return response.body()
    }
}
