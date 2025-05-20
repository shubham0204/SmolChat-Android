<img src="resources/app_icon/icon.png" alt="app icon" width="256"/>

# SmolChat - On-Device Inference of SLMs in Android

<table>
<tr>
<td>
<img src="resources/app_screenshots/phone/1.png" alt="app_img_01">
</td>
<td>
<img src="resources/app_screenshots/phone/2.png" alt="app_img_02">
</td>
<td>
<img src="resources/app_screenshots/phone/3.png" alt="app_img_03">
</td>
</tr>
<tr>
<td>
<img src="resources/app_screenshots/phone/4.png" alt="app_img_04">
</td>
<td>
<img src="resources/app_screenshots/phone/5.png" alt="app_img_05">
</td>
<td>
<img src="resources/app_screenshots/phone/6.png" alt="app_img_06">
</td>
</tr>
</table>

## Installation

### Google Play

![](https://raw.githubusercontent.com/pioug/google-play-badges/06ccd9252af1501613da2ca28eaffe31307a4e6d/svg/English.svg)

**[Get it on Google Play](https://play.google.com/store/apps/details?id=io.shubham0204.smollmandroid)**

### GitHub

1. Download the latest APK from [GitHub Releases](https://github.com/shubham0204/SmolChat-Android/releases/) and transfer it to your Android device.
2. If your device does not downloading APKs from untrusted sources, search for **how to allow downloading APKs from unknown sources** for your device.

### Obtainium

[Obtainium](https://obtainium.imranr.dev/) allows users to update/download apps directly from their sources, like GitHub or FDroid. 

1. [Download the Obtainium app](https://obtainium.imranr.dev/) by choosing your device architecture or 'Download Universal APK'.
2. From the bottom menu, select '➕Add App'
3. In the text field labelled 'App source URL *', enter the following URL and click 'Add' besides the text field: `https://github.com/shubham0204/SmolChat-Android`
4. SmolChat should now be visible in the 'Apps' screen. You can get notifications about newer releases and download them directly without going to the GitHub repo.

## Project Goals

- Provide a usable user interface to interact with local SLMs (small language models) locally, on-device
- Allow users to add/remove SLMs (GGUF models) and modify their system prompts or inference parameters (temperature, 
  min-p)
- Allow users to create specific-downstream tasks quickly and use SLMs to generate responses
- Simple, easy to understand, extensible codebase

## Setup

1. Clone the repository with its submodule originating from llama.cpp,

```commandline
git clone --depth=1 https://github.com/shubham0204/SmolChat-Android
cd SmolChat-Android
git submodule update --init --recursive
```

2. Android Studio starts building the project automatically. If not, select **Build > Rebuild Project** to start a project build.

3. After a successful project build, [connect an Android device](https://developer.android.com/studio/run/device) to your system. Once connected, the name of the device must be visible in top menu-bar in Android Studio.

## Working

1. The application uses llama.cpp to load and execute GGUF models. As llama.cpp is written in pure C/C++, it is easy 
   to compile on Android-based targets using the [NDK](https://developer.android.com/ndk). 

2. The `smollm` module uses a `llm_inference.cpp` class which interacts with llama.cpp's C-style API to execute the 
   GGUF model and a JNI binding `smollm.cpp`. Check the [C++ source files here](https://github.com/shubham0204/SmolChat-Android/tree/main/smollm/src/main/cpp). On the Kotlin side, the [`SmolLM`](https://github.com/shubham0204/SmolChat-Android/blob/main/smollm/src/main/java/io/shubham0204/smollm/SmolLM.kt) class provides 
   the required methods to interact with the JNI (C++ side) bindings.

3. The `app` module contains the application logic and UI code. Whenever a new chat is opened, the app instantiates 
   the `SmolLM` class and provides it the model file-path which is stored by the [`LLMModel`](https://github.com/shubham0204/SmolChat-Android/blob/main/app/src/main/java/io/shubham0204/smollmandroid/data/DataModels.kt) entity.
   Next, the app adds messages with role `user` and `system` to the chat by retrieving them from the database and
   using `LLMInference::addChatMessage`.

4. For tasks, the messages are not persisted, and we inform to `LLMInference` by passing `_storeChats=false` to
   `LLMInference::loadModel`.

## Technologies

* [ggerganov/llama.cpp](https://github.com/ggerganov/llama.cpp) is a pure C/C++ framework to execute machine learning 
  models on multiple execution backends. It provides a primitive C-style API to interact with LLMs 
  converted to the [GGUF format](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md) native to [ggml](https://github.com/ggerganov/ggml)/llama.cpp. The app uses JNI bindings to interact with a small class `smollm.
  cpp` which uses llama.cpp to load and execute GGUF models.

* [noties/Markwon](https://github.com/noties/Markwon) is a markdown rendering library for Android. The app uses 
  Markwon and [Prism4j](https://github.com/noties/Prism4j) (for code syntax highlighting) to render Markdown responses 
  from the SLMs.

## More On-Device ML Projects

- [shubham0204/Android-Doc-QA](https://github.com/shubham0204/Android-Document-QA): On-device RAG-based question 
  answering from documents
- [shubham0204/OnDevice-Face-Recognition-Android](https://github.com/shubham0204/OnDevice-Face-Recognition-Android): 
  Realtime face recognition with FaceNet, Mediapipe and ObjectBox's vector database
- [shubham0204/FaceRecognition_With_FaceNet_Android](https://github.com/shubham0204/OnDevice-Face-Recognition-Android):
  Realtime face recognition with FaceNet, MLKit
- [shubham0204/CLIP-Android](https://github.com/shubham0204/CLIP-Android): On-device CLIP inference in Android 
  (search images with textual queries)
- [shubham0204/Segment-Anything-Android](https://github.com/shubham0204/Segment-Anything-Android): Execute Meta's 
  SAM model in Android with onnxruntime
- [shubham0204/Depth-Anything-Android](https://github.com/shubham0204/Depth-Anything-Android): Execute the 
  Depth-Anything model in Android with onnxruntime for monocular depth estimation
- [shubham0204/Sentence-Embeddings-Android](https://github.com/shubham0204/Sentence-Embeddings-Android): Generate 
  sentence-embeddings (from models like `all-MiniLM-L6-V2`) in Android

## Future

The following features/tasks are planned for the future releases of the app:

- Assign names to chats automatically (just like ChatGPT and Claude)
- Add a search bar to the navigation drawer to search for messages within chats
- Add a background service which uses BlueTooth/HTTP/WiFi to communicate with a desktop application to send queries 
  from the desktop to the mobile device for inference
- Enable auto-scroll when generating partial response in `ChatActivity`
- Measure RAM consumption
- Integrate [Android-Doc-QA](https://github.com/shubham0204/Android-Document-QA) for on-device RAG-based question answering from documents
- Check if llama.cpp can be compiled to use Vulkan for inference on Android devices (and use the mobile GPU)