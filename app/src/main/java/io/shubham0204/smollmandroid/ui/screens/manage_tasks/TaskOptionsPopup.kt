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

package io.shubham0204.smollmandroid.ui.screens.manage_tasks

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import compose.icons.FeatherIcons
import compose.icons.feathericons.Delete
import compose.icons.feathericons.Edit
import compose.icons.feathericons.Plus
import io.shubham0204.smollmandroid.R

@Composable
fun TaskOptionsPopup(
    isTaskShortcut: Boolean,
    onDismiss: () -> Unit,
    onEditTaskClick: () -> Unit,
    onDeleteTaskClick: () -> Unit,
    onAddTaskShortcut: () -> Unit,
    onRemoveTaskShortcut: () -> Unit,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { onDismiss() },
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(FeatherIcons.Edit, contentDescription = "Edit Task") },
            text = { Text(stringResource(R.string.task_popup_edit_task)) },
            onClick = { onEditTaskClick() },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(FeatherIcons.Delete, contentDescription = "Delete Task") },
            text = { Text(stringResource(R.string.task_popup_delete_task)) },
            onClick = { onDeleteTaskClick() },
        )
        if (isTaskShortcut) {
            DropdownMenuItem(
                leadingIcon = { Icon(FeatherIcons.Delete, contentDescription = "Remove Task Shortcut") },
                text = { Text(stringResource(R.string.task_popup_remove_shortcut)) },
                onClick = { onRemoveTaskShortcut() },
            )
        } else {
            DropdownMenuItem(
                leadingIcon = { Icon(FeatherIcons.Plus, contentDescription = "Add Task Shortcut") },
                text = { Text(stringResource(R.string.task_popup_add_shortcut)) },
                onClick = { onAddTaskShortcut() },
            )
        }
    }
}
