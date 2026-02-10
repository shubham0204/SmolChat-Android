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

package io.shubham0204.smollmandroid.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

/**
 * Manages the state of the VoiceChatService and provides a way for the service
 * and UI components to communicate.
 */
@Single
class VoiceChatServiceManager {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _stopServiceRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stopServiceRequest: SharedFlow<Unit> = _stopServiceRequest

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun requestStopService() {
        _stopServiceRequest.tryEmit(Unit)
    }
}
