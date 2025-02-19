/*
 * Copyright (C) 2025 Shubham Panchal
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

package io.shubham0204.smollmandroid.data.docs

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.flow
import io.shubham0204.smollmandroid.data.ObjectBoxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Single
import java.util.Date

@Entity
data class Document(
    @Id var id: Long = 0,
    var chatId: Long = 0,
    var name: String = "",
    var fileSize: Long = 0,
    var dateAdded: Date = Date(),
)

@Single
class DocumentsDB {
    private val documentsBox = ObjectBoxStore.store.boxFor(Document::class.java)

    fun addDocument(
        chatId: Long,
        name: String,
        fileSize: Long,
    ) {
        val document = Document(chatId = chatId, name = name, fileSize = fileSize)
        documentsBox.put(document)
    }

    fun removeDocument(documentId: Long) {
        documentsBox.remove(documentId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDocumentsForChat(chatId: Long) =
        documentsBox
            .query()
            .equal(Document_.chatId, chatId)
            .orderDesc(Document_.chatId)
            .build()
            .flow()
            .flowOn(Dispatchers.IO)

    fun removeDocumentsForChat(chatId: Long) {
        with(documentsBox.query().equal(Document_.chatId, chatId).build()) {
            remove()
        }
    }
}
