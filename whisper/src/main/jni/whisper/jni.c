#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <sys/sysinfo.h>
#include <string.h>
#include "whisper.h"
#include "ggml.h"

#include <stdbool.h>
#include <pthread.h>

static bool g_should_abort = false;
static pthread_mutex_t g_abort_mutex = PTHREAD_MUTEX_INITIALIZER;

#define UNUSED(x) (void)(x)
#define TAG "JNI"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)

// Global references for callback handling
static JavaVM* g_jvm = NULL;
static jobject g_callback = NULL;

// Method IDs for callback functions
static jmethodID g_onNewSegmentMethod = NULL;
static jmethodID g_onProgressMethod = NULL;
static jmethodID g_onCompleteMethod = NULL;



static inline int min(int a, int b) {
    return (a < b) ? a : b;
}

static inline int max(int a, int b) {
    return (a > b) ? a : b;
}

struct input_stream_context {
    size_t offset;
    JNIEnv * env;
    jobject thiz;
    jobject input_stream;

    jmethodID mid_available;
    jmethodID mid_read;
};

size_t inputStreamRead(void * ctx, void * output, size_t read_size) {
    struct input_stream_context* is = (struct input_stream_context*)ctx;

    jint avail_size = (*is->env)->CallIntMethod(is->env, is->input_stream, is->mid_available);
    jint size_to_copy = read_size < avail_size ? (jint)read_size : avail_size;

    jbyteArray byte_array = (*is->env)->NewByteArray(is->env, size_to_copy);

    jint n_read = (*is->env)->CallIntMethod(is->env, is->input_stream, is->mid_read, byte_array, 0, size_to_copy);

    if (size_to_copy != read_size || size_to_copy != n_read) {
        LOGI("Insufficient Read: Req=%zu, ToCopy=%d, Available=%d", read_size, size_to_copy, n_read);
    }

    jbyte* byte_array_elements = (*is->env)->GetByteArrayElements(is->env, byte_array, NULL);
    memcpy(output, byte_array_elements, size_to_copy);
    (*is->env)->ReleaseByteArrayElements(is->env, byte_array, byte_array_elements, JNI_ABORT);

    (*is->env)->DeleteLocalRef(is->env, byte_array);

    is->offset += size_to_copy;

    return size_to_copy;
}
bool inputStreamEof(void * ctx) {
    struct input_stream_context* is = (struct input_stream_context*)ctx;

    jint result = (*is->env)->CallIntMethod(is->env, is->input_stream, is->mid_available);
    return result <= 0;
}
void inputStreamClose(void * ctx) {

}

JNIEXPORT jlong JNICALL
Java_com_whispercppdemo_whisper_WhisperLib_00024Companion_initContextFromInputStream(
        JNIEnv *env, jobject thiz, jobject input_stream) {
    UNUSED(thiz);

    struct whisper_context *context = NULL;
    struct whisper_model_loader loader = {};
    struct input_stream_context inp_ctx = {};

    inp_ctx.offset = 0;
    inp_ctx.env = env;
    inp_ctx.thiz = thiz;
    inp_ctx.input_stream = input_stream;

    jclass cls = (*env)->GetObjectClass(env, input_stream);
    inp_ctx.mid_available = (*env)->GetMethodID(env, cls, "available", "()I");
    inp_ctx.mid_read = (*env)->GetMethodID(env, cls, "read", "([BII)I");

    loader.context = &inp_ctx;
    loader.read = inputStreamRead;
    loader.eof = inputStreamEof;
    loader.close = inputStreamClose;

    loader.eof(loader.context);

    context = whisper_init(&loader);
    return (jlong) context;
}

static size_t asset_read(void *ctx, void *output, size_t read_size) {
    return AAsset_read((AAsset *) ctx, output, read_size);
}

static bool asset_is_eof(void *ctx) {
    return AAsset_getRemainingLength64((AAsset *) ctx) <= 0;
}

static void asset_close(void *ctx) {
    AAsset_close((AAsset *) ctx);
}

