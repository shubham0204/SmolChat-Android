package io.shubham0204.smollmandroid.ui.screens.manage_asr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.Download
import compose.icons.feathericons.Play
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.androidx.compose.koinViewModel

class ManageASRActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: ManageASRViewModel = koinViewModel()
            val uiState by model.uiState.collectAsStateWithLifecycle()
            ScreenUI(uiState, model::onEvent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ScreenUI(uiState: ManageASRUIState, onEvent: (ManageASRUIEvent) -> Unit) {
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { AppBarTitleText("Manage Speech-to-Text") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()) {
                    val launcher =
                        rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) {
                            if (it) {
                                onEvent(ManageASRUIEvent.EnableSpeech2Text)
                            } else {
                                Toast.makeText(
                                    this@ManageASRActivity,
                                    "You need permission to record audio to enable speech-to-text",
                                    Toast.LENGTH_LONG,
                                )
                                    .show()
                            }
                        }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "Speech to Text",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Enable/disable speech-to-text globally",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = {
                                if (it) {
                                    if (checkAudioRecordingPermission()) {
                                        onEvent(ManageASRUIEvent.EnableSpeech2Text)
                                    } else {
                                        launcher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    onEvent(ManageASRUIEvent.DisableSpeech2Text)
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Available Models",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(uiState.modelsList) { uiASRModel ->
                            ASRModelItem(uiASRModel, onEvent)
                        }
                    }

                    if (uiState.downloadingModelProgress != null) {
                        DownloadProgressDialog(uiState.downloadingModelProgress)
                    }
                }
            }
        }
    }

    @Composable
    private fun ASRModelItem(asrModel: UIASRModel, onEvent: (ManageASRUIEvent) -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (asrModel.isEnabled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                ),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        asrModel.model.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (asrModel.isDownloaded) {
                        Text(
                            if (asrModel.isEnabled) "Currently active" else "Downloaded",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (asrModel.isEnabled) MaterialTheme.colorScheme.primary
                                else Color.Gray,
                        )
                    } else {
                        Text(
                            "Not downloaded",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                        )
                    }
                }

                if (asrModel.isDownloaded) {
                    if (asrModel.isEnabled) {
                        Icon(
                            FeatherIcons.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButton(
                            onClick = {
                                onEvent(ManageASRUIEvent.EnableSpeech2TextModel(asrModel.model))
                            }
                        ) {
                            Icon(FeatherIcons.Play, contentDescription = "Use Model")
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            onEvent(ManageASRUIEvent.DownloadSpeech2TextModel(asrModel.model))
                        }
                    ) {
                        Icon(FeatherIcons.Download, contentDescription = "Download Model")
                    }
                }
            }
        }
    }

    @Composable
    private fun DownloadProgressDialog(progress: Int) {
        Dialog(onDismissRequest = {}) {
            Card(shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Downloading Model...", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$progress%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    private fun checkAudioRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    @Preview
    @Composable
    private fun PreviewScreenUI() {
        ScreenUI(
            uiState =
                ManageASRUIState(
                    isEnabled = true,
                    modelsList =
                        listOf(
                            UIASRModel(
                                model =
                                    ASRModel(
                                        "tiny-en",
                                        "Tiny Model - English",
                                        "",
                                        "",
                                        ASRModelArch.TINY,
                                    ),
                                isEnabled = true,
                                isDownloaded = true,
                            ),
                            UIASRModel(
                                model =
                                    ASRModel(
                                        "base-zh",
                                        "Base Model - Chinese",
                                        "",
                                        "",
                                        ASRModelArch.BASE,
                                    ),
                                isEnabled = false,
                                isDownloaded = false,
                            ),
                        ),
                ),
            onEvent = {},
        )
    }
}
