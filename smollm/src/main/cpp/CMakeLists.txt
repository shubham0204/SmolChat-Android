cmake_minimum_required(VERSION 3.22.1)
project("smollm")

set(LLAMA_DIR "../../../../llama.cpp")
set(GGML_DIR ${LLAMA_DIR}/ggml)
set(COMMON_DIR ${LLAMA_DIR}/common)
set(SOURCES
        ${GGML_DIR}/src/ggml.c
        ${GGML_DIR}/src/ggml-alloc.c
        ${GGML_DIR}/src/ggml-backend.cpp
        ${GGML_DIR}/src/ggml-threading.cpp
        ${GGML_DIR}/src/ggml-quants.c
        ${GGML_DIR}/src/ggml-backend-reg.cpp
        ${GGML_DIR}/src/ggml-opt.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu.c
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-aarch64.cpp
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-quants.c
        ${GGML_DIR}/src/ggml-cpu/ggml-cpu-traits.cpp

        ${LLAMA_DIR}/src/llama.cpp
        ${LLAMA_DIR}/src/llama-vocab.cpp
        ${LLAMA_DIR}/src/llama-grammar.cpp
        ${LLAMA_DIR}/src/llama-sampling.cpp
        ${LLAMA_DIR}/src/unicode.h
        ${LLAMA_DIR}/src/unicode.cpp
        ${LLAMA_DIR}/src/unicode-data.cpp

        ${COMMON_DIR}/arg.cpp
        ${COMMON_DIR}/base64.hpp
        ${COMMON_DIR}/common.cpp
        ${COMMON_DIR}/console.cpp
        ${COMMON_DIR}/json-schema-to-grammar.cpp
        ${COMMON_DIR}/json.hpp
        ${COMMON_DIR}/log.cpp
        ${COMMON_DIR}/ngram-cache.cpp
        ${COMMON_DIR}/sampling.cpp

        llm_inference.cpp
        smollm.cpp
)

# compiling for different CPU extensions for Arm64 (aarch64)
# See docs/build_arm_flags.md for more details

function(build_library target_name cpu_flags)
    add_library(
            ${target_name}
            SHARED
            ${SOURCES}
    )
    target_include_directories(
            ${target_name}
            PUBLIC
            ${GGML_DIR}/include
            ${GGML_DIR}/src
            ${GGML_DIR}/src/ggml-cpu
            ${LLAMA_DIR}/include
            ${COMMON_DIR}
    )

    # set the compile options for the target `ggml`
    # targets `llama` and `common` are linked to `ggml`,
    # thus inheriting the same compile options

    target_compile_options(
            ${target_name}
            PUBLIC
            -DGGML_USE_CPU -DGGML_USE_CPU_AARCH64 ${cpu_flags} -O3
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
            android log
    )
    # -Wl,--gc-sections: remove unused sections (garbage collection)
    # -flto: link-time optimization
    target_link_options(
            ${target_name}
            PRIVATE
            -Wl,--gc-sections -flto
    )
endfunction()

build_library("smollm" "")
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    build_library("smollm_v8" "-march=armv8-a")
    # Targets for Arm-v8.2a
    build_library("smollm_v8_2_fp16" "-march=armv8.2-a+fp16")
    build_library("smollm_v8_2_fp16_dotprod" "-march=armv8.2-a+fp16+dotprod")

    # Targets for Arm-v8.4a
    build_library("smollm_v8_4_fp16_dotprod" "-march=armv8.4-a+fp16+dotprod")
    build_library("smollm_v8_4_fp16_dotprod_sve" "-march=armv8.4-a+fp16+dotprod+sve")
    build_library("smollm_v8_4_fp16_dotprod_i8mm" "-march=armv8.4-a+fp16+dotprod+i8mm")
    build_library("smollm_v8_4_fp16_dotprod_i8mm_sve" "-march=armv8.4-a+fp16+dotprod+i8mm+sve")
endif()