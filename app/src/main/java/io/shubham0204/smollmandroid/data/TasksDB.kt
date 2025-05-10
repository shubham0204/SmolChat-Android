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

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "Task")
data class Task(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var name: String = "",
    var systemPrompt: String = "",
    var modelId: Long = -1,
    var shortcutId: String? = null,
    @Transient var modelName: String = "",
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM Task WHERE id = :taskId")
    suspend fun getTask(taskId: Long): Task

    @Query("SELECT * FROM Task")
    fun getTasks(): Flow<List<Task>>

    @Insert
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM Task WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)
}
