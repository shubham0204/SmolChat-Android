cmake_minimum_required(VERSION 3.22.1)
project("smollm")

add_subdirectory(../../../../llama.cpp llama.cpp)
set(LLAMA_DIR_RELATIVE "../../../../llama.cpp")
get_filename_component(LLAMA_DIR ${LLAMA_DIR_RELATIVE} ABSOLUTE)

set(GGML_DIR ${LLAMA_DIR}/ggml)
set(COMMON_DIR ${LLAMA_DIR}/common)
set(VENDOR_DIR ${LLAMA_DIR}/vendor)

# -fvisibility=hidden: hide all symbols by default
# -fvisibility-inlines-hidden: hide all inline symbols by default
target_compile_options(
        llama
        PUBLIC
        -fvisibility=hidden -fvisibility-inlines-hidden
)
# -ffunction-sections: place each function in its own section
# -fdata-sections: place each data member in its own section
target_compile_options(
        llama
        PUBLIC
        -ffunction-sections -fdata-sections
)
target_link_options(
        llama
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)

# compiling for different CPU extensions for Arm64 (aarch64)
# See docs/build_arm_flags.md for more details

function(build_library target_name)
    add_library(
            ${target_name}
            SHARED
            LLMInference.cpp
            smollm.cpp
    )
    target_include_directories(
            ${target_name}
            PUBLIC
            ${COMMON_DIR}
            ${GGML_DIR}/include
            ${GGML_DIR}/src
            ${GGML_DIR}/src/ggml-cpu
            ${LLAMA_DIR}/include
            ${VENDOR_DIR}
    )
    # -fvisibility=hidden: hide all symbols by default
    # -fvisibility-inlines-hidden: hide all inline symbols by default
    target_compile_options(
            ${target_name}
            PUBLIC
            -fvisibility=hidden -fvisibility-inlines-hidden
    )
    # -ffunction-sections: place each function in its own section
    # -fdata-sections: place each data member in its own section
    target_compile_options(
            ${target_name}
            PUBLIC
            -ffunction-sections -fdata-sections
    )
    target_link_libraries(
            ${target_name}
            android log llama common vulkan
    )
    # -Wl,--gc-sections: remove unused sections (garbage collection)
    # -flto: link-time optimization
    # -Wl,--exclude-libs,ALL: exclude all libraries
    target_link_options(
            ${target_name}
            PRIVATE
            -Wl,--gc-sections -flto
            -Wl,--exclude-libs,ALL
    )
endfunction()


function(build_library_arm64 target_name cpu_flags)
    build_library(${target_name})
    set(GGML_SYSTEM_ARCH "ARM")
    set(GGML_CPU_KLEIDIAI ON)
    set(GGML_OPENMP ON)
    target_compile_definitions(${target_name} PRIVATE
            GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
            GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
            GGML_OPENMP=$<BOOL:${GGML_OPENMP}>
    )
    target_compile_options(
            ${target_name}
            PUBLIC
            -DGGML_USE_CPU -DGGML_USE_CPU_AARCH64 ${cpu_flags} -O3
    )
endfunction()

function(build_library_armv7a target_name cpu_flags fpu fpu_abi)
    build_library(${target_name})
    target_compile_options(
            ${target_name}
            PUBLIC
            -DGGML_USE_CPU ${cpu_flags} ${fpu} ${fpu_abi} -O3
    )
endfunction()

function(build_library_universal target_name)
    build_library(${target_name})
    target_compile_options(
            ${target_name}
            PUBLIC
            -DGGML_USE_CPU -O3
    )
endfunction()

build_library_universal("smollm")
if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    build_library_armv7a("smollm_v7a" "-march=armv7-a" "-mfpu=neon-vfpv4" "-mfloat-abi=softfp")
endif()
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    build_library_arm64("smollm_v8" "-march=armv8-a")
    # Targets for Arm-v8.2a
    build_library_arm64("smollm_v8_2_fp16" "-march=armv8.2-a+fp16")
    build_library_arm64("smollm_v8_2_fp16_dotprod" "-march=armv8.2-a+fp16+dotprod")

    # Targets for Arm-v8.4a
    build_library_arm64("smollm_v8_4_fp16_dotprod" "-march=armv8.4-a+fp16+dotprod")
    build_library_arm64("smollm_v8_4_fp16_dotprod_sve" "-march=armv8.4-a+fp16+dotprod+sve")
    build_library_arm64("smollm_v8_4_fp16_dotprod_i8mm" "-march=armv8.4-a+fp16+dotprod+i8mm")
    build_library_arm64("smollm_v8_4_fp16_dotprod_i8mm_sve" "-march=armv8.4-a+fp16+dotprod+i8mm+sve")
endif()

# library target for GGUFReader
set(TARGET_NAME_GGUF_READER ggufreader)
add_library(${TARGET_NAME_GGUF_READER} SHARED GGUFReader.cpp)
target_include_directories(
        ${TARGET_NAME_GGUF_READER}
        PUBLIC
        ${GGML_DIR}/include
)
target_compile_options(
        ${TARGET_NAME_GGUF_READER}
        PUBLIC
        -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections
)
target_link_libraries(
        ${TARGET_NAME_GGUF_READER}
        ggml
)
target_link_options(
        ${TARGET_NAME_GGUF_READER}
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)