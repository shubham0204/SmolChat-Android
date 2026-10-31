package io.shubham0204.smollmandroid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.shubham0204.smollmandroid.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

const val KEY_GLOBAL_SYSTEM_PROMPT = "global_chat_system_prompt"
const val KEY_GLOBAL_MIN_P = "global_chat_min_p"
const val KEY_GLOBAL_TEMPERATURE = "global_chat_temperature"
const val KEY_GLOBAL_CONTEXT_SIZE = "global_chat_context_size"
const val KEY_GLOBAL_N_THREADS = "global_chat_n_threads"
const val KEY_GLOBAL_USE_MMAP = "global_chat_use_mmap"
const val KEY_GLOBAL_USE_MLOCK = "global_chat_use_mlock"
const val KEY_GLOBAL_ENABLE_THINKING = "global_chat_enable_thinking"

const val DEFAULT_GLOBAL_SYSTEM_PROMPT = "You are a helpful assistant."
const val DEFAULT_GLOBAL_MIN_P = 0.1f
const val DEFAULT_GLOBAL_TEMPERATURE = 0.8f
const val DEFAULT_GLOBAL_CONTEXT_SIZE = 2048
const val DEFAULT_GLOBAL_N_THREADS = 4
const val DEFAULT_GLOBAL_USE_MMAP = true
const val DEFAULT_GLOBAL_USE_MLOCK = false
const val DEFAULT_GLOBAL_ENABLE_THINKING = true

@Serializable
data class GlobalChatSettings(
    val systemPrompt: String = DEFAULT_GLOBAL_SYSTEM_PROMPT,
    val minP: Float = DEFAULT_GLOBAL_MIN_P,
    val temperature: Float = DEFAULT_GLOBAL_TEMPERATURE,
    val contextSize: Int = DEFAULT_GLOBAL_CONTEXT_SIZE,
    val nThreads: Int = DEFAULT_GLOBAL_N_THREADS,
    val useMmap: Boolean = DEFAULT_GLOBAL_USE_MMAP,
    val useMlock: Boolean = DEFAULT_GLOBAL_USE_MLOCK,
    val enableThinking: Boolean = DEFAULT_GLOBAL_ENABLE_THINKING,
)

@Single
class SharedPrefStore(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
    private val _sharedPrefStoreChanges = MutableStateFlow<String?>(null)
    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        _sharedPrefStoreChanges.value = key
    }
    val sharedPrefStoreChanges: StateFlow<String?> = _sharedPrefStoreChanges

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener)
    }

    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
            is Float -> sharedPreferences.getFloat(key, defaultValue) as T
            is Int -> sharedPreferences.getInt(key, defaultValue) as T
            is String -> sharedPreferences.getString(key, defaultValue) as T
            is Long -> sharedPreferences.getLong(key, defaultValue) as T
            else -> throw Exception("Unsupported type")
        }
    }

    fun put(key: String, value: Any) {
        sharedPreferences.edit {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                is String -> putString(key, value)
                is Long -> putLong(key, value)
                else -> throw Exception("Unsupported type")
            }
        }
    }

    fun getGlobalChatSettings(): GlobalChatSettings =
        GlobalChatSettings(
            systemPrompt = get(KEY_GLOBAL_SYSTEM_PROMPT, DEFAULT_GLOBAL_SYSTEM_PROMPT),
            minP = get(KEY_GLOBAL_MIN_P, DEFAULT_GLOBAL_MIN_P),
            temperature = get(KEY_GLOBAL_TEMPERATURE, DEFAULT_GLOBAL_TEMPERATURE),
            contextSize = get(KEY_GLOBAL_CONTEXT_SIZE, DEFAULT_GLOBAL_CONTEXT_SIZE),
            nThreads = get(KEY_GLOBAL_N_THREADS, DEFAULT_GLOBAL_N_THREADS),
            useMmap = get(KEY_GLOBAL_USE_MMAP, DEFAULT_GLOBAL_USE_MMAP),
            useMlock = get(KEY_GLOBAL_USE_MLOCK, DEFAULT_GLOBAL_USE_MLOCK),
            enableThinking = get(KEY_GLOBAL_ENABLE_THINKING, DEFAULT_GLOBAL_ENABLE_THINKING),
        )

    fun saveGlobalChatSettings(settings: GlobalChatSettings) {
        put(KEY_GLOBAL_SYSTEM_PROMPT, settings.systemPrompt)
        put(KEY_GLOBAL_MIN_P, settings.minP)
        put(KEY_GLOBAL_TEMPERATURE, settings.temperature)
        put(KEY_GLOBAL_CONTEXT_SIZE, settings.contextSize)
        put(KEY_GLOBAL_N_THREADS, settings.nThreads)
        put(KEY_GLOBAL_USE_MMAP, settings.useMmap)
        put(KEY_GLOBAL_USE_MLOCK, settings.useMlock)
        put(KEY_GLOBAL_ENABLE_THINKING, settings.enableThinking)
    }

    fun setupChangeListener(key: String, onChange: (String, String) -> Unit) {

    }
}
