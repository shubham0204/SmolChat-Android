#include "VectorDB.cpp"
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jlong JNICALL
Java_io_shubham0204_smolvectordb_SmolVectorDB_initialize(JNIEnv* env, jobject thiz) {
    VectorDB* db = new VectorDB();
    return reinterpret_cast<jlong>(db);
}

extern "C" JNIEXPORT void JNICALL
Java_io_shubham0204_smolvectordb_SmolVectorDB_insertRecord(JNIEnv* env, jobject thiz, jlong handle, jstring text,
                                                           jfloatArray embedding) {
    VectorDB*   db              = reinterpret_cast<VectorDB*>(handle);
    const char* nativeText      = env->GetStringUTFChars(text, 0);
    jfloat*     nativeEmbedding = env->GetFloatArrayElements(embedding, 0);

    std::array<float, EMBEDDING_DIM> embeddingArray;
    for (int i = 0; i < EMBEDDING_DIM; ++i) {
        embeddingArray[i] = nativeEmbedding[i];
    }

    db->insertRecord(VectorDBRecord(nativeText, embeddingArray));

    env->ReleaseStringUTFChars(text, nativeText);
    env->ReleaseFloatArrayElements(embedding, nativeEmbedding, 0);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_shubham0204_smolvectordb_SmolVectorDB_nearestNeighbor(JNIEnv* env, jobject thiz, jlong handle,
                                                              jfloatArray query, jint k) {
    VectorDB* db          = reinterpret_cast<VectorDB*>(handle);
    jfloat*   nativeQuery = env->GetFloatArrayElements(query, 0);

    std::array<float, EMBEDDING_DIM> queryArray;
    for (int i = 0; i < EMBEDDING_DIM; ++i) {
        queryArray[i] = nativeQuery[i];
    }

    std::vector<VectorDBRecord> neighbors = db->nearestNeighbor(queryArray, k);
    env->ReleaseFloatArrayElements(query, nativeQuery, 0);

    jclass    listClass       = env->FindClass("java/util/ArrayList");
    jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
    jobject   list            = env->NewObject(listClass, listConstructor);
    jmethodID addMethod       = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");

    for (const auto& neighbor : neighbors) {
        jstring neighborText = env->NewStringUTF(neighbor.text.c_str());
        env->CallBooleanMethod(list, addMethod, neighborText);
        env->DeleteLocalRef(neighborText);
    }

    return list;
}

extern "C" JNIEXPORT void JNICALL
Java_io_shubham0204_smolvectordb_SmolVectorDB_close(JNIEnv* env, jobject thiz, jlong handle) {
    VectorDB* db = reinterpret_cast<VectorDB*>(handle);
    delete db;
}