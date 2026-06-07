package io.shubham0204.smollmandroid.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class SystemPromptsStore(private val sharedPrefStore: SharedPrefStore) {
    private val key = "system_prompts"
    private val _systemPrompts = MutableStateFlow<List<SystemPrompt>>(loadPrompts())
    val systemPrompts: StateFlow<List<SystemPrompt>> = _systemPrompts.asStateFlow()

    private fun loadPrompts(): List<SystemPrompt> {
        val json = sharedPrefStore.get(key, "[]")
        return try {
            Json.decodeFromString<List<SystemPrompt>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addPrompt(name: String, body: String) {
        val newList = _systemPrompts.value + SystemPrompt(name = name, body = body)
        savePrompts(newList)
    }

    fun deletePrompt(id: Long) {
        val newList = _systemPrompts.value.filter { it.id != id }
        savePrompts(newList)
    }

    private fun savePrompts(prompts: List<SystemPrompt>) {
        val json = Json.encodeToString(prompts)
        sharedPrefStore.put(key, json)
        _systemPrompts.value = prompts
    }
}
