package io.shubham0204.smollm_server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI Model object, representing a LLM model available for inference
 * docs: https://platform.openai.com/docs/api-reference/models/object
 */
@Serializable
data class OpenAIModel(
    val id: String,
    val created: Long,
    @SerialName("object") val object_: String = "model", // `object` is a reserved word in Kotlin
    @SerialName("owned_by") val ownedBy: String,
)
