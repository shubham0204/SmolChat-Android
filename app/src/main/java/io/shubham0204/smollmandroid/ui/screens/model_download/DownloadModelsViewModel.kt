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

package io.shubham0204.smollmandroid.ui.screens.model_download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelSearch
import io.shubham0204.hf_model_hub_api.HFModelTree
import io.shubham0204.smollmandroid.data.HFModelsAPI
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.data.ModelsDB
import io.shubham0204.smollmandroid.ui.components.hideProgressDialog
import io.shubham0204.smollmandroid.ui.components.setProgressDialogText
import io.shubham0204.smollmandroid.ui.components.setProgressDialogTitle
import io.shubham0204.smollmandroid.ui.components.showProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

@Single
class DownloadModelsViewModel(
    val context: Context,
    val modelsDB: ModelsDB,
    val hfModelsAPI: HFModelsAPI,
) : ViewModel() {
    private val _modelInfoAndTree = MutableStateFlow<Pair<HFModelInfo.ModelInfo, List<HFModelTree.HFModelFile>>?>(null)
    val modelInfoAndTree: StateFlow<Pair<HFModelInfo.ModelInfo, List<HFModelTree.HFModelFile>>?> = _modelInfoAndTree

    val selectedModelState = mutableStateOf<LLMModel?>(null)
    val modelUrlState = mutableStateOf("")

    var viewModelId: String? = null

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadModel() {
        // Downloading files in Android with the DownloadManager API
        // Ref: https://youtu.be/4t8EevQSYK4?feature=shared
        val modelUrl = modelUrlState.value.ifEmpty { selectedModelState.value?.url ?: return }
        val fileName = modelUrl.substring(modelUrl.lastIndexOf('/') + 1)
        val request =
            DownloadManager
                .Request(modelUrl.toUri())
                .setTitle(fileName)
                .setDescription(
                    "The GGUF model will be downloaded on your device for use with SmolChat.",
                ).setMimeType("application/octet-stream")
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE,
                ).setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                ).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        downloadManager.enqueue(request)
    }

    fun getModels(query: String): Flow<PagingData<HFModelSearch.ModelSearchResult>> = hfModelsAPI.getModelsList(query)

    /**
     * Given the model file URI, copy the model file to the app's internal directory. Once copied,
     * add a new LLMModel entity with modelName=fileName where fileName is the name of the model
     * file.
     */
    fun copyModelFile(
        uri: Uri,
        onComplete: () -> Unit,
    ) {
        var fileName = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }
        if (fileName.isNotEmpty()) {
            setProgressDialogTitle("Copying model file...")
            setProgressDialogText(
                "$fileName\n(The model needs to be copied to the app's internal directory for use)",
            )
            showProgressDialog()
            CoroutineScope(Dispatchers.IO).launch {
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    FileOutputStream(File(context.filesDir, fileName)).use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                }
                modelsDB.addModel(
                    fileName,
                    "",
                    Paths.get(context.filesDir.absolutePath, fileName).toString(),
                )
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    onComplete()
                }
            }
        } else {
            Toast.makeText(context, "Invalid file", Toast.LENGTH_SHORT).show()
        }
    }

    fun fetchModelInfoAndTree(modelId: String) {
        _modelInfoAndTree.value = null
        CoroutineScope(Dispatchers.IO).launch {
            val modelInfo = hfModelsAPI.getModelInfo(modelId)
            var modelTree = hfModelsAPI.getModelTree(modelId)
            modelTree =
                modelTree.filter { modelFile ->
                    modelFile.path.endsWith("gguf")
                }
            _modelInfoAndTree.value = Pair(modelInfo, modelTree)
        }
    }
}
