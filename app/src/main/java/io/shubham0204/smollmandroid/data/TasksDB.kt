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

package io.shubham0204.smollmandroid.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Single

@Entity
data class Task(
    @Id var id: Long = 0,
    var name: String = "",
    var systemPrompt: String = "",
    var modelId: Long = -1,
    @Transient var modelName: String = "",
)

@Single
class TasksDB {
    private val tasksBox = ObjectBoxStore.store.boxFor(Task::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTasks(): Flow<List<Task>> =
        tasksBox
            .query()
            .build()
            .flow()
            .flowOn(Dispatchers.IO)

    fun addTask(
        name: String,
        systemPrompt: String,
        modelId: Long,
    ) {
        tasksBox.put(Task(name = name, systemPrompt = systemPrompt, modelId = modelId))
    }

    fun deleteTask(taskId: Long) {
        tasksBox.remove(taskId)
    }

    fun updateTask(task: Task) {
        tasksBox.put(task)
    }
}
