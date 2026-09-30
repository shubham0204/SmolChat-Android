cmake_minimum_required(VERSION 3.22.1)
project("smollm")

# Configure KleidiAI and Arm architecture for unified single-target build
set(GGML_SYSTEM_ARCH "ARM" CACHE STRING "" FORCE)
set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE)
set(LLAMA_RUNNER_BACKEND "llama.cpp" CACHE STRING "" FORCE)

set(LLAMA_DIR_RELATIVE "../../../../llama.cpp")
get_filename_component(LLAMA_DIR ${LLAMA_DIR_RELATIVE} ABSOLUTE)


add_subdirectory(../../../../llama.cpp llama.cpp)

set(GGML_DIR ${LLAMA_DIR}/ggml)
set(COMMON_DIR ${LLAMA_DIR}/common)
set(VENDOR_DIR ${LLAMA_DIR}/vendor)

# Compile options for llama
target_compile_options(
        llama
        PUBLIC
        -fvisibility=hidden -fvisibility-inlines-hidden
        -ffunction-sections -fdata-sections
)
target_link_options(
        llama
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)

# Unified single shared library target using Arm KleidiAI dynamic dispatching
add_library(
        smollm
        SHARED
        LLMRunner.cpp
        LLMInference.cpp
        smollm.cpp
)

target_include_directories(
        smollm
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${COMMON_DIR}
        ${GGML_DIR}/include
        ${GGML_DIR}/src
        ${GGML_DIR}/src/ggml-cpu
        ${LLAMA_DIR}/include
        ${VENDOR_DIR}
)

target_compile_definitions(
        smollm
        PRIVATE
        GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
        GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
        GGML_USE_CPU
        GGML_USE_CPU_AARCH64
)

target_compile_options(
        smollm
        PUBLIC
        -march=armv8-a
        -O3
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -ffunction-sections
        -fdata-sections
)

target_link_libraries(
        smollm
        android
        log
        llama
        common
        vulkan
)

target_link_options(
        smollm
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)

# Library target for GGUFReader
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