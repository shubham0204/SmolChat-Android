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
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Single
import java.util.Date

private const val LOGTAG = "[ChatDB-Kt]"
private val LOGD: (String) -> Unit = { Log.d(LOGTAG, it) }

@Entity
data class Chat(
    @Id var id: Long = 0,
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
)

@Single
class ChatsDB {
    private val chatsBox = ObjectBoxStore.store.boxFor(Chat::class.java)

    /** Get all chats from the database sorted by dateUsed in descending order. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChats(): Flow<List<Chat>> =
        chatsBox
            .query()
            .orderDesc(Chat_.dateUsed)
            .build()
            .flow()
            .flowOn(Dispatchers.IO)

    fun loadDefaultChat(): Chat {
        val defaultChat =
            if (getChatsCount() == 0L) {
                addChat("Untitled")
                getRecentlyUsedChat()!!
            } else {
                // Given that chatsDB has at least one chat
                // chatsDB.getRecentlyUsedChat() will never return null
                getRecentlyUsedChat()!!
            }
        LOGD("Default chat is $defaultChat")
        return defaultChat
    }

    /**
     * Get the most recently used chat from the database. This function might return null, if there
     * are no chats in the database.
     */
    fun getRecentlyUsedChat(): Chat? =
        chatsBox
            .query()
            .orderDesc(Chat_.dateUsed)
            .build()
            .findFirst()

    /**
     * Adds a new chat to the database initialized with given
     * arguments and returns the new Chat object
     */
    fun addChat(
        chatName: String,
        chatTemplate: String = "",
        systemPrompt: String = "You are a helpful assistant.",
        llmModelId: Long = -1,
        isTask: Boolean = false,
    ): Chat {
        val newChat =
            Chat(
                name = chatName,
                systemPrompt = systemPrompt,
                dateCreated = Date(),
                dateUsed = Date(),
                llmModelId = llmModelId,
                contextSize = 2048,
                chatTemplate = chatTemplate,
                isTask = isTask,
            )
        val newChatId = chatsBox.put(newChat)
        return newChat.copy(id = newChatId)
    }

    /** Update the chat in the database. ObjectBox overwrites the entry if it already exists. */
    fun updateChat(modifiedChat: Chat) {
        chatsBox.put(modifiedChat)
    }

    fun deleteChat(chat: Chat) {
        chatsBox.remove(chat)
    }

    fun getChatsCount(): Long = chatsBox.count()
}
