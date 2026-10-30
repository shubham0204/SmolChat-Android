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

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.Copy
import compose.icons.feathericons.Play
import compose.icons.feathericons.Square
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.server.OpenAIServerState
import io.shubham0204.smollmandroid.server.ServerConfig
import io.shubham0204.smollmandroid.server.ServerRequestLog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.preview.dummyLLMModels
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.androidx.compose.koinViewModel

class ManageServerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ManageServerViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ManageServerScreen(
                uiState = uiState,
                onStartServer = viewModel::startServer,
                onStopServer = viewModel::stopServer,
                onUpdateConfig = viewModel::updateConfig,
                onClearLogs = viewModel::clearLogs,
                onBackClick = { finish() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServerScreen(
    uiState: ManageServerUIState,
    onStartServer: (ServerConfig) -> Unit,
    onStopServer: () -> Unit,
    onUpdateConfig: (ServerConfig) -> Unit,
    onClearLogs: () -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val serverState = uiState.serverState
    val config = serverState.config

    var portText by remember(config.port) { mutableStateOf(config.port.toString()) }
    var contextSizeText by remember(config.contextSize) { mutableStateOf(config.contextSize.toString()) }
    var threadsText by remember(config.numThreads) { mutableStateOf(config.numThreads.toString()) }
    var selectedHost by remember(config.host) { mutableStateOf(config.host) }
    var selectedModelId by remember(config.modelId) { mutableStateOf(config.modelId) }

    LaunchedEffect(uiState.availableModels, selectedModelId) {
        if (selectedModelId == -1L && uiState.availableModels.isNotEmpty()) {
            selectedModelId = uiState.availableModels.first().id
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val newConfig = buildServerConfig(
            base = config,
            portText = portText,
            host = selectedHost,
            modelId = selectedModelId,
            contextSizeText = contextSizeText,
            threadsText = threadsText,
        )
        onStartServer(newConfig)
    }

    val displayHost = if (selectedHost == "0.0.0.0") serverState.localIpAddress else "127.0.0.1"
    val activePort =
        if (serverState.isRunning) serverState.config.port else (portText.toIntOrNull() ?: 8080)
    val baseUrl = "http://$displayHost:$activePort/v1"
    val openApiUrl = "http://$displayHost:$activePort/openapi.json"
    val selectedModel = uiState.availableModels.find { it.id == selectedModelId }
        ?: uiState.availableModels.firstOrNull()
    val modelNameForCurl = serverState.loadedModelName ?: selectedModel?.name ?: "default"
    val curlCommand = """
        curl $baseUrl/chat/completions \
          -H "Content-Type: application/json" \
          -d '{
            "model": "$modelNameForCurl",
            "messages": [{"role": "user", "content": "Hello!"}],
            "stream": false
          }'
    """.trimIndent()

    SmolLMAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                ManageServerTopBar(onBackClick = onBackClick)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                ServerStatusCard(
                    serverState = serverState,
                    canStart = uiState.availableModels.isNotEmpty(),
                    onStartClick = {
                        val newConfig = buildServerConfig(
                            base = config,
                            portText = portText,
                            host = selectedHost,
                            modelId = selectedModelId,
                            contextSizeText = contextSizeText,
                            threadsText = threadsText,
                        )
                        onUpdateConfig(newConfig)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            onStartServer(newConfig)
                        }
                    },
                    onStopClick = onStopServer,
                )

                ServerConfigurationCard(
                    availableModels = uiState.availableModels,
                    selectedModel = selectedModel,
                    selectedHost = selectedHost,
                    portText = portText,
                    threadsText = threadsText,
                    contextSizeText = contextSizeText,
                    isRunning = serverState.isRunning,
                    onModelSelected = { model ->
                        selectedModelId = model.id
                        contextSizeText = model.contextSize.coerceAtMost(4096).toString()
                    },
                    onHostSelected = { selectedHost = it },
                    onPortChanged = { portText = it.filter { ch -> ch.isDigit() } },
                    onThreadsChanged = { threadsText = it.filter { ch -> ch.isDigit() } },
                    onContextSizeChanged = { contextSizeText = it.filter { ch -> ch.isDigit() } },
                )

                ServerEndpointsCard(
                    baseUrl = baseUrl,
                    openApiUrl = openApiUrl,
                    curlCommand = curlCommand,
                    onCopy = { label, text -> copyToClipboard(context, label, text) },
                )

                ServerLogsCard(
                    logs = serverState.logs,
                    onClearLogs = onClearLogs,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageServerTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { AppBarTitleText(stringResource(R.string.server_screen_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    FeatherIcons.ArrowLeft,
                    contentDescription = stringResource(R.string.button_text_back),
                )
            }
        },
    )
}

@Composable
private fun ServerStatusCard(
    serverState: OpenAIServerState,
    canStart: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (serverState.isRunning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ServerStatusHeader(serverState = serverState)

            ServerActionSection(
                isLoadingModel = serverState.isLoadingModel,
                isRunning = serverState.isRunning,
                canStart = canStart,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
            )

            if (serverState.errorMessage != null) {
                Text(
                    text = stringResource(R.string.server_error_prefix, serverState.errorMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ServerStatusHeader(serverState: OpenAIServerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    when {
                        serverState.isLoadingModel -> MaterialTheme.colorScheme.tertiary
                        serverState.isRunning -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    serverState.isLoadingModel -> stringResource(R.string.server_status_loading_model)
                    serverState.isRunning -> stringResource(R.string.server_status_running)
                    else -> stringResource(R.string.server_status_stopped)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (serverState.isRunning) {
                    stringResource(
                        R.string.server_status_active_model,
                        serverState.loadedModelName ?: stringResource(R.string.server_status_none),
                    )
                } else {
                    stringResource(R.string.server_status_stopped_desc)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ServerActionSection(
    isLoadingModel: Boolean,
    isRunning: Boolean,
    canStart: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    when {
        isLoadingModel -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.server_initializing_model),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        isRunning -> {
            Button(
                onClick = onStopClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    FeatherIcons.Square,
                    contentDescription = stringResource(R.string.server_action_stop),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.server_action_stop))
            }
        }

        else -> {
            Button(
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartClick,
            ) {
                Icon(
                    FeatherIcons.Play,
                    contentDescription = stringResource(R.string.server_action_start),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.server_action_start))
            }
        }
    }
}

@Composable
private fun ServerConfigurationCard(
    availableModels: List<LLMModel>,
    selectedModel: LLMModel?,
    selectedHost: String,
    portText: String,
    threadsText: String,
    contextSizeText: String,
    isRunning: Boolean,
    onModelSelected: (LLMModel) -> Unit,
    onHostSelected: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onThreadsChanged: (String) -> Unit,
    onContextSizeChanged: (String) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.server_config_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            ModelSelectorSection(
                availableModels = availableModels,
                selectedModel = selectedModel,
                enabled = !isRunning,
                onModelSelected = onModelSelected,
            )

            NetworkBindingSelector(
                selectedHost = selectedHost,
                enabled = !isRunning,
                onHostSelected = onHostSelected,
            )

            ServerRuntimeParametersFields(
                portText = portText,
                threadsText = threadsText,
                contextSizeText = contextSizeText,
                enabled = !isRunning,
                onPortChanged = onPortChanged,
                onThreadsChanged = onThreadsChanged,
                onContextSizeChanged = onContextSizeChanged,
            )
        }
    }
}

@Composable
private fun ModelSelectorSection(
    availableModels: List<LLMModel>,
    selectedModel: LLMModel?,
    enabled: Boolean,
    onModelSelected: (LLMModel) -> Unit,
) {
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    Text(stringResource(R.string.server_config_model), style = MaterialTheme.typography.labelMedium)
    if (availableModels.isEmpty()) {
        Text(
            stringResource(R.string.server_config_no_models),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        modelDropdownExpanded = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedModel?.name
                            ?: stringResource(R.string.server_config_select_model),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        FeatherIcons.ChevronDown,
                        contentDescription = stringResource(R.string.server_config_select_model),
                    )
                }
            }
            DropdownMenu(
                expanded = modelDropdownExpanded,
                onDismissRequest = { modelDropdownExpanded = false },
            ) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.name) },
                        onClick = {
                            onModelSelected(model)
                            modelDropdownExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkBindingSelector(
    selectedHost: String,
    enabled: Boolean,
    onHostSelected: (String) -> Unit,
) {
    Text(
        stringResource(R.string.server_config_network_binding),
        style = MaterialTheme.typography.labelMedium,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedHost == "0.0.0.0",
            onClick = { if (enabled) onHostSelected("0.0.0.0") },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    stringResource(R.string.server_config_binding_lan),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
        FilterChip(
            selected = selectedHost == "127.0.0.1",
            onClick = { if (enabled) onHostSelected("127.0.0.1") },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    stringResource(R.string.server_config_binding_localhost),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
private fun ServerRuntimeParametersFields(
    portText: String,
    threadsText: String,
    contextSizeText: String,
    enabled: Boolean,
    onPortChanged: (String) -> Unit,
    onThreadsChanged: (String) -> Unit,
    onContextSizeChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = portText,
            onValueChange = onPortChanged,
            label = { Text(stringResource(R.string.server_config_port), maxLines = 1) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = threadsText,
            onValueChange = onThreadsChanged,
            label = { Text(stringResource(R.string.server_config_cpu_threads), maxLines = 1) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }

    OutlinedTextField(
        value = contextSizeText,
        onValueChange = onContextSizeChanged,
        label = { Text(stringResource(R.string.server_config_context_size), maxLines = 1) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ServerEndpointsCard(
    baseUrl: String,
    openApiUrl: String,
    curlCommand: String,
    onCopy: (label: String, text: String) -> Unit,
) {
    val baseUrlLabel = stringResource(R.string.server_endpoint_base_url)
    val openApiLabel = stringResource(R.string.server_endpoint_openapi_spec)
    val curlLabel = stringResource(R.string.server_curl_command_label)

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.server_endpoints_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            EndpointRow(
                label = baseUrlLabel,
                value = baseUrl,
                onCopy = { onCopy(baseUrlLabel, baseUrl) },
            )
            EndpointRow(
                label = openApiLabel,
                value = openApiUrl,
                onCopy = { onCopy(openApiLabel, openApiUrl) },
            )
            HorizontalDivider()
            CurlExampleSection(
                curlCommand = curlCommand,
                onCopy = { onCopy(curlLabel, curlCommand) },
            )
        }
    }
}

@Composable
private fun EndpointRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Medium,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                FeatherIcons.Copy,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CurlExampleSection(
    curlCommand: String,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.server_curl_example_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                FeatherIcons.Copy,
                contentDescription = stringResource(R.string.server_curl_command_label),
                modifier = Modifier.size(16.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(8.dp),
            )
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = curlCommand,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
        )
    }
}

@Composable
private fun ServerLogsCard(
    logs: List<ServerRequestLog>,
    onClearLogs: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.server_logs_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (logs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs) {
                        Text(stringResource(R.string.server_logs_clear))
                    }
                }
            }

            if (logs.isEmpty()) {
                Text(
                    stringResource(R.string.server_logs_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                logs.forEach { log ->
                    ServerLogItem(log = log)
                }
            }
        }
    }
}

@Composable
private fun ServerLogItem(log: ServerRequestLog) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "[${log.timestamp}] ${log.method} ${log.path} (${log.status})",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = log.details,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewManageServerScreen() {
    ManageServerScreen(
        uiState = ManageServerUIState(
            serverState = OpenAIServerState(
                isRunning = false,
                localIpAddress = "192.168.1.24",
                logs = listOf(
                    ServerRequestLog(
                        timestamp = "21:30:10",
                        method = "POST",
                        path = "/v1/chat/completions",
                        status = 200,
                        details = "tokens=42, 18.4 tok/s",
                    )
                ),
            ),
            availableModels = dummyLLMModels,
        ),
        onStartServer = {},
        onStopServer = {},
        onUpdateConfig = {},
        onClearLogs = {},
        onBackClick = {},
    )
}

private fun buildServerConfig(
    base: ServerConfig,
    portText: String,
    host: String,
    modelId: Long,
    contextSizeText: String,
    threadsText: String,
): ServerConfig {
    return base.copy(
        port = (portText.toIntOrNull() ?: 8080).coerceIn(1024, 65535),
        host = host,
        modelId = modelId,
        contextSize = (contextSizeText.toLongOrNull() ?: 2048L).coerceAtLeast(256L),
        numThreads = (threadsText.toIntOrNull() ?: 4).coerceIn(1, 16),
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(
        context,
        context.getString(R.string.server_toast_copied, label),
        Toast.LENGTH_SHORT,
    ).show()
}
