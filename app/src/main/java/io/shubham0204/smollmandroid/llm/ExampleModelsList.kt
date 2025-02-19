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

package io.shubham0204.smollmandroid.llm

import io.shubham0204.smollmandroid.data.models.LLMModel

/**
 * A list of models that are shown in the DownloadModelActivity for the user to quickly get started
 * by downloading a model.
 */
val exampleModelsList =
    listOf(
        LLMModel(
            name = "SmolLM2 360M Instruct GGUF",
            url =
                "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
        ),
        LLMModel(
            name = "SmolLM2 1.7B Instruct GGUF",
            url =
                "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
        ),
        LLMModel(
            name = "Qwen2.5 1.5B Q8 Instruct GGUF",
            url =
                "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q8_0.gguf",
        ),
        LLMModel(
            name = "Qwen2.5 3B Q5_K_M Instruct GGUF",
            url =
                "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q5_k_m.gguf",
        ),
        LLMModel(
            name = "Qwen2.5 Coder 3B Instruct Q5 GGUF",
            url =
                "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/qwen2.5-coder-3b-instruct-q5_0.gguf",
        ),
    )
