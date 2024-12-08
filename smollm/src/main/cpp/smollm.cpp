#include "llama.h"
#include "common.h"
#include "llm_inference.h"
#include <jni.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_io_shubham0204_smollm_SmolLM_loadModel(
    JNIEnv *env,
    jobject thiz,
    jstring model_path,
    jfloat min_p,
    jfloat temperature,
    jboolean store_chats
) {
    jboolean isCopy = true;
    const char* model_path_cstr = env->GetStringUTFChars(model_path, &isCopy);
    LLMInference* llmInference = new LLMInference();

    try {
        llmInference->load_model(model_path_cstr, min_p, temperature, store_chats);
    }
    catch (std::runtime_error& error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
    }

    env->ReleaseStringUTFChars(model_path, model_path_cstr);
    return reinterpret_cast<jlong>(llmInference);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_addChatMessage(JNIEnv *env, jobject thiz, jlong model_ptr, jstring message,
                                                 jstring role) {
    jboolean isCopy = true;
    const char* message_cstr = env->GetStringUTFChars(message, &isCopy);
    const char* role_cstr = env->GetStringUTFChars(role, &isCopy);
    LLMInference* llmInference = reinterpret_cast<LLMInference*>(model_ptr);
    llmInference->add_chat_message(message_cstr, role_cstr);
    env->ReleaseStringUTFChars(message, message_cstr);
    env->ReleaseStringUTFChars(role, role_cstr);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_io_shubham0204_smollm_SmolLM_getResponseGenerationSpeed(JNIEnv *env, jobject thiz, jlong model_ptr) {
    LLMInference* llmInference = reinterpret_cast<LLMInference*>(model_ptr);
    return llmInference->get_response_generation_time();
}

extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_close(
    JNIEnv *env,
    jobject thiz,
    jlong model_ptr
) {
    LLMInference* llmInference = reinterpret_cast<LLMInference*>(model_ptr);
    delete llmInference;
}


extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_startCompletion(
    JNIEnv *env,
    jobject thiz,
    jlong model_ptr,
    jstring prompt
) {
    jboolean isCopy = true;
    const char* prompt_cstr = env->GetStringUTFChars(prompt, &isCopy);
    LLMInference* llmInference = reinterpret_cast<LLMInference*>(model_ptr);
    llmInference->start_completion(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
}


extern "C"
JNIEXPORT jstring JNICALL
Java_io_shubham0204_smollm_SmolLM_completionLoop(
    JNIEnv *env,
    jobject thiz,
    jlong model_ptr
) {
    LLMInference* llmInference = reinterpret_cast<LLMInference*>(model_ptr);
    try {
        std::string response = llmInference->completion_loop();
        return env->NewStringUTF(response.c_str());
    }
    catch (std::runtime_error& error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_io_shubham0204_smollm_SmolLM_stopCompletion(
    JNIEnv *env,
    jobject thiz,
    jlong model_ptr
) {
    LLMInference* llmInference = reinterpret_cast<LLMInference*>(model_ptr);
    llmInference->stop_completion();
}