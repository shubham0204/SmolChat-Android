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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Plus
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.data.LLMModel
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.ui.components.AppAlertDialog
import io.shubham0204.smollmandroid.ui.components.AppBarTitleText
import io.shubham0204.smollmandroid.ui.components.TasksList
import io.shubham0204.smollmandroid.ui.components.createAlertDialog
import io.shubham0204.smollmandroid.ui.preview.dummyLLMModels
import io.shubham0204.smollmandroid.ui.preview.dummyTasksList
import io.shubham0204.smollmandroid.ui.theme.SmolLMAndroidTheme
import org.koin.android.ext.android.inject

class ManageTasksActivity : ComponentActivity() {
    private val viewModel: TasksViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(modifier = Modifier.safeDrawingPadding()) {
                val tasks by viewModel.appDB.getTasks().collectAsState(emptyList())
                val models by viewModel.appDB.getModels().collectAsState(emptyList())
                TasksActivityScreenUI(
                    tasks =
                        tasks.map {
                            val modelName =
                                viewModel.modelsRepository.getModelFromId(it.modelId).name
                            it.copy(modelName = modelName)
                        },
                    availableModelsList = models,
                    getModelFromId = viewModel.modelsRepository::getModelFromId,
                    onUpdateTask = viewModel::updateTask,
                    onAddTask = viewModel::addTask,
                    onDeleteTask = viewModel::deleteTask,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTasksActivityScreenUI() {
    TasksActivityScreenUI(
        tasks = dummyTasksList,
        availableModelsList = dummyLLMModels,
        getModelFromId = { id -> dummyLLMModels.first { it.id == id } },
        onUpdateTask = {},
        onAddTask = { _, _, _ -> },
        onDeleteTask = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksActivityScreenUI(
    tasks: List<Task>,
    availableModelsList: List<LLMModel>,
    getModelFromId: (Long) -> LLMModel,
    onAddTask: (String, String, Long) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onDeleteTask: (Long) -> Unit,
) {
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    val context = LocalContext.current
    SmolLMAndroidTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        AppBarTitleText(text = stringResource(R.string.tasks_manage_tasks_title))
                    },
                    actions = {
                        IconButton(onClick = { showCreateTaskDialog = true }) {
                            Icon(FeatherIcons.Plus, contentDescription = "Add New Task")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { (context as ManageTasksActivity).finish() }) {
                            Icon(FeatherIcons.ArrowLeft, contentDescription = "Navigate Back")
                        }
                    },
                )
            }
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize()
                        .padding(paddingValues)
            ) {
                Text(
                    text = stringResource(R.string.tasks_manage_tasks_desc),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TasksList(
                    tasks,
                    onTaskSelected = { /* Not applicable as enableTaskClick is set to `false` */ },
                    onUpdateTaskClick = { task -> onUpdateTask(task) },
                    onEditTaskClick = { task ->
                        selectedTask = task
                        showEditTaskDialog = true
                    },
                    onDeleteTaskClick = { task ->
                        createAlertDialog(
                            dialogTitle = context.getString(R.string.dialog_delete_task_title),
                            dialogText = "Are you sure you want to delete task '${task.name}'?",
                            dialogPositiveButtonText =
                                context.getString(R.string.dialog_pos_delete),
                            dialogNegativeButtonText =
                                context.getString(R.string.dialog_neg_cancel),
                            onPositiveButtonClick = {
                                onDeleteTask(task.id)
                                Toast.makeText(
                                        context,
                                        "Task '${task.name}' deleted",
                                        Toast.LENGTH_LONG,
                                )
                                    .show()
                            },
                            onNegativeButtonClick = {},
                        )
                    },
                    enableTaskClick = false,
                    showTaskOptions = true,
                )
                if (showCreateTaskDialog) {
                    CreateTaskDialog(
                        availableModelsList = availableModelsList,
                        onDismiss = { showCreateTaskDialog = false },
                        onAddTask = onAddTask,
                    )
                }
                if (showEditTaskDialog && selectedTask != null) {
                    EditTaskDialog(
                        selectedTask = selectedTask!!,
                        selectedTaskModel = getModelFromId(selectedTask!!.modelId),
                        availableModelsList = availableModelsList,
                        onDismiss = { showEditTaskDialog = false },
                        onUpdateTask = onUpdateTask,
                    )
                }
                AppAlertDialog()
            }
        }
    }
}
