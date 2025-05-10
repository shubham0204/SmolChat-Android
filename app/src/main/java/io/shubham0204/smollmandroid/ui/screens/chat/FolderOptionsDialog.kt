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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.shubham0204.smollmandroid.R

private lateinit var onEditFolderNameClick: (() -> Unit)
private lateinit var onDeleteFolderClick: (() -> Unit)
private lateinit var onDeleteFolderWithChatsClick: (() -> Unit)
private val textFieldDialogShowStatus = mutableStateOf(false)

@Composable
fun FolderOptionsDialog() {
    var visible by remember { textFieldDialogShowStatus }
    if (visible) {
        Surface {
            Dialog(onDismissRequest = { visible = false }) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(16.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .clickable {
                                    visible = false
                                    onEditFolderNameClick()
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Folder Name",
                            tint = MaterialTheme.colorScheme.surfaceTint,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.dialog_folder_opts_edit),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .clickable {
                                    visible = false
                                    onDeleteFolderClick()
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Folder",
                            tint = MaterialTheme.colorScheme.surfaceTint,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.dialog_folder_opts_delete_folder),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier =
                            Modifier
                                .padding(8.dp)
                                .clickable {
                                    visible = false
                                    onDeleteFolderWithChatsClick()
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Folder With Chats",
                            tint = MaterialTheme.colorScheme.surfaceTint,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.dialog_folder_opts_delete_folder_chats),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

fun createFolderOptionsDialog(
    editFolderNameClick: (() -> Unit),
    deleteFolderClick: (() -> Unit),
    deleteFolderWithChatsClick: (() -> Unit),
) {
    onEditFolderNameClick = editFolderNameClick
    onDeleteFolderClick = deleteFolderClick
    onDeleteFolderWithChatsClick = deleteFolderWithChatsClick
    textFieldDialogShowStatus.value = true
}
