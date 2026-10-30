package io.shubham0204.smollmandroid.services

import android.content.Context
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ASRModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import java.io.File

@Single
class ModelsRepository(private val context: Context, private val appDB: AppDB) {
    init {
        for (model in appDB.getModelsList()) {
            if (!File(model.path).exists()) {
                deleteModel(model.id)
            }
        }
    }

    companion object {
        fun checkIfModelsDownloaded(context: Context): Boolean {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".gguf")) {
                    return true
                }
            }
            return false
        }
    }

    fun getModelFromId(id: Long): LLMModel = appDB.getModel(id)

    fun getAvailableModels(): Flow<List<LLMModel>> = appDB.getModels()

    fun getAvailableModelsList(): List<LLMModel> = appDB.getModelsList()

    fun deleteModel(id: Long) {
        appDB.getModel(id).also {
            File(it.path).delete()
            appDB.deleteModel(it.id)
        }
    }

    fun isSpeech2TextModelDownloaded(asrModel: ASRModel): Boolean {
        return File(context.filesDir, asrModel.name).exists()
    }
}