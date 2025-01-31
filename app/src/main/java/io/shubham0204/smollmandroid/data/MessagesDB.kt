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
data class ChatMessage(
    @Id var id: Long = 0,
    var chatId: Long = 0,
    var message: String = "",
    var isUserMessage: Boolean = false,
)

@Single
class MessagesDB {
    private val messagesBox = ObjectBoxStore.store.boxFor(ChatMessage::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessages(chatId: Long): Flow<List<ChatMessage>> =
        messagesBox
            .query(ChatMessage_.chatId.equal(chatId))
            .build()
            .flow()
            .flowOn(Dispatchers.IO)

    fun getMessagesForModel(chatId: Long): List<ChatMessage> = messagesBox.query(ChatMessage_.chatId.equal(chatId)).build().find()

    fun addUserMessage(
        chatId: Long,
        message: String,
    ) {
        messagesBox.put(ChatMessage(chatId = chatId, message = message, isUserMessage = true))
    }

    fun addAssistantMessage(
        chatId: Long,
        message: String,
    ) {
        messagesBox.put(ChatMessage(chatId = chatId, message = message, isUserMessage = false))
    }

    fun deleteMessages(chatId: Long) {
        messagesBox.query(ChatMessage_.chatId.equal(chatId)).build().remove()
    }
}
