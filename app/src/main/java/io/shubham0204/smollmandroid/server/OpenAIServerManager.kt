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

package io.shubham0204.smollmandroid.server

import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.shubham0204.smollm.SmolLM
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.LLMModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val LOGTAG = "[OpenAIServerManager]"

data class ServerConfig(
    val port: Int = 8080,
    val host: String = "0.0.0.0",
    val modelId: Long = -1L,
    val contextSize: Long = 2048L,
    val numThreads: Int = 4,
    val temperature: Float = 0.8f,
    val minP: Float = 0.1f,
)

data class ServerRequestLog(
    val timestamp: String,
    val method: String,
    val path: String,
    val status: Int,
    val details: String,
)

data class OpenAIServerState(
    val isRunning: Boolean = false,
    val isLoadingModel: Boolean = false,
    val config: ServerConfig = ServerConfig(),
    val loadedModelName: String? = null,
    val localIpAddress: String = "127.0.0.1",
    val errorMessage: String? = null,
    val logs: List<ServerRequestLog> = emptyList(),
)

@Single
class OpenAIServerManager(private val appDB: AppDB) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inferenceMutex = Mutex()
    private val smolLM = SmolLM()
    private var loadedModel: LLMModel? = null
    private var loadedConfig: ServerConfig? = null
    private var ktorServer:
            EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val _state =
        MutableStateFlow(OpenAIServerState(localIpAddress = detectLocalIpAddress()))
    val state: StateFlow<OpenAIServerState> = _state.asStateFlow()

    fun refreshLocalIp() {
        _state.update { it.copy(localIpAddress = detectLocalIpAddress()) }
    }

    fun updateConfig(config: ServerConfig) {
        _state.update { it.copy(config = config, errorMessage = null) }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
    }

    private fun appendLog(method: String, path: String, status: Int, details: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry =
            ServerRequestLog(
                timestamp = timeStr,
                method = method,
                path = path,
                status = status,
                details = details,
            )
        _state.update { current ->
            current.copy(logs = (listOf(entry) + current.logs).take(50))
        }
    }

    suspend fun startServer(config: ServerConfig): Result<Unit> {
        return inferenceMutex.withLock {
            try {
                stopServerInternal()
                _state.update {
                    it.copy(
                        config = config,
                        isLoadingModel = true,
                        errorMessage = null,
                        localIpAddress = detectLocalIpAddress(),
                    )
                }

                val models = appDB.getModelsList()
                val targetModel = models.find { it.id == config.modelId } ?: models.firstOrNull()
                if (targetModel != null) {
                    loadModelInternal(targetModel, config)
                }

                val server =
                    embeddedServer(
                        factory = CIO,
                        port = config.port,
                        host = config.host,
                    ) {
                        install(CORS) {
                            anyHost()
                            allowHeader(HttpHeaders.ContentType)
                            allowHeader(HttpHeaders.Authorization)
                            allowMethod(HttpMethod.Get)
                            allowMethod(HttpMethod.Post)
                            allowMethod(HttpMethod.Options)
                        }
                        install(ContentNegotiation) {
                            json(jsonFormat)
                        }
                        routing {
                            healthRoute()
                            getOpenAPISpec()
                            listModels()
                            completionsEndpoints()
                        }
                    }

                server.start(wait = false)
                ktorServer = server
                _state.update {
                    it.copy(
                        isRunning = true,
                        isLoadingModel = false,
                        loadedModelName = loadedModel?.name,
                        errorMessage = null,
                    )
                }
                appendLog(
                    "SERVER",
                    "Started on ${config.host}:${config.port}",
                    200,
                    "Model: ${loadedModel?.name ?: "None"}",
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(LOGTAG, "Failed to start server", e)
                stopServerInternal()
                _state.update {
                    it.copy(
                        isRunning = false,
                        isLoadingModel = false,
                        errorMessage = e.message ?: "Failed to start server",
                    )
                }
                Result.failure(e)
            }
        }
    }

    private fun Routing.healthRoute() {
        get("/health") {
            val response =
                HealthStatusResponse(
                    status = "ok",
                    loadedModel = loadedModel?.name,
                )
            appendLog("GET", "/health", 200, "model=${loadedModel?.name ?: "none"}")
            call.respond(HttpStatusCode.OK, response)
        }
    }

    private fun Routing.getOpenAPISpec() {
        get("/openapi.json") {
            appendLog("GET", "/openapi.json", 200, "OpenAPI 3.1.0 spec")
            call.respondText(
                text = OPENAPI_SPEC_JSON,
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }

    private fun Routing.listModels() {
        get("/v1/models") {
            val availableModels =
                appDB.getModelsList().map { model ->
                    ModelObject(
                        id = model.name,
                        created = System.currentTimeMillis() / 1000L,
                    )
                }
            appendLog("GET", "/v1/models", 200, "${availableModels.size} model(s)")
            call.respond(
                HttpStatusCode.OK,
                ModelListResponse(data = availableModels),
            )
        }
    }

    private fun Routing.completionsEndpoints() {
        post("/v1/chat/completions") {
            val request =
                try {
                    call.receive<ChatCompletionRequest>()
                } catch (e: Exception) {
                    appendLog(
                        "POST",
                        "/v1/chat/completions",
                        400,
                        "Invalid JSON: ${e.message}",
                    )
                    call.respond(
                        HttpStatusCode.BadRequest,
                        OpenAIErrorResponse(
                            OpenAIErrorDetail(message = "Invalid request body: ${e.message}")
                        ),
                    )
                    return@post
                }

            if (request.messages.isEmpty()) {
                appendLog(
                    "POST",
                    "/v1/chat/completions",
                    400,
                    "Empty messages list",
                )
                call.respond(
                    HttpStatusCode.BadRequest,
                    OpenAIErrorResponse(
                        OpenAIErrorDetail(message = "'messages' array must not be empty")
                    ),
                )
                return@post
            }

            inferenceMutex.withLock {
                val model =
                    try {
                        resolveAndEnsureModelLoaded(request)
                    } catch (e: Exception) {
                        appendLog(
                            "POST",
                            "/v1/chat/completions",
                            500,
                            "Model error: ${e.message}",
                        )
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            OpenAIErrorResponse(
                                OpenAIErrorDetail(
                                    message = e.message ?: "Failed to load model",
                                    type = "server_error",
                                )
                            ),
                        )
                        return@withLock
                    }

                // Reset conversation state and populate history from the request
                smolLM.clearMessages()
                val historyMessages = request.messages.dropLast(1)
                for (msg in historyMessages) {
                    when (msg.role.lowercase(Locale.ROOT)) {
                        "system",
                        "developer" -> smolLM.addSystemPrompt(msg.content)

                        "assistant" -> smolLM.addAssistantMessage(msg.content)
                        else -> smolLM.addUserMessage(msg.content)
                    }
                }
                val lastQuery = request.messages.last().content
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val createdTimestamp = System.currentTimeMillis() / 1000L
                val maxTokens = request.maxTokens

                if (request.stream) {
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                    call.response.header(HttpHeaders.Connection, "keep-alive")
                    call.respondTextWriter(
                        contentType = ContentType.Text.EventStream,
                        status = HttpStatusCode.OK,
                    ) {
                        try {
                            // Initial chunk with assistant role
                            val initialChunk =
                                ChatCompletionChunk(
                                    id = completionId,
                                    created = createdTimestamp,
                                    model = model.name,
                                    choices =
                                        listOf(
                                            ChatCompletionChunkChoice(
                                                index = 0,
                                                delta = ChatDeltaPayload(role = "assistant"),
                                                finishReason = null,
                                            )
                                        ),
                                )
                            write("data: ${jsonFormat.encodeToString(initialChunk)}\n\n")
                            flush()

                            var generatedTokens = 0
                            var finishReason = "stop"
                            try {
                                smolLM.getResponseAsFlow(lastQuery).collect { piece ->
                                    if (piece.isNotEmpty()) {
                                        val chunk =
                                            ChatCompletionChunk(
                                                id = completionId,
                                                created = createdTimestamp,
                                                model = model.name,
                                                choices =
                                                    listOf(
                                                        ChatCompletionChunkChoice(
                                                            index = 0,
                                                            delta =
                                                                ChatDeltaPayload(content = piece),
                                                            finishReason = null,
                                                        )
                                                    ),
                                            )
                                        write(
                                            "data: ${
                                                jsonFormat.encodeToString(
                                                    chunk
                                                )
                                            }\n\n"
                                        )
                                        flush()
                                    }
                                    generatedTokens++
                                    if (maxTokens != null && generatedTokens >= maxTokens) {
                                        finishReason = "length"
                                        throw MaxTokensReachedException()
                                    }
                                }
                            } catch (_: MaxTokensReachedException) {
                                // Reached max_tokens limit
                            }

                            val stopChunk =
                                ChatCompletionChunk(
                                    id = completionId,
                                    created = createdTimestamp,
                                    model = model.name,
                                    choices =
                                        listOf(
                                            ChatCompletionChunkChoice(
                                                index = 0,
                                                delta = ChatDeltaPayload(),
                                                finishReason = finishReason,
                                            )
                                        ),
                                )
                            write("data: ${jsonFormat.encodeToString(stopChunk)}\n\n")
                            write("data: [DONE]\n\n")
                            flush()

                            val speed = smolLM.getResponseGenerationSpeed()
                            appendLog(
                                "POST",
                                "/v1/chat/completions",
                                200,
                                "stream=true, tokens=$generatedTokens, %.1f tok/s".format(speed),
                            )
                        } catch (e: Exception) {
                            Log.e(LOGTAG, "Streaming error", e)
                            appendLog(
                                "POST",
                                "/v1/chat/completions",
                                500,
                                "Stream error: ${e.message}",
                            )
                        }
                    }
                } else {
                    try {
                        val responseBuilder = StringBuilder()
                        var generatedTokens = 0
                        var finishReason = "stop"
                        try {
                            smolLM.getResponseAsFlow(lastQuery).collect { piece ->
                                responseBuilder.append(piece)
                                generatedTokens++
                                if (maxTokens != null && generatedTokens >= maxTokens) {
                                    finishReason = "length"
                                    throw MaxTokensReachedException()
                                }
                            }
                        } catch (_: MaxTokensReachedException) {
                            // Reached max_tokens limit
                        }

                        val totalTokens = smolLM.getContextLengthUsed()
                        val promptTokens = (totalTokens - generatedTokens).coerceAtLeast(0)
                        val speed = smolLM.getResponseGenerationSpeed()

                        val completionResponse =
                            ChatCompletionResponse(
                                id = completionId,
                                created = createdTimestamp,
                                model = model.name,
                                choices =
                                    listOf(
                                        ChatCompletionChoice(
                                            index = 0,
                                            message =
                                                ChatMessagePayload(
                                                    role = "assistant",
                                                    content = responseBuilder.toString(),
                                                ),
                                            finishReason = finishReason,
                                        )
                                    ),
                                usage =
                                    CompletionUsage(
                                        promptTokens = promptTokens,
                                        completionTokens = generatedTokens,
                                        totalTokens = totalTokens,
                                    ),
                            )
                        appendLog(
                            "POST",
                            "/v1/chat/completions",
                            200,
                            "tokens=$generatedTokens, %.1f tok/s".format(speed),
                        )
                        call.respond(HttpStatusCode.OK, completionResponse)
                    } catch (e: Exception) {
                        Log.e(LOGTAG, "Completion error", e)
                        appendLog(
                            "POST",
                            "/v1/chat/completions",
                            500,
                            "Error: ${e.message}",
                        )
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            OpenAIErrorResponse(
                                OpenAIErrorDetail(
                                    message = e.message ?: "Inference failed",
                                    type = "server_error",
                                )
                            ),
                        )
                    }
                }
            }
        }
    }

    fun stopServer() {
        scope.launch {
            inferenceMutex.withLock {
                stopServerInternal()
                appendLog("SERVER", "Stopped", 200, "Server shut down")
            }
        }
    }

    private fun stopServerInternal() {
        try {
            ktorServer?.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
        } catch (e: Exception) {
            Log.e(LOGTAG, "Error stopping Ktor server", e)
        }
        ktorServer = null
        try {
            smolLM.close()
        } catch (e: Exception) {
            Log.e(LOGTAG, "Error closing SmolLM instance", e)
        }
        loadedModel = null
        loadedConfig = null
        _state.update {
            it.copy(
                isRunning = false,
                isLoadingModel = false,
                loadedModelName = null,
            )
        }
    }

    private suspend fun resolveAndEnsureModelLoaded(request: ChatCompletionRequest): LLMModel {
        val currentConfig = _state.value.config
        val models = appDB.getModelsList()
        if (models.isEmpty()) {
            throw IllegalStateException(
                "No GGUF models are downloaded in SmolChat. Please download a model first."
            )
        }

        val requestedModel =
            if (request.model.isNotBlank()) {
                models.find { it.name.equals(request.model, ignoreCase = true) }
            } else {
                null
            }
        val targetModel =
            requestedModel
                ?: loadedModel
                ?: models.find { it.id == currentConfig.modelId }
                ?: models.first()

        val effectiveTemp = request.temperature ?: currentConfig.temperature
        val effectiveMinP = request.minP ?: currentConfig.minP

        val needsReload =
            loadedModel?.id != targetModel.id ||
                loadedConfig?.temperature != effectiveTemp ||
                loadedConfig?.minP != effectiveMinP

        if (needsReload) {
            val updatedConfig =
                currentConfig.copy(
                    modelId = targetModel.id,
                    temperature = effectiveTemp,
                    minP = effectiveMinP,
                )
            loadModelInternal(targetModel, updatedConfig)
        }
        return targetModel
    }

    private suspend fun loadModelInternal(model: LLMModel, config: ServerConfig) {
        _state.update { it.copy(isLoadingModel = true) }
        try {
            smolLM.close()
            smolLM.load(
                modelPath = model.path,
                params =
                    SmolLM.InferenceParams(
                        minP = config.minP,
                        temperature = config.temperature,
                        storeChats = false,
                        contextSize =
                            config.contextSize.takeIf { it > 0 } ?: model.contextSize.toLong(),
                        chatTemplate =
                            model.chatTemplate.takeIf {
                                it.isNotBlank() && ("{%" in it || "{{" in it)
                            },
                        numThreads = config.numThreads,
                        useMmap = true,
                        useMlock = false,
                    ),
            )
            loadedModel = model
            loadedConfig = config
            _state.update {
                it.copy(
                    isLoadingModel = false,
                    loadedModelName = model.name,
                )
            }
        } catch (e: Exception) {
            loadedModel = null
            loadedConfig = null
            _state.update { it.copy(isLoadingModel = false, loadedModelName = null) }
            throw e
        }
    }

    companion object {
        fun detectLocalIpAddress(): String {
            try {
                val interfaces =
                    NetworkInterface.getNetworkInterfaces()?.toList() ?: return "127.0.0.1"
                // Prefer wlan / ap / eth interfaces with site-local (RFC1918) IPv4 addresses
                interfaces
                    .sortedBy { networkInterface ->
                        val name = networkInterface.name.lowercase(Locale.ROOT)
                        when {
                            name.startsWith("wlan") || name.startsWith("ap") -> 0
                            name.startsWith("eth") || name.startsWith("rndis") -> 1
                            else -> 2
                        }
                    }
                    .filter { it.isUp && !it.isLoopback }
                    .forEach { networkInterface ->
                        networkInterface.inetAddresses
                            .toList()
                            .filter {
                                !it.isLoopbackAddress && it is Inet4Address && it.isSiteLocalAddress
                            }
                            .forEach { address ->
                                val ip = address.hostAddress
                                if (!ip.isNullOrBlank()) {
                                    return ip
                                }
                            }
                    }
            } catch (e: Exception) {
                Log.e(LOGTAG, "Failed to get local IP address", e)
            }
            return "127.0.0.1"
        }
    }
}

private class MaxTokensReachedException : Exception()
