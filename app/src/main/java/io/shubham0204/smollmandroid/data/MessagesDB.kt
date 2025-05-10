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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ChatMessage")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var chatId: Long = 0,
    var message: String = "",
    var isUserMessage: Boolean = false,
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM ChatMessage WHERE chatId = :chatId")
    fun getMessages(chatId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM ChatMessage WHERE chatId = :chatId")
    suspend fun getMessagesForModel(chatId: Long): List<ChatMessage>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM ChatMessage WHERE chatId = :chatId")
    suspend fun deleteMessages(chatId: Long)

    @Query("DELETE FROM ChatMessage WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)
}
