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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.shubham0204.smollmandroid.data.Task
import io.shubham0204.smollmandroid.data.TasksDB
import io.shubham0204.smollmandroid.llm.ModelsRepository
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TasksViewModel(
    val modelsRepository: ModelsRepository,
    val tasksDB: TasksDB,
) : ViewModel() {
    val showTaskOptionsPopupState = mutableStateOf(false)
    val showCreateTaskDialogState = mutableStateOf(false)
    val showEditTaskDialogState = mutableStateOf(false)
    val selectedTaskState = mutableStateOf<Task?>(null)

    fun addTask(
        name: String,
        systemPrompt: String,
        modelId: Long,
    ) {
        tasksDB.addTask(name, systemPrompt, modelId)
    }

    fun updateTask(newTask: Task) {
        tasksDB.updateTask(newTask)
    }

    fun deleteTask(taskId: Long) {
        tasksDB.deleteTask(taskId)
    }
}