package io.shubham0204.smollmandroid.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Folder

@Composable
fun ChangeFolderDialogUI(
    onDismissRequest: () -> Unit,
    initialChatFolderId: Long,
    folders: List<Folder>,
    onUpdateFolderId: (Long) -> Unit,
) {
    val modifiedFolders = ArrayList(folders)
    modifiedFolders.add(0, Folder(id = -1L, name = "No Folder"))
    var selectedFolderId by remember { mutableLongStateOf(initialChatFolderId) }
    Surface {
        Dialog(onDismissRequest) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(8.dp),
                        ).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.dialog_select_folder_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn {
                    items(modifiedFolders) { folder ->
                        Row(
                            modifier =
                                Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFolderId = folder.id
                                        onUpdateFolderId(folder.id)
                                    },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = folder.id == selectedFolderId, onClick = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(folder.name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onDismissRequest) { Text(stringResource(R.string.dialog_err_close)) }
            }
        }
    }
}
