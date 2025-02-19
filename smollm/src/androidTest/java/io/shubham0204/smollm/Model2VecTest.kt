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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Model2VecTest {
    /**
     * Paths to the embeddings and tokenizer files
     * Download model.safetensors and tokenizer.json from this HF repository,
     * https://huggingface.co/minishlab/potion-base-8M/tree/main
     *
     * Upload `embeddings.safetensors` and `tokenizer.json` to the test-device using,
     *
     * adb push model.safetensors /data/local/tmp/embeddings.safetensors
     * adb push tokenizer.json /data/local/tmp/tokenizer.json
     */
    private val embeddingsPath = "/data/local/tmp/embeddings.safetensors"
    private val tokenizerPath = "/data/local/tmp/tokenizer.json"
    private val embeddingDims = 256

    @Test
    fun encode_works() {
        val model2Vec = Model2Vec(embeddingsPath, tokenizerPath)
        val embeddings = model2Vec.encode(listOf("Hello World"))
        assert(embeddings.size == 1)
        assert(embeddings[0].size == embeddingDims)
    }
}
