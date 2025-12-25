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

import androidx.compose.runtime.Stable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "LLMModel")
@Stable
@Serializable
data class LLMModel(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var name: String = "",
    var url: String = "",
    var path: String = "",
    var contextSize: Int = 0,
    var chatTemplate: String = "",
)

@Dao
interface LLMModelDao {
    @Query("SELECT * FROM LLMModel")
    fun getAllModels(): Flow<List<LLMModel>>

    @Query("SELECT * FROM LLMModel")
    suspend fun getAllModelsList(): List<LLMModel>

    @Query("SELECT * FROM LLMModel WHERE id = :id")
    suspend fun getModel(id: Long): LLMModel

    @Insert
    suspend fun insertModels(vararg models: LLMModel)

    @Query("DELETE FROM LLMModel WHERE id = :id")
    suspend fun deleteModel(id: Long)
}
