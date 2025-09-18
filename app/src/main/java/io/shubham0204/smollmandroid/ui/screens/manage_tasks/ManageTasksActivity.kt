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

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Plus
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.LargeLabelText
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.androidx.compose.koinViewModel

class ManageTasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(modifier = Modifier.safeDrawingPadding()) {
                TasksActivityScreenUI()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksActivityScreenUI() {
    val viewModel: TasksViewModel = koinViewModel()
    val context = LocalContext.current
    SmolLMAndroidTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { AppBarTitleText(text = stringResource(R.string.tasks_manage_tasks_title)) },
                    actions = {
                        IconButton(
                            onClick = { viewModel.showCreateTaskDialogState.value = true },
                        ) {
                            Icon(FeatherIcons.Plus, contentDescription = "Add New Task")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { (context as ManageTasksActivity).finish() }) {
                            Icon(
                                FeatherIcons.ArrowLeft,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                val tasks by viewModel.appDB.getTasks().collectAsState(emptyList())
                Text(
                    text = stringResource(R.string.tasks_manage_tasks_desc),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TasksList(
                    tasks.map {
                        val modelName =
                            viewModel.modelsRepository.getModelFromId(it.modelId)?.name
                                ?: return@map it
                        it.copy(modelName = modelName)
                    },
                    onTaskSelected = { /* Not applicable as enableTaskClick is set to `false` */ },
                    onUpdateTaskClick = { task ->
                        viewModel.updateTask(task)
                    },
                    onEditTaskClick = { task ->
                        viewModel.selectedTaskState.value = task
                        viewModel.showEditTaskDialogState.value = true
                    },
                    onDeleteTaskClick = { task ->
                        createAlertDialog(
                            dialogTitle = context.getString(R.string.dialog_delete_task_title),
                            dialogText = "Are you sure you want to delete task '${task.name}'?",
                            dialogPositiveButtonText = context.getString(R.string.dialog_pos_delete),
                            dialogNegativeButtonText = context.getString(R.string.dialog_neg_cancel),
                            onPositiveButtonClick = {
                                viewModel.deleteTask(task.id)
                                Toast
                                    .makeText(
                                        context,
                                        "Task '${task.name}' deleted",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            },
                            onNegativeButtonClick = {},
                        )
                    },
                    enableTaskClick = false,
                    showTaskOptions = true,
                )

                CreateTaskDialog(viewModel)
                EditTaskDialog(viewModel)
                AppAlertDialog()
            }
        }
    }
}

@Composable
fun TasksList(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    onEditTaskClick: (Task) -> Unit,
    onDeleteTaskClick: (Task) -> Unit,
    onUpdateTaskClick: (Task) -> Unit,
    enableTaskClick: Boolean,
    showTaskOptions: Boolean,
) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(
                task,
                onTaskSelected = { onTaskSelected(task) },
                onDeleteTaskClick = { onDeleteTaskClick(task) },
                onEditTaskClick = { onEditTaskClick(task) },
                onUpdateTask = { onUpdateTaskClick(it) },
                enableTaskClick,
                showTaskOptions,
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onTaskSelected: () -> Unit,
    onDeleteTaskClick: () -> Unit,
    onEditTaskClick: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    enableTaskClick: Boolean = false,
    showTaskOptions: Boolean = true,
) {
    Row(
        modifier =
            if (enableTaskClick) {
                Modifier
                    .fillMaxWidth()
                    .clickable { onTaskSelected() }
            } else {
                Modifier.fillMaxWidth()
            }.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .padding(8.dp),
        ) {
            LargeLabelText(text = task.name)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.systemPrompt,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = task.modelName,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (showTaskOptions) {
            Box {
                var showTaskOptionsPopup by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showTaskOptionsPopup = true },
                ) {
                    Icon(
                        FeatherIcons.MoreVertical,
                        contentDescription = "More Options",
                    )
                }
                if (showTaskOptionsPopup) {
                    TaskOptionsPopup(
                        task.shortcutId != null,
                        onDismiss = { showTaskOptionsPopup = false },
                        onDeleteTaskClick = {
                            task.shortcutId?.let {
                                ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(it))
                                onUpdateTask(task.copy(shortcutId = null))
                                Toast.makeText(context, "Shortcut for task '${task.name}' removed", Toast.LENGTH_LONG).show()
                            }
                            onDeleteTaskClick()
                            showTaskOptionsPopup = false
                        },
                        onEditTaskClick = {
                            onEditTaskClick()
                            showTaskOptionsPopup = false
                        },
                        onAddTaskShortcut = {
                            val shortcut =
                                ShortcutInfoCompat
                                    .Builder(context, "${task.id}")
                                    .setShortLabel(task.name)
                                    .setIcon(IconCompat.createWithResource(context, R.drawable.task_shortcut_icon))
                                    .setIntent(
                                        Intent(context, ChatActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            putExtra("task_id", task.id)
                                        },
                                    ).build()
                            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
                            Toast.makeText(context, "Shortcut for task '${task.name}' added", Toast.LENGTH_LONG).show()
                            onUpdateTask(task.copy(shortcutId = shortcut.id))
                            showTaskOptionsPopup = false
                        },
                        onRemoveTaskShortcut = {
                            task.shortcutId?.let {
                                ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(it))
                                onUpdateTask(task.copy(shortcutId = null))
                                Toast.makeText(context, "Shortcut for task '${task.name}' removed", Toast.LENGTH_LONG).show()
                            }
                            showTaskOptionsPopup = false
                        },
                    )
                }
            }
        }
    }
}
