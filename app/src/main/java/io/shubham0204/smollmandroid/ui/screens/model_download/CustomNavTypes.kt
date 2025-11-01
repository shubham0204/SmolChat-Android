package io.shubham0204.smollmandroid.ui.screens.model_download

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelTree
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CustomNavTypes {
    val HFModelInfoNavType =
        object : NavType<HFModelInfo.ModelInfo>(
            isNullableAllowed = false,
        ) {
            override fun get(
                bundle: Bundle,
                key: String,
            ): HFModelInfo.ModelInfo? {
                return Json.decodeFromString(bundle.getString(key) ?: return null)
            }

            override fun parseValue(value: String): HFModelInfo.ModelInfo = Json.decodeFromString(Uri.decode(value))

            override fun serializeAsValue(value: HFModelInfo.ModelInfo): String = Uri.encode(Json.encodeToString(value))

            override fun put(
                bundle: Bundle,
                key: String,
                value: HFModelInfo.ModelInfo,
            ) {
                bundle.putString(key, Json.encodeToString(value))
            }
        }

    val HFModelFileNavType =
        object : NavType<List<HFModelTree.HFModelFile>>(
            isNullableAllowed = false,
        ) {
            override fun get(
                bundle: Bundle,
                key: String,
            ): List<HFModelTree.HFModelFile>? {
                return Json.decodeFromString(bundle.getString(key) ?: return null)
            }

            override fun parseValue(value: String): List<HFModelTree.HFModelFile> = Json.decodeFromString(Uri.decode(value))

            override fun serializeAsValue(value: List<HFModelTree.HFModelFile>): String = Uri.encode(Json.encodeToString(value))

            override fun put(
                bundle: Bundle,
                key: String,
                value: List<HFModelTree.HFModelFile>,
            ) {
                bundle.putString(key, Json.encodeToString(value))
            }
        }
}
