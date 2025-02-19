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
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.shubham0204.smollmandroid.data.ObjectBoxStore
import org.koin.core.annotation.Single

@Entity
data class Chunk(
    @Id var id: Long = 0,
    var docId: Long = 0,
    var chatId: Long = 0,
    var text: String = "",
    @HnswIndex(dimensions = 384) var embedding: FloatArray = floatArrayOf(),
)

@Single
class ChunksDB {
    private val chunksBox = ObjectBoxStore.store.boxFor(Chunk::class.java)

    fun addChunk(
        chatId: Long,
        docId: Long,
        chunks: List<String>,
    ) {
        chunksBox.put(chunks.map { Chunk(chatId = chatId, docId = docId, text = it) })
    }

    fun getSimilarChunks(
        chatId: Long,
        docId: Long,
        queryEmbedding: FloatArray,
        numNeighbors: Int,
    ) {
        with(
            chunksBox
                .query(
                    Chunk_.chatId.equal(chatId).and(Chunk_.docId.equal(docId)).and(
                        Chunk_.embedding.nearestNeighbors(
                            queryEmbedding,
                            numNeighbors,
                        ),
                    ),
                ).build(),
        ) {
            find()
        }
    }
}
