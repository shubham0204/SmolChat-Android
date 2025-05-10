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

import android.util.Log
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

private const val LOGTAG = "[ChatDB-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@Entity(tableName = "Chat")
data class Chat(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    /**
     * Name of the chat, as shown in the UI
     * This is editable by users in the EditChatSettingsScreen.kt
     * When a new chat is created, its name is set to "Untitled [x+1]"
     * where x is the number of chats created so far in the DB.
     */
    var name: String = "",
    /**
     * System prompt for the model that is selected for this chat.
     * It defines the overall tone and flow of the conversation.
     * This is editable by users in the EditChatSettingsScreen.kt.
     */
    var systemPrompt: String = "",
    /**
     * [dateUsed] is updated every time the chat is used in the app.
     * [dateCreated] is set when the chat is created for the first time.
     */
    var dateCreated: Date = Date(),
    var dateUsed: Date = Date(),
    /**
     * The ID of the [LLMModel] currently being used for this chat.
     * A model with this ID is loaded when the user selects this chat.
     */
    var llmModelId: Long = -1L,
    /**
     * LLM inference parameters that are used for this chat.
     */
    var minP: Float = 0.05f,
    var temperature: Float = 1.0f,
    var nThreads: Int = 4,
    var useMmap: Boolean = true,
    var useMlock: Boolean = false,
    /**
     * The maximum number of tokens that can be used as context to the model
     * This is editable by users in the EditChatSettingsScreen.kt.
     * Its initial value is taken from the GGUF model selected by the user.
     */
    var contextSize: Int = 0,
    /**
     * The number of tokens that have been used as context in the current chat session
     */
    var contextSizeConsumed: Int = 0,
    /**
     * The template that is used to format the chat messages.
     * This is editable by users in the EditChatSettingsScreen.kt
     */
    var chatTemplate: String = "",
    /**
     * Whether this chat is a task or not.
     * Tasks are special chats that are used to perform a specific task.
     * They do not store conversation messages thus being 'stateless' in nature.
     */
    var isTask: Boolean = false,
    /**
     * The ID of the folder that this chat belongs to.
     * -1 indicates that the chat does not belong to any folder.
     */
    var folderId: Long = -1L,
)

@Dao
interface ChatsDao {
    @Query("SELECT * FROM Chat ORDER BY dateUsed DESC")
    fun getChats(): Flow<List<Chat>>

    @Insert
    suspend fun insertChat(chat: Chat): Long

    @Query("SELECT * FROM Chat ORDER BY dateUsed DESC LIMIT 1")
    suspend fun getRecentlyUsedChat(): Chat?

    @Query("DELETE FROM Chat WHERE id = :chatId")
    suspend fun deleteChat(chatId: Long)

    @Query("DELETE FROM Chat WHERE folderId = :folderId")
    suspend fun deleteChatsInFolder(folderId: Long)

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("SELECT COUNT(*) FROM Chat")
    suspend fun getChatsCount(): Long

    @Query("SELECT * FROM Chat WHERE folderId = :folderId")
    fun getChatsForFolder(folderId: Long): Flow<List<Chat>>

    @Query("UPDATE Chat SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    fun updateFolderIds(
        oldFolderId: Long,
        newFolderId: Long,
    )
}
