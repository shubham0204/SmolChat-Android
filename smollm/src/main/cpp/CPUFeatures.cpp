#include <jni.h>
#include <string>
#include <vector>

#include "cpu_features_macros.h"

#if defined(CPU_FEATURES_ARCH_AARCH64)
#include "cpuinfo_aarch64.h"
#elif defined(CPU_FEATURES_ARCH_ARM)
#include "cpuinfo_arm.h"
#elif defined(CPU_FEATURES_ARCH_X86)
#include "cpuinfo_x86.h"
#endif

namespace {

std::vector<std::string>
getAvailableCPUFeatures() {
    std::vector<std::string> features;
#if defined(CPU_FEATURES_ARCH_AARCH64)
    using namespace cpu_features;
    const Aarch64Info info = GetAarch64Info();
    for (int i = 0; i < AARCH64_LAST_; ++i) {
        auto feat = static_cast<Aarch64FeaturesEnum>(i);
        if (GetAarch64FeaturesEnumValue(&info.features, feat)) {
            const char* name = GetAarch64FeaturesEnumName(feat);
            if (name && name[0] != '\0') {
                features.push_back(name);
            }
        }
    }
    // Add aliases for compatibility with SmolLM CPU feature checks
    if (info.features.fphp) {
        features.push_back("fp16");
    }
    if (info.features.asimddp) {
        features.push_back("dotprod");
    }
#elif defined(CPU_FEATURES_ARCH_ARM)
    using namespace cpu_features;
    const ArmInfo info = GetArmInfo();
    for (int i = 0; i < ARM_LAST_; ++i) {
        auto feat = static_cast<ArmFeaturesEnum>(i);
        if (GetArmFeaturesEnumValue(&info.features, feat)) {
            const char* name = GetArmFeaturesEnumName(feat);
            if (name && name[0] != '\0') {
                features.push_back(name);
            }
        }
    }
#elif defined(CPU_FEATURES_ARCH_X86)
    using namespace cpu_features;
    const X86Info info = GetX86Info();
    for (int i = 0; i < X86_LAST_; ++i) {
        auto feat = static_cast<X86FeaturesEnum>(i);
        if (GetX86FeaturesEnumValue(&info.features, feat)) {
            const char* name = GetX86FeaturesEnumName(feat);
            if (name && name[0] != '\0') {
                features.push_back(name);
            }
        }
    }
#endif
    return features;
}

std::string
getCPUFeaturesString() {
    const std::vector<std::string> features = getAvailableCPUFeatures();
    std::string                    result;
    for (size_t i = 0; i < features.size(); ++i) {
        if (i > 0) {
            result += " ";
        }
        result += features[i];
    }
    return result;
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_io_shubham0204_smollm_SmolLM_getCPUFeatures(JNIEnv* env, jclass /*clazz*/) {
    const std::string featuresStr = getCPUFeaturesString();
    return env->NewStringUTF(featuresStr.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_shubham0204_smollm_SmolLM_00024Companion_getCPUFeatures(JNIEnv* env, jobject /*thiz*/) {
    const std::string featuresStr = getCPUFeaturesString();
    return env->NewStringUTF(featuresStr.c_str());
}