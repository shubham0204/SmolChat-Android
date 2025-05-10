package io.shubham0204.smollmandroid.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.Folder

@Composable
fun ChangeFolderDialogUI(
    onDismissRequest: () -> Unit,
    chat: Chat,
    folders: List<Folder>,
    onUpdateFolderId: (Long) -> Unit,
) {
    Surface {
        Dialog(onDismissRequest) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                        .padding(16.dp),
            ) {
                LazyColumn {
                    items(folders) { folder ->
                        val selected = folder.id == chat.folderId
                        Row(
                            modifier =
                                Modifier.clickable {
                                    onUpdateFolderId(folder.id)
                                },
                        ) {
                            RadioButton(selected = selected, onClick = { })
                            Text(folder.name)
                        }
                    }
                }
                OutlinedButton(onClick = onDismissRequest) { Text("Close") }
            }
        }
    }
}
