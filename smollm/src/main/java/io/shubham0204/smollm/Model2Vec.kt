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

package io.shubham0204.smollm

class Model2Vec(
    private val embeddingsPath: String,
    private val tokenizerPath: String,
) {
    private var nativeHandle: Long = 0L

    companion object {
        init {
            System.loadLibrary("model2vec")
        }
    }

    init {
        nativeHandle = create(embeddingsPath, tokenizerPath)
    }

    fun encode(sequences: List<String>): List<FloatArray> {
        sequences.forEach { addSeqBuffer(nativeHandle, it) }
        val embeddings = encode(nativeHandle, Runtime.getRuntime().availableProcessors())
        clearSeqBuffer(nativeHandle)
        return embeddings.toList()
    }

    private external fun create(
        embeddingsPath: String,
        tokenizerPath: String,
    ): Long

    private external fun addSeqBuffer(
        handle: Long,
        sequence: String,
    )

    private external fun clearSeqBuffer(handle: Long)

    private external fun encode(
        handle: Long,
        numThreads: Int,
    ): Array<FloatArray>

    private external fun release(handle: Long)
}
