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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.shubham0204.hf_model_hub_api.HFModelInfo
import io.shubham0204.hf_model_hub_api.HFModelTree
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.AppProgressDialog
import io.shubham0204.smollmandroid.ui.components.hideProgressDialog
import io.shubham0204.smollmandroid.ui.components.setProgressDialogTitle
import io.shubham0204.smollmandroid.ui.components.showProgressDialog
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import kotlin.reflect.typeOf

class DownloadModelActivity : ComponentActivity() {
    private var openChatScreen: Boolean = true
    private val viewModel: DownloadModelsViewModel by inject()

    @Serializable
    data class ViewModelRoute(
        val modelId: String,
        val modelInfo: HFModelInfo.ModelInfo,
        val modelFiles: List<HFModelTree.HFModelFile>,
    )

    @Serializable
    object HfModelSelectRoute

    @Serializable
    object DownloadModelRoute

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            Box(modifier = Modifier.safeDrawingPadding()) {
                NavHost(
                    navController = navController,
                    startDestination = DownloadModelRoute,
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                ) {
                    composable<ViewModelRoute>(
                        typeMap =
                            mapOf(
                                typeOf<HFModelInfo.ModelInfo>() to
                                        CustomNavTypes.HFModelInfoNavType,
                                typeOf<List<HFModelTree.HFModelFile>>() to
                                        CustomNavTypes.HFModelFileNavType,
                            )
                    ) { backStackEntry ->
                        val route: ViewModelRoute = backStackEntry.toRoute()
                        ViewHFModelScreen(
                            route.modelId,
                            route.modelInfo,
                            route.modelFiles,
                            onDownloadModel = { modelUrl ->
                                viewModel.downloadModelFromUrl(modelUrl)
                            },
                            onBackClicked = { navController.navigateUp() },
                        )
                    }
                    composable<HfModelSelectRoute> {
                        HFModelDownloadScreen(
                            viewModel,
                            onBackClicked = { navController.navigateUp() },
                            onModelClick = { modelId ->
                                setProgressDialogTitle("Getting Model Data")
                                showProgressDialog()
                                viewModel.fetchModelInfoAndTree(
                                    modelId,
                                    onResult = { modelInfo, modelFiles ->
                                        hideProgressDialog()
                                        navController.navigate(
                                            ViewModelRoute(modelId, modelInfo, modelFiles)
                                        )
                                    },
                                )
                            },
                        )
                    }
                    composable<DownloadModelRoute> {
                        AddNewModelScreen(
                            onHFModelSelectClick = { navController.navigate(HfModelSelectRoute) }
                        )
                    }
                }
            }
        }
        openChatScreen = intent.extras?.getBoolean("openChatScreen") ?: true
    }

    private fun openChatActivity() {
        if (openChatScreen) {
            Intent(this, ChatActivity::class.java).apply {
                startActivity(this)
                finish()
            }
        } else {
            finish()
        }
    }

    private enum class AddNewModelStep {
        ImportModel,
        DownloadModel,
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddNewModelScreen(onHFModelSelectClick: () -> Unit) {
        var addNewModelStep by remember { mutableStateOf(AddNewModelStep.DownloadModel) }
        SmolLMAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { AppBarTitleText(stringResource(R.string.add_new_model_title)) }
                    )
                },
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (addNewModelStep) {
                        AddNewModelStep.ImportModel -> {
                            ImportModelScreen(
                                onPrevSectionClick = {
                                    addNewModelStep = AddNewModelStep.DownloadModel
                                },
                                checkGGUFFile = ::checkGGUFFile,
                                copyModelFile = { modelFileUri ->
                                    viewModel.copyModelFile(
                                        modelFileUri,
                                        onComplete = { openChatActivity() },
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                            )
                        }

                        AddNewModelStep.DownloadModel -> {
                            DownloadModelScreen(
                                onHFModelSelectClick = onHFModelSelectClick,
                                onNextSectionClick = {
                                    addNewModelStep = AddNewModelStep.ImportModel
                                },
                                onDownloadModelClick = { selectedPopularModelIndex ->
                                    viewModel.downloadModelFromIndex(selectedPopularModelIndex)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                            )
                        }
                    }
                }
                AppProgressDialog()
                AppAlertDialog()
            }
        }
    }

    // check if the first four bytes of the file
    // represent the GGUF magic number
    // see:https://github.com/ggml-org/ggml/blob/master/docs/gguf.md#file-structure
    private fun checkGGUFFile(uri: Uri): Boolean {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val ggufMagicNumberBytes = ByteArray(4)
            inputStream.read(ggufMagicNumberBytes)
            return ggufMagicNumberBytes.contentEquals(byteArrayOf(71, 71, 85, 70))
        }
        return false
    }
}
