package io.shubham0204.smollmandroid.ui.preview

import io.shubham0204.smollmandroid.data.LLMModel

val dummyLLMModels =
    listOf<LLMModel>(
        LLMModel(
            id = 2,
            name = "Phi-3 Mini 4k",
            url = "",
            path = "",
            contextSize = 4096,
            chatTemplate = "",
        ),
        LLMModel(
            id = 3,
            name = "Gemma 2 2B",
            url = "",
            path = "",
            contextSize = 4096,
            chatTemplate = "",
        ),
        LLMModel(
            id = 4,
            name = "Qwen 2 1.5B",
            url = "",
            path = "",
            contextSize = 4096,
            chatTemplate = "",
        ),
    )
