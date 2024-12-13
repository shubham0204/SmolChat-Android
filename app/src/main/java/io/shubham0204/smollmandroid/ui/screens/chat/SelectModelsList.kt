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

package io.shubham0204.smollmandroid.ui.screens.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.ui.components.DialogTitleText
import io.shubham0204.smollmandroid.ui.components.SmallLabelText
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.screens.model_download.DownloadModelActivity
import io.shubham0204.smollmandroid.ui.theme.AppAccentColor
import io.shubham0204.smollmandroid.ui.theme.AppFontFamily
import java.io.File

@Composable
fun SelectModelsList(
    onDismissRequest: () -> Unit,
    modelsList: List<LLMModel>,
    onModelListItemClick: (LLMModel) -> Unit,
    onModelDeleteClick: (LLMModel) -> Unit,
    showModelDeleteIcon: Boolean = true,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogTitleText(text = "Choose Model")
            SmallLabelText(
                "Select a downloaded model from below to use as a 'default' model for this chat. You will " +
                    "be able to change it later by clicking ⫶ on the app bar.",
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(modelsList) {
                    ModelListItem(
                        model = it,
                        onModelListItemClick,
                        onModelDeleteClick,
                        showModelDeleteIcon,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    Intent(context, DownloadModelActivity::class.java).also {
                        it.putExtra("openChatScreen", false)
                        context.startActivity(it)
                    }
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add model", tint = AppAccentColor)
                Text("Add model", fontFamily = AppFontFamily)
            }
        }
    }
}

@Composable
private fun ModelListItem(
    model: LLMModel,
    onModelListItemClick: (LLMModel) -> Unit,
    onModelDeleteClick: (LLMModel) -> Unit,
    showModelDeleteIcon: Boolean,
) {
    Row(
        modifier = Modifier.clickable { onModelListItemClick(model) }.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = model.name, fontSize = 12.sp, fontFamily = AppFontFamily, maxLines = 1)
            Text(
                text = "%.1f GB".format(File(model.path).length() / (1e+9)),
                fontSize = 10.sp,
                fontFamily = AppFontFamily,
                maxLines = 1,
            )
        }
        if (showModelDeleteIcon) {
            IconButton(
                onClick = {
                    createAlertDialog(
                        dialogTitle = "Delete Model",
                        dialogText = "Are you sure you want to delete the model '${model.name}'?",
                        dialogPositiveButtonText = "Delete",
                        dialogNegativeButtonText = "Cancel",
                        onPositiveButtonClick = { onModelDeleteClick(model) },
                        onNegativeButtonClick = {},
                    )
                },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Model",
                    tint = AppAccentColor,
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
}
