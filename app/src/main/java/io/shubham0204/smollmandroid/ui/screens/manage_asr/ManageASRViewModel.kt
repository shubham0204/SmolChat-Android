package io.shubham0204.smollmandroid.ui.screens.manage_asr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shubham0204.smollmandroid.data.SharedPrefStore
import io.shubham0204.smollmandroid.llm.ModelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.pathString

data class UIASRModel(
    val model: ASRModel,
    val isEnabled: Boolean = false,
    val isDownloaded: Boolean = false,
)

data class ManageASRUIState(
    val isEnabled: Boolean = false,
    val downloadingModelProgress: Int? = null,
    val downloadingModelErrorMessage: String? = null,
    val modelsList: List<UIASRModel> = emptyList(),
)

sealed interface ManageASRUIEvent {
    data object EnableSpeech2Text : ManageASRUIEvent

    data object DisableSpeech2Text : ManageASRUIEvent

    data class EnableSpeech2TextModel(val asrModel: ASRModel) : ManageASRUIEvent

    data class DownloadSpeech2TextModel(val asrModel: ASRModel) : ManageASRUIEvent
}

@KoinViewModel
class ManageASRViewModel(
    private val sharedPrefStore: SharedPrefStore,
    private val modelsRepository: ModelsRepository,
    private val downloadService: DownloadService,
    private val toastNotifService: ToastNotifService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initializeSettingsState())
    val uiState: StateFlow<ManageASRUIState> = _uiState

    fun onEvent(event: ManageASRUIEvent) {
        when (event) {
            is ManageASRUIEvent.EnableSpeech2Text -> {
                enableSpeech2Text()
            }

            is ManageASRUIEvent.DisableSpeech2Text -> {
                sharedPrefStore.put(SETTING_KEY_SPEECH2TEXT_ENABLED, false)
                _uiState.update { it.copy(isEnabled = false) }
            }

            is ManageASRUIEvent.DownloadSpeech2TextModel -> {
                startModelDownload(event.asrModel)
            }

            is ManageASRUIEvent.EnableSpeech2TextModel -> {
                enableModelForSpeech2Text(event.asrModel)
            }
        }
    }

    private fun enableModelForSpeech2Text(asrModel: ASRModel) {
        sharedPrefStore.put(SETTING_KEY_SPEECH2TEXT_CURR_MODEL_NAME, asrModel.name)
        _uiState.update {
            val updatedModelsList =
                it.modelsList.map { uiASRModel ->
                    uiASRModel.copy(isEnabled = uiASRModel.model.name == asrModel.name)
                }
            it.copy(isEnabled = true, modelsList = updatedModelsList)
        }
        sharedPrefStore.put(SETTING_KEY_SPEECH2TEXT_ENABLED, true)
    }

    private fun enableSpeech2Text() {
        if (!checkIfAnyASRModelIsDownloaded() || !checkIfAnyUIASRModelIsSelected()) {
            toastNotifService.showLongToast("Please download or select an ASR model.")
        } else {
            sharedPrefStore.put(SETTING_KEY_SPEECH2TEXT_ENABLED, true)
            _uiState.update { it.copy(isEnabled = true) }
        }
    }

    private fun startModelDownload(asrModel: ASRModel) {
        _uiState.update { it.copy(downloadingModelProgress = 0) }
        downloadService.startDownload(
            asrModel.bundleDownloadUrl,
            downloadService.defaultDownloadDir,
            asrModel.bundleFileName,
            onStart = { toastNotifService.showShortToast("Starting model download...") },
            onProgress = { progress ->
                _uiState.update { it.copy(downloadingModelProgress = progress) }
            },
            onSuccess = {
                viewModelScope.launch {
                    val zipPath =
                        Paths.get(downloadService.defaultDownloadDir, asrModel.bundleFileName)
                            .pathString
                    withContext(Dispatchers.IO) {
                        unzipModel(zipPath, asrModel.name)
                    }
                    _uiState.update {
                        val updatedModelsList =
                            it.modelsList.map { uiASRModel ->
                                if (uiASRModel.model.name == asrModel.name) {
                                    uiASRModel.copy(isDownloaded = true)
                                } else {
                                    uiASRModel.copy()
                                }
                            }
                        it.copy(downloadingModelProgress = null, modelsList = updatedModelsList)
                    }
                    toastNotifService.showShortToast("Model downloaded and extracted.")
                }

            },
            onFailure = { failureMessage ->
                _uiState.update {
                    it.copy(
                        downloadingModelProgress = null,
                        downloadingModelErrorMessage = failureMessage,
                    )
                }
                toastNotifService.showLongToast("Download failed: $failureMessage")
            },
        )
    }

    private fun unzipModel(modelZipBundlePath: String, modelName: String) {
        val fileInputStream = FileInputStream(modelZipBundlePath)
        val zipInputStream = ZipInputStream(fileInputStream)
        val destDir = File(downloadService.defaultDownloadDir, modelName)
        if (!destDir.exists()) {
            destDir.mkdir()
        }
        var zipEntry: ZipEntry? = zipInputStream.getNextEntry()
        while (zipEntry != null) {
            if (zipEntry.name.startsWith("__MACOSX")) {
                continue
            }
            Files.copy(zipInputStream, Paths.get(destDir.absolutePath, zipEntry.name))
            zipInputStream.closeEntry()
            zipEntry = zipInputStream.getNextEntry()
        }
        zipInputStream.closeEntry()
        zipInputStream.close()
        fileInputStream.close()
        // Delete the zip file after extraction
        File(modelZipBundlePath).delete()
    }

    private fun initializeSettingsState(): ManageASRUIState {
        val isASREnabled =
            sharedPrefStore.get(
                SETTING_KEY_SPEECH2TEXT_ENABLED,
                SETTING_DEF_VALUE_SPEECH2_TEXT_ENABLED,
            )
        val currentlyLoadedASRModelName =
            sharedPrefStore.get(
                SETTING_KEY_SPEECH2TEXT_CURR_MODEL_NAME,
                SETTING_DEF_VALUE_SPEECH2TEXT_CURR_MODEL_NAME,
            )
        return ManageASRUIState(
            isEnabled = isASREnabled,
            modelsList =
                availableASRModels.map { asrModel ->
                    UIASRModel(
                        model = asrModel,
                        isEnabled = currentlyLoadedASRModelName == asrModel.name,
                        isDownloaded = modelsRepository.isSpeech2TextModelDownloaded(asrModel),
                    )
                },
        )
    }

    private fun checkIfAnyASRModelIsDownloaded(): Boolean {
        return availableASRModels.any { modelsRepository.isSpeech2TextModelDownloaded(it) }
    }

    private fun checkIfAnyUIASRModelIsSelected(): Boolean {
        return _uiState.value.modelsList.any { it.isEnabled }
    }
}
