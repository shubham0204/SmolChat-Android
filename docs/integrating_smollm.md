# Integrating the `smollm` module in other projects for using LLMs

The `smollm` Gradle module contains the JNI bindings to llama.cpp that are used by SmolChat. This guide will cover the steps on how to integrate the LLM inference capabilities of SmolChat in other applications or Android projects.

#### Building the AAR

Open this repository in Android Studio and make sure it builds successfully. In the root directory, use the following command,

```
gradlew :smollm:assemble
```

to generate an Android Archive (`.aar` file) for the `smollm` module. The generated AARs can be found in the `smollm/build/outputs` directory.

#### Including the AAR in an Android Studio Project

In an Android project, copy the AAR file to the `app/libs` directory (create the `libs` directory if it does not exist). Next, in `app/build.gradle.kts` `dependencies` block, add the following line,

```kotlin
dependencies {
    // ...
    implementation(files("libs/smollm.aar"))
    // ...
}
```

and sync the project. The classes `SmolLM` and `GGUFReader` should now be available for use in the project.

#### Using the `SmolLM` class

1. Instantiate the `SmolLM` class for a single conversation, like `val smollm = SmolLM()`.
2. Initialize the inference pipeline by calling `smollm.create` with chat parameters and path to the GGUF model.
3. Use `addUserMessage`, `addAssistantMessage` and `addSystemPrompt` methods to add messages with different roles to the conversation. `addSystemPrompt` should only be called once at the beginning of the conversation.
4. Use `getResponse` to get the response from the LLM as a stream of tokens i.e. `Flow<String>`.
5. Use `getResponseGenerationSpeed` to get the rate at which the LLM is generating the response,
   `getTimeToFirstToken` to get the time taken for the first token (TTFT in seconds), and
   `getContextLengthUsed` to get the number of tokens consumed by the context window.
6. Call `smollm.close` to release resources taken by the native code.