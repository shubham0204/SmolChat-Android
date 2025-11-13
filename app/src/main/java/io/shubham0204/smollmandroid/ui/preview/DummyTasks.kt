package io.shubham0204.smollmandroid.ui.preview

import io.shubham0204.smollmandroid.data.Task

val dummyTasksList =
    listOf<Task>(
        Task(
            id = 1,
            name = "Summarize",
            systemPrompt =
                "You are a text summarizer. Summarize the given text in two to three points.",
            modelId = 2,
            modelName = "Phi-3 Mini 4k",
        ),
        Task(
            id = 2,
            name = "Rewrite",
            systemPrompt =
                "You are a text rewriter. Rewrite the given text in a more professional and formal tone.",
            modelId = 3,
            modelName = "Gemma 2 2B",
        ),
        Task(
            id = 3,
            name = "Explain",
            systemPrompt =
                "You are a text explainer. Explain the given text in a simple and easy-to-understand manner.",
            modelId = 4,
            modelName = "Qwen 2 1.5B",
        ),
    )
