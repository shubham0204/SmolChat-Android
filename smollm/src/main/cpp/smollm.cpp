#include "LLMInference.h"
#include <jni.h>
#include <string>

// Convert a std::string (UTF-8) to a Java String via NewString (UTF-16),
// which correctly handles 4-byte emoji that NewStringUTF cannot encode.
static jstring utf8_to_jstring(JNIEnv* env, const std::string& utf8) {
    // Fast path: delegate to NewStringUTF only when there are no 4-byte sequences.
    for (size_t i = 0; i < utf8.size(); ) {
        unsigned char c = (unsigned char)utf8[i];
        size_t len = c < 0x80 ? 1 : (c & 0xE0) == 0xC0 ? 2 :
                     (c & 0xF0) == 0xE0 ? 3 : (c & 0xF8) == 0xF0 ? 4 : 1;
        if (len == 4) goto use_utf16;
        i += len;
    }
    return env->NewStringUTF(utf8.c_str());

use_utf16:
    // Convert UTF-8 -> jchar array (UTF-16) manually.
    std::vector<jchar> utf16;
    utf16.reserve(utf8.size());
    size_t i = 0;
    while (i < utf8.size()) {
        unsigned char c = (unsigned char)utf8[i];
        uint32_t cp = 0;
        size_t len = 1;
        if      (c < 0x80)             { cp = c; len = 1; }
        else if ((c & 0xE0) == 0xC0)   { cp = c & 0x1F; len = 2; }
        else if ((c & 0xF0) == 0xE0)   { cp = c & 0x0F; len = 3; }
        else if ((c & 0xF8) == 0xF0)   { cp = c & 0x07; len = 4; }
        for (size_t k = 1; k < len && i + k < utf8.size(); ++k)
            cp = (cp << 6) | ((unsigned char)utf8[i + k] & 0x3F);
        i += len;
        if (cp < 0x10000) {
            utf16.push_back((jchar)cp);
        } else {
            // Surrogate pair
            cp -= 0x10000;
            utf16.push_back((jchar)(0xD800 + (cp >> 10)));
            utf16.push_back((jchar)(0xDC00 + (cp & 0x3FF)));
        }
    }
    return env->NewString(utf16.data(), (jsize)utf16.size());
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_shubham0204_smollm_SmolLM_loadModel(JNIEnv* env, jobject thiz, jstring modelPath, jfloat minP,
                                            jfloat temperature, jboolean storeChats, jlong contextSize,
                                            jstring chatTemplate, jint nThreads, jboolean useMmap, jboolean useMlock) {
    jboolean    isCopy           = true;
    const char* modelPathCstr    = env->GetStringUTFChars(modelPath, &isCopy);
    auto*       llmInference     = new LLMInference();
    const char* chatTemplateCstr = env->GetStringUTFChars(chatTemplate, &isCopy);

    try {
        llmInference->loadModel(modelPathCstr, minP, temperature, storeChats, contextSize, chatTemplateCstr, nThreads,
                                useMmap, useMlock);
    } catch (std::exception& error) {
        env->ReleaseStringUTFChars(modelPath, modelPathCstr);
        env->ReleaseStringUTFChars(chatTemplate, chatTemplateCstr);
        delete llmInference;
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return 0;
    }

    env->ReleaseStringUTFChars(modelPath, modelPathCstr);
    env->ReleaseStringUTFChars(chatTemplate, chatTemplateCstr);
    return reinterpret_cast<jlong>(llmInference);
}

extern "C" JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_addChatMessage(JNIEnv* env, jobject thiz, jlong modelPtr, jstring message,
                                                 jstring role) {
    jboolean    isCopy       = true;
    const char* messageCstr  = env->GetStringUTFChars(message, &isCopy);
    const char* roleCstr     = env->GetStringUTFChars(role, &isCopy);
    auto*       llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    llmInference->addChatMessage(messageCstr, roleCstr);
    env->ReleaseStringUTFChars(message, messageCstr);
    env->ReleaseStringUTFChars(role, roleCstr);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_io_shubham0204_smollm_SmolLM_getResponseGenerationSpeed(JNIEnv* env, jobject thiz, jlong modelPtr) {
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    return llmInference->getResponseGenerationTime();
}

extern "C" JNIEXPORT jint JNICALL
Java_io_shubham0204_smollm_SmolLM_getContextSizeUsed(JNIEnv* env, jobject thiz, jlong modelPtr) {
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    return llmInference->getContextSizeUsed();
}

extern "C" JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_close(JNIEnv* env, jobject thiz, jlong modelPtr) {
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    delete llmInference;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_shubham0204_smollm_SmolLM_startCompletion(JNIEnv* env, jobject thiz, jlong modelPtr, jstring prompt) {
    jboolean    isCopy       = true;
    const char* promptCstr   = env->GetStringUTFChars(prompt, &isCopy);
    auto*       llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    bool usedJinja;
    try {
        usedJinja = llmInference->startCompletion(promptCstr);
    } catch (std::exception& error) {
        env->ReleaseStringUTFChars(prompt, promptCstr);
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return JNI_TRUE;
    }
    env->ReleaseStringUTFChars(prompt, promptCstr);
    return usedJinja ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_shubham0204_smollm_SmolLM_completionLoop(JNIEnv* env, jobject thiz, jlong modelPtr) {
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    try {
        std::string response = llmInference->completionLoop();
        return utf8_to_jstring(env, response);
    } catch (std::exception& error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_stopCompletion(JNIEnv* env, jobject thiz, jlong modelPtr) {
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    llmInference->stopCompletion();
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_shubham0204_smollm_SmolLM_benchModel(JNIEnv* env, jobject /*unused*/, jlong modelPtr, jint pp, jint tg, jint pl,
                                             jint nr) {
    auto*       llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    std::string result       = llmInference->benchModel(pp, tg, pl, nr);
    return utf8_to_jstring(env, result);
}
