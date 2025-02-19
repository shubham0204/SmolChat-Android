#include "splitters/RecursiveTextSplitter.h"
#include <jni.h>

extern "C" JNIEXPORT jobjectArray JNICALL
Java_io_shubham0204_smollm_TextSplitters_00024Companion_splitWhiteSpace(JNIEnv* env, jobject thiz, jstring text,
                                                                        jlong chunkSize) {
    jboolean                 isCopy   = true;
    const char*              textCStr = env->GetStringUTFChars(text, &isCopy);
    RecursiveTextSplitter    whiteSpaceSplitter({ "\n\n", "\n", " " });
    std::vector<std::string> parts = whiteSpaceSplitter.split(textCStr, chunkSize);
    env->ReleaseStringUTFChars(text, textCStr);

    jclass       stringClass = env->FindClass("java/lang/String");
    jobjectArray result      = env->NewObjectArray(parts.size(), stringClass, nullptr);
    for (size_t i = 0; i < parts.size(); i++) {
        jstring part = env->NewStringUTF(parts[i].c_str());
        env->SetObjectArrayElement(result, i, part);
    }

    return result;
}