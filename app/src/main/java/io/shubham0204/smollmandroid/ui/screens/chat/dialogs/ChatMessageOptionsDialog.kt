package io.shubham0204.smollmandroid.ui.screens.chat.dialogs

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clipboard
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Share
import io.shubham0204.smollmandroid.R

private lateinit var onDialogCopyClick: (() -> Unit)
private lateinit var onDialogShareClick: (() -> Unit)
private lateinit var onDialogEditClick: (() -> Unit)
private val dialogVisibleStatus = mutableStateOf(false)
private var dialogShowChatMessageEditOption = false

@Composable
fun ChatMessageOptionsDialog() {
    var visible by remember { dialogVisibleStatus }
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
                            .padding(16.dp)
                ) {
                    ChatMessageOption(
                        titleStringResId = R.string.chat_message_copy,
                        icon = FeatherIcons.Clipboard,
                        onClick = {
                            visible = false
                            onDialogCopyClick()
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChatMessageOption(
                        titleStringResId = R.string.chat_message_share,
                        icon = FeatherIcons.Share,
                        onClick = {
                            visible = false
                            onDialogShareClick()
                        },
                    )
                    if (dialogShowChatMessageEditOption) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ChatMessageOption(
                            titleStringResId = R.string.dialog_edit_folder_button_text,
                            icon = FeatherIcons.Edit2,
                            onClick = {
                                visible = false
                                onDialogEditClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageOption(titleStringResId: Int, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.surfaceTint)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            stringResource(titleStringResId),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

fun createChatMessageOptionsDialog(
    onCopyClick: (() -> Unit),
    onShareClick: (() -> Unit),
    showEditOption: Boolean,
    onEditClick: (() -> Unit) = {},
) {
    onDialogCopyClick = onCopyClick
    onDialogShareClick = onShareClick
    onDialogEditClick = onEditClick
    dialogShowChatMessageEditOption = showEditOption
    dialogVisibleStatus.value = true
}
