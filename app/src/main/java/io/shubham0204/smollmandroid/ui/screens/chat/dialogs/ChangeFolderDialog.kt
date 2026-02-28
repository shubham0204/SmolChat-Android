package io.shubham0204.smollmandroid.ui.screens.chat.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Folder

@Preview
@Composable
private fun PreviewChangeFolderDialogUI() {
    ChangeFolderDialogUI(
        onDismissRequest = {},
        initialChatFolderId = 0,
        folders =
            listOf(
                Folder(id = 0, "History"),
                Folder(id = 1, "Geography"),
                Folder(id = 2, "Math"),
                Folder(id = 3, "Science"),
            ),
        onUpdateFolderId = {},
    )
}

@Composable
fun ChangeFolderDialogUI(
    onDismissRequest: () -> Unit,
    initialChatFolderId: Long,
    folders: List<Folder>,
    onUpdateFolderId: (Long) -> Unit,
) {
    val modifiedFolders =
        remember(folders) { listOf(Folder(id = -1L, name = "No Folder")) + folders }
    var selectedFolderId by remember { mutableLongStateOf(initialChatFolderId) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.dialog_select_folder_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .selectableGroup()
                        .heightIn(max = 400.dp),
                ) {
                    items(modifiedFolders) { folder ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = folder.id == selectedFolderId,
                                        onClick = {
                                            selectedFolderId = folder.id
                                            onUpdateFolderId(folder.id)
                                        },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = folder.id == selectedFolderId,
                                onClick = null,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector =
                                    if (folder.id == -1L) {
                                        Icons.Default.FolderOff
                                    } else {
                                        Icons.Default.Folder
                                    },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_err_close))
            }
        },
    )
}
