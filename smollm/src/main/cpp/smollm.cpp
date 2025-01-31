#include "LLMInference.h"
#include <jni.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_io_shubham0204_smollm_SmolLM_loadModel(
        JNIEnv *env,
        jobject thiz,
        jstring modelPath,
        jfloat minP,
        jfloat temperature,
        jboolean storeChats,
        jlong contextSize
) {
    jboolean isCopy = true;
    const char *modelPathCstr = env->GetStringUTFChars(modelPath, &isCopy);
    LLMInference *llmInference = new LLMInference();

    try {
        llmInference->loadModel(modelPathCstr, minP, temperature, storeChats, contextSize);
    }
    catch (std::runtime_error &error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
    }

    env->ReleaseStringUTFChars(modelPath, modelPathCstr);
    return reinterpret_cast<jlong>(llmInference);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_addChatMessage(JNIEnv *env, jobject thiz, jlong modelPtr, jstring message,
                                                 jstring role) {
    jboolean isCopy = true;
    const char *messageCstr = env->GetStringUTFChars(message, &isCopy);
    const char *roleCstr = env->GetStringUTFChars(role, &isCopy);
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    llmInference->addChatMessage(messageCstr, roleCstr);
    env->ReleaseStringUTFChars(message, messageCstr);
    env->ReleaseStringUTFChars(role, roleCstr);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_io_shubham0204_smollm_SmolLM_getResponseGenerationSpeed(JNIEnv *env, jobject thiz, jlong modelPtr) {
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    return llmInference->getResponseGenerationTime();
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_shubham0204_smollm_SmolLM_getContextSizeUsed(JNIEnv *env, jobject thiz, jlong modelPtr) {
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    return llmInference->getContextSizeUsed();
}

extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_close(
        JNIEnv *env,
        jobject thiz,
        jlong modelPtr
) {
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    delete llmInference;
}


extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_startCompletion(
        JNIEnv *env,
        jobject thiz,
        jlong modelPtr,
        jstring prompt
) {
    jboolean isCopy = true;
    const char *promptCstr = env->GetStringUTFChars(prompt, &isCopy);
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    llmInference->startCompletion(promptCstr);
    env->ReleaseStringUTFChars(prompt, promptCstr);
}


extern "C"
JNIEXPORT jstring JNICALL
Java_io_shubham0204_smollm_SmolLM_completionLoop(
        JNIEnv *env,
        jobject thiz,
        jlong modelPtr
) {
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    try {
        std::string response = llmInference->completionLoop();
        return env->NewStringUTF(response.c_str());
    }
    catch (std::runtime_error &error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_stopCompletion(
        JNIEnv *env,
        jobject thiz,
        jlong modelPtr
) {
    LLMInference *llmInference = reinterpret_cast<LLMInference *>(modelPtr);
    llmInference->stopCompletion();
}