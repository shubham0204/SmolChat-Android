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

package io.shubham0204.smollmandroid.server

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String = "",
    val messages: List<ChatMessagePayload> = emptyList(),
    val temperature: Float? = null,
    @SerialName("min_p") val minP: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
)

@Serializable
data class ChatMessagePayload(
    val role: String,
    val content: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatCompletionResponse(
    val id: String,
    @EncodeDefault @SerialName("object") val objectType: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<ChatCompletionChoice>,
    val usage: CompletionUsage,
)

@Serializable
data class ChatCompletionChoice(
    val index: Int = 0,
    val message: ChatMessagePayload,
    @SerialName("finish_reason") val finishReason: String? = "stop",
)

@Serializable
data class CompletionUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatCompletionChunk(
    val id: String,
    @EncodeDefault @SerialName("object") val objectType: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<ChatCompletionChunkChoice>,
)

@Serializable
data class ChatCompletionChunkChoice(
    val index: Int = 0,
    val delta: ChatDeltaPayload,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class ChatDeltaPayload(
    val role: String? = null,
    val content: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ModelListResponse(
    @EncodeDefault @SerialName("object") val objectType: String = "list",
    val data: List<ModelObject>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ModelObject(
    val id: String,
    @EncodeDefault @SerialName("object") val objectType: String = "model",
    val created: Long = 0L,
    @EncodeDefault @SerialName("owned_by") val ownedBy: String = "smolchat-android",
)

@Serializable
data class OpenAIErrorResponse(
    val error: OpenAIErrorDetail,
)

@Serializable
data class OpenAIErrorDetail(
    val message: String,
    val type: String = "invalid_request_error",
    val code: String? = null,
)

@Serializable
data class HealthStatusResponse(
    val status: String,
    @SerialName("loaded_model") val loadedModel: String? = null,
)

const val OPENAPI_SPEC_JSON = """
{
  "openapi": "3.1.0",
  "info": {
    "title": "SmolChat Android Local LLM API",
    "description": "OpenAI-compatible & OpenAPI-documented local inference server running on-device via SmolChat Android (llama.cpp).",
    "version": "1.0.0"
  },
  "paths": {
    "/health": {
      "get": {
        "operationId": "getHealth",
        "summary": "Check server and model status",
        "responses": {
          "200": {
            "description": "Server health status"
          }
        }
      }
    },
    "/v1/models": {
      "get": {
        "operationId": "listModels",
        "summary": "List downloaded GGUF models available on the device",
        "responses": {
          "200": {
            "description": "List of available models in OpenAI format"
          }
        }
      }
    },
    "/v1/chat/completions": {
      "post": {
        "operationId": "createChatCompletion",
        "summary": "Create a chat completion (supports streaming via SSE and non-streaming JSON)",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "required": ["messages"],
                "properties": {
                  "model": {
                    "type": "string",
                    "description": "Name of the downloaded model to use (defaults to the active server model)."
                  },
                  "messages": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "required": ["role", "content"],
                      "properties": {
                        "role": {
                          "type": "string",
                          "enum": ["system", "user", "assistant"]
                        },
                        "content": {
                          "type": "string"
                        }
                      }
                    }
                  },
                  "stream": {
                    "type": "boolean",
                    "default": false
                  },
                  "max_tokens": {
                    "type": "integer",
                    "description": "Maximum number of tokens to generate."
                  }
                }
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "Chat completion response or SSE token stream"
          }
        }
      }
    }
  }
}
"""
