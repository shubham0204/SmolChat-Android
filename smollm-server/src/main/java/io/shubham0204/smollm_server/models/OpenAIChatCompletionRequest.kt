package io.shubham0204.smollm_server.models

import kotlinx.serialization.Serializable

/**
 * Represents a message in the OpenAI Chat Completion API
 * docs: https://platform.openai.com/docs/api-reference/chat/create#chat-create-messages
 */
@Serializable
data class ChatCompletionMessage(
    val role: String,
    val content: String,
)

/**
 * Represents the request for the OpenAI Chat Completion API
 * docs: https://platform.openai.com/docs/api-reference/chat/create
 */
@Serializable
data class OpenAIChatCompletionRequest(
    val messages: List<ChatCompletionMessage>,
    val model: String,
    val topP: Float = 1.0f,
    val temperature: Float = 1.0f,
)
