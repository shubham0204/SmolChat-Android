cmake_minimum_required(VERSION 3.22.1)
project("smollm")

set(LLAMA_DIR "../../../../llama.cpp")
set(GGML_DIR ${LLAMA_DIR}/ggml)
set(COMMON_DIR ${LLAMA_DIR}/common)
set(SOURCES
        ${GGML_DIR}/include/ggml.h
        ${GGML_DIR}/include/ggml-cpu.h
        ${GGML_DIR}/include/ggml-alloc.h
        ${GGML_DIR}/include/ggml-backend.h
        ${GGML_DIR}/include/ggml-cpp.h
        ${GGML_DIR}/src/ggml.c
        ${GGML_DIR}/src/ggml-cpu.c
        ${GGML_DIR}/src/ggml-alloc.c
        ${GGML_DIR}/src/ggml-backend.cpp
        ${GGML_DIR}/src/ggml-quants.c
        ${GGML_DIR}/src/ggml-quants.h
        ${GGML_DIR}/src/ggml-aarch64.c
        ${GGML_DIR}/src/ggml-aarch64.h

        ${LLAMA_DIR}/include/llama.h
        ${LLAMA_DIR}/src/llama.cpp
        ${LLAMA_DIR}/src/llama-vocab.cpp
        ${LLAMA_DIR}/src/llama-grammar.cpp
        ${LLAMA_DIR}/src/llama-sampling.cpp
        ${LLAMA_DIR}/src/unicode.h
        ${LLAMA_DIR}/src/unicode.cpp
        ${LLAMA_DIR}/src/unicode-data.cpp

        ${COMMON_DIR}/arg.cpp
        ${COMMON_DIR}/arg.h
        ${COMMON_DIR}/base64.hpp
        ${COMMON_DIR}/common.cpp
        ${COMMON_DIR}/common.h
        ${COMMON_DIR}/console.cpp
        ${COMMON_DIR}/console.h
        ${COMMON_DIR}/json-schema-to-grammar.cpp
        ${COMMON_DIR}/json.hpp
        ${COMMON_DIR}/log.cpp
        ${COMMON_DIR}/log.h
        ${COMMON_DIR}/ngram-cache.cpp
        ${COMMON_DIR}/ngram-cache.h
        ${COMMON_DIR}/sampling.cpp
        ${COMMON_DIR}/sampling.h

        llm_inference.cpp
        smollm.cpp
)

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
            ${LLAMA_DIR}/include
            ${COMMON_DIR}
    )

    # set the compile options for the target `ggml`
    # targets `llama` and `common` are linked to `ggml`,
    # thus inheriting the same compile options

    # LLAMA_CPU_FLAGS is passed from smollm/build.gradle.kts
    target_compile_options(
            ${target_name}
            PUBLIC
            ${cpu_flags} -O3
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