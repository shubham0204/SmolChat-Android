import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import io.shubham0204.smollmandroid.data.Chat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CustomNavTypes {
    val ChatNavType =
        object : NavType<Chat>(isNullableAllowed = false) {
            override fun get(bundle: Bundle, key: String): Chat? {
                return Json.decodeFromString(bundle.getString(key) ?: return null)
            }

            override fun parseValue(value: String): Chat =
                Json.decodeFromString(Uri.decode(value))

            override fun serializeAsValue(value: Chat): String =
                Uri.encode(Json.encodeToString(value))

            override fun put(bundle: Bundle, key: String, value: Chat) {
                bundle.putString(key, Json.encodeToString(value))
            }
        }
}