static struct whisper_context *whisper_init_from_asset(
        JNIEnv *env,
        jobject assetManager,
        const char *asset_path
) {
    LOGI("Loading model from asset '%s'\n", asset_path);
    AAssetManager *asset_manager = AAssetManager_fromJava(env, assetManager);
    AAsset *asset = AAssetManager_open(asset_manager, asset_path, AASSET_MODE_STREAMING);
    if (!asset) {
        LOGW("Failed to open '%s'\n", asset_path);
        return NULL;
    }

    whisper_model_loader loader = {
            .context = asset,
            .read = &asset_read,
            .eof = &asset_is_eof,
            .close = &asset_close
    };

    return whisper_init_with_params(&loader, whisper_context_default_params());
}

JNIEXPORT jlong JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContextFromAsset(
        JNIEnv *env, jobject thiz, jobject assetManager, jstring asset_path_str) {
    UNUSED(thiz);
    struct whisper_context *context = NULL;
    const char *asset_path_chars = (*env)->GetStringUTFChars(env, asset_path_str, NULL);
    context = whisper_init_from_asset(env, assetManager, asset_path_chars);
    (*env)->ReleaseStringUTFChars(env, asset_path_str, asset_path_chars);
    return (jlong) context;
}

JNIEXPORT jlong JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContext(
        JNIEnv *env, jobject thiz, jstring model_path_str) {
    UNUSED(thiz);
    struct whisper_context *context = NULL;
    const char *model_path_chars = (*env)->GetStringUTFChars(env, model_path_str, NULL);
    context = whisper_init_from_file_with_params(model_path_chars, whisper_context_default_params());
    (*env)->ReleaseStringUTFChars(env, model_path_str, model_path_chars);
    return (jlong) context;
}

JNIEXPORT void JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_freeContext(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    UNUSED(env);
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    whisper_free(context);
}
// Callback for new segments
void new_segment_callback(struct whisper_context * ctx, struct whisper_state * state, int n_new, void * user_data) {
    JNIEnv* env;
    (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    for (int i = 0; i < n_new; i++) {
        const int segment_id = whisper_full_n_segments(ctx) - n_new + i;
        const char* text = whisper_full_get_segment_text(ctx, segment_id);
        const int64_t t0 = whisper_full_get_segment_t0(ctx, segment_id);
        const int64_t t1 = whisper_full_get_segment_t1(ctx, segment_id);

        jstring jtext = (*env)->NewStringUTF(env, text);
        (*env)->CallVoidMethod(
                env,
                g_callback,
                g_onNewSegmentMethod,
                (jlong)t0,
                (jlong)t1,
                jtext
        );
        (*env)->DeleteLocalRef(env, jtext);
    }
}

// Progress callback
void progress_callback(struct whisper_context * ctx, struct whisper_state * state, int progress, void * user_data) {
    JNIEnv* env;
    (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);
    (*env)->CallVoidMethod(env, g_callback, g_onProgressMethod, (jint)progress);
}

static bool abort_callback(void* user_data) {
    bool should_abort;
    pthread_mutex_lock(&g_abort_mutex);
    should_abort = g_should_abort;
    pthread_mutex_unlock(&g_abort_mutex);
    return should_abort;
}

JNIEXPORT void JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_resetAbort(JNIEnv* env, jobject thiz) {
    pthread_mutex_lock(&g_abort_mutex);
    g_should_abort = false;
    pthread_mutex_unlock(&g_abort_mutex);
}

JNIEXPORT void JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_stopTranscription(JNIEnv* env, jobject thiz) {
    pthread_mutex_lock(&g_abort_mutex);
    g_should_abort = true;
    pthread_mutex_unlock(&g_abort_mutex);
}


JNIEXPORT void JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_fullTranscribe(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint num_threads,
        jfloatArray audio_data, jstring language, jobject callback) {
    UNUSED(thiz);
// Reset abort state
    Java_com_whispercpp_whisper_WhisperLib_00024Companion_resetAbort(env, thiz);
    // Store JavaVM for later callbacks
    if (g_jvm == NULL) {
        (*env)->GetJavaVM(env, &g_jvm);
    }

    // Clean up previous callback if exists
    if (g_callback != NULL) {
        (*env)->DeleteGlobalRef(env, g_callback);
    }

    // Create new global reference
    g_callback = (*env)->NewGlobalRef(env, callback);

    // Get method IDs
    jclass callbackClass = (*env)->GetObjectClass(env, g_callback);

    jmethodID toStringMethod = (*env)->GetMethodID(env, callbackClass, "toString", "()Ljava/lang/String;");
    jstring str = (*env)->CallObjectMethod(env, g_callback, toStringMethod);
    const char *cStr = (*env)->GetStringUTFChars(env, str, NULL);
    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "Callback class: %s", cStr);
    (*env)->ReleaseStringUTFChars(env, str, cStr);
    g_onNewSegmentMethod = (*env)->GetMethodID(
            env,
            callbackClass,
            "onNewSegment",
            "(JJLjava/lang/String;)V"
    );
    g_onProgressMethod = (*env)->GetMethodID(
            env,
            callbackClass,
            "onProgress",
            "(I)V"
    );
    g_onCompleteMethod = (*env)->GetMethodID(
            env,
            callbackClass,
            "onComplete",
            "()V"
    );

    struct whisper_context *context = (struct whisper_context *) context_ptr;
    jfloat *audio_data_arr = (*env)->GetFloatArrayElements(env, audio_data, NULL);
    const jsize audio_data_length = (*env)->GetArrayLength(env, audio_data);

    // Get language parameter (default to "auto" if null)
    const char *language_str = "auto";
    if (language != NULL) {
        language_str = (*env)->GetStringUTFChars(env, language, NULL);
    }

    // Configure whisper parameters with callbacks
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false; // We handle callbacks ourselves
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = language_str;
    params.n_threads = num_threads;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    // Set our callbacks
    params.new_segment_callback = new_segment_callback;
    params.new_segment_callback_user_data = NULL;
    params.progress_callback = progress_callback;
    params.progress_callback_user_data = NULL;
    params.abort_callback = abort_callback;
    params.abort_callback_user_data = NULL;


    whisper_reset_timings(context);

    LOGI("About to run whisper_full with callbacks (language: %s)", language_str);
    int result = whisper_full(context, params, audio_data_arr, audio_data_length);

    // Cleanup language string if we allocated it
    if (language != NULL) {
        (*env)->ReleaseStringUTFChars(env, language, language_str);
    }

    // Notify completion
    if (result == 0) {
        (*env)->CallVoidMethod(env, g_callback, g_onCompleteMethod);
    } else {
        LOGI("Failed to run the model");
    }

    // Cleanup
    (*env)->ReleaseFloatArrayElements(env, audio_data, audio_data_arr, JNI_ABORT);
    (*env)->DeleteGlobalRef(env, g_callback);
    g_callback = NULL;
}

