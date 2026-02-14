package io.shubham0204.smollmandroid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.shubham0204.smollmandroid.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

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

    fun setupChangeListener(key: String, onChange: (String, String) -> Unit) {

    }
}
