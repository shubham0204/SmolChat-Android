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

package io.shubham0204.smollmandroid.ui.screens.chat

import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri

@Composable
fun ManageDocsDialog(viewModel: ChatScreenViewModel) {
    Surface {
        Dialog(onDismissRequest = { viewModel.hideManageDocsDialog() }) {
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                    activityResult.data?.let {
                        it.data?.let { uri ->
                        }
                    }
                }
            Column {
                Button(onClick = {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            setType("application/pdf")
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI,
                                Environment
                                    .getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOCUMENTS,
                                    ).toUri(),
                            )
                        }
                    launcher.launch(intent)
                }) {
                    Text("Add Document")
                }
            }
        }
    }
}

@Composable
private fun DocsList(viewModel: ChatScreenViewModel) {
    // val documents by viewModel.getDocuments().collectAsState(initial = emptyList())
}