JNIEXPORT jint JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentCount(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    UNUSED(env);
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    return whisper_full_n_segments(context);
}

JNIEXPORT jstring JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegment(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    const char *text = whisper_full_get_segment_text(context, index);
    jstring string = (*env)->NewStringUTF(env, text);
    return string;
}

JNIEXPORT jlong JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentT0(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    return whisper_full_get_segment_t0(context, index);
}

JNIEXPORT jlong JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentT1(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    return whisper_full_get_segment_t1(context, index);
}

JNIEXPORT jstring JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_getSystemInfo(
        JNIEnv *env, jobject thiz
) {
    UNUSED(thiz);
    const char *sysinfo = whisper_print_system_info();
    jstring string = (*env)->NewStringUTF(env, sysinfo);
    return string;
}

JNIEXPORT jstring JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_benchMemcpy(JNIEnv *env, jobject thiz,
                                                                      jint n_threads) {
    UNUSED(thiz);
    const char *bench_ggml_memcpy = whisper_bench_memcpy_str(n_threads);
    jstring string = (*env)->NewStringUTF(env, bench_ggml_memcpy);
    return string;
}

JNIEXPORT jstring JNICALL
Java_com_whispercpp_whisper_WhisperLib_00024Companion_benchGgmlMulMat(JNIEnv *env, jobject thiz,
                                                                          jint n_threads) {
    UNUSED(thiz);
    const char *bench_ggml_mul_mat = whisper_bench_ggml_mul_mat_str(n_threads);
    jstring string = (*env)->NewStringUTF(env, bench_ggml_mul_mat);
    return string;
}
