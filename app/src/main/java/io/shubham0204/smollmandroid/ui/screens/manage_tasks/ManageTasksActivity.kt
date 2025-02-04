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

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.unit.dp
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.LargeLabelText
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.androidx.compose.koinViewModel

class ManageTasksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TasksActivityScreenUI() }
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
                    title = { AppBarTitleText(text = "Manage Tasks") },
                    actions = {
                        IconButton(
                            onClick = { viewModel.showCreateTaskDialogState.value = true },
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add New Task")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { (context as ManageTasksActivity).finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
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
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                val tasks by viewModel.tasksDB.getTasks().collectAsState(emptyList())
                Text(
                    text =
                        "Tasks are chat templates with a system prompt and model defined. Use them to perform quick " +
                            "actions with the selected SLM model.",
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
                    onEditTaskClick = { task ->
                        viewModel.selectedTaskState.value = task
                        viewModel.showEditTaskDialogState.value = true
                    },
                    onDeleteTaskClick = { task ->
                        createAlertDialog(
                            dialogTitle = "Delete Task",
                            dialogText = "Are you sure you want to delete task '${task.name}'?",
                            dialogPositiveButtonText = "Delete",
                            dialogNegativeButtonText = "Cancel",
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
        Column(modifier = Modifier
            .weight(1f)
            .padding(4.dp)
            .padding(8.dp)) {
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
                        Icons.Default.MoreVert,
                        contentDescription = "More Options",
                    )
                }
                if (showTaskOptionsPopup) {
                    TaskOptionsPopup(
                        onDismiss = { showTaskOptionsPopup = false },
                        onDeleteTaskClick = {
                            onDeleteTaskClick()
                            showTaskOptionsPopup = false
                        },
                        onEditTaskClick = {
                            onEditTaskClick()
                            showTaskOptionsPopup = false
                        },
                    )
                }
            }
        }
    }
}
