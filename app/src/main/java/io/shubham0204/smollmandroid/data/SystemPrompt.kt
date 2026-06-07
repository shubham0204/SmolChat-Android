package io.shubham0204.smollmandroid.data

import kotlinx.serialization.Serializable

@Serializable
data class SystemPrompt(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val body: String = "",
)
