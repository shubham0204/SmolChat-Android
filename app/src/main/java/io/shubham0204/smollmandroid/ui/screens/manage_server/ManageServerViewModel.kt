/*
 * Copyright (C) 2025 Shubham Panchal
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

package io.shubham0204.smollmandroid.ui.screens.manage_server

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.server.OpenAIServerManager
import io.shubham0204.smollmandroid.server.OpenAIServerService
import io.shubham0204.smollmandroid.server.OpenAIServerState
import io.shubham0204.smollmandroid.server.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class ManageServerUIState(
    val serverState: OpenAIServerState = OpenAIServerState(),
    val availableModels: List<LLMModel> = emptyList(),
)

@KoinViewModel
class ManageServerViewModel(
    private val context: Context,
    private val appDB: AppDB,
    private val serverManager: OpenAIServerManager,
) : ViewModel() {

    private val modelsFlow = MutableStateFlow<List<LLMModel>>(emptyList())

    val uiState: StateFlow<ManageServerUIState> =
        combine(serverManager.state, modelsFlow) { sState, models ->
            ManageServerUIState(
                serverState = sState,
                availableModels = models,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ManageServerUIState(
                serverState = serverManager.state.value,
                availableModels = appDB.getModelsList(),
            ),
        )

    init {
        serverManager.refreshLocalIp()
        viewModelScope.launch {
            appDB.getModels().collect { models ->
                modelsFlow.value = models
                val currentConfig = serverManager.state.value.config
                if (currentConfig.modelId == -1L && models.isNotEmpty()) {
                    val firstModel = models.first()
                    serverManager.updateConfig(
                        currentConfig.copy(
                            modelId = firstModel.id,
                            contextSize = firstModel.contextSize.toLong().coerceAtMost(4096L),
                        )
                    )
                }
            }
        }
    }

    fun updateConfig(config: ServerConfig) {
        serverManager.updateConfig(config)
    }

    fun startServer(config: ServerConfig) {
        serverManager.updateConfig(config)
        OpenAIServerService.start(context, config)
    }

    fun stopServer() {
        OpenAIServerService.stop(context)
    }

    fun clearLogs() {
        serverManager.clearLogs()
    }
}
