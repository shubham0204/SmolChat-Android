cmake_minimum_required(VERSION 3.10)
add_link_options("LINKER:--build-id=none")

# 16KB Page Size Support, linker flags for 16KB page size alignment on Android 15+ devices
add_link_options("LINKER:-z,max-page-size=16384")

project(whisper.cpp)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_LIB_DIR ${CMAKE_SOURCE_DIR}/..)

# Remove file paths from debug info
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")

# Set consistent release flags - ADDED
set(CMAKE_C_FLAGS_RELEASE "-O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")

# Disable timestamps - ADDED
add_definitions(-DGGML_NO_TIMESTAMPS=1)

# 16KB Page Size Support: Add NDK r27 flexible page size compilation flag
add_definitions(-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON)

# Path to external GGML, otherwise uses the copy in whisper.cpp.
option(GGML_HOME "whisper: Path to external GGML source" OFF)

set(
        SOURCE_FILES
        ${WHISPER_LIB_DIR}/src/whisper.cpp
        ${CMAKE_SOURCE_DIR}/jni.c
)

# TODO: this needs to be updated to work with the new ggml CMakeLists

if (NOT GGML_HOME)
    set(
            SOURCE_FILES
            ${SOURCE_FILES}
            ${WHISPER_LIB_DIR}/ggml/src/ggml.c
            ${WHISPER_LIB_DIR}/ggml/src/ggml-alloc.c
            ${WHISPER_LIB_DIR}/ggml/src/ggml-backend.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-backend-reg.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-quants.c
            ${WHISPER_LIB_DIR}/ggml/src/ggml-threading.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-hbm.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-quants.c
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ggml-cpu-traits.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/unary-ops.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/binary-ops.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/vec.cpp
            ${WHISPER_LIB_DIR}/ggml/src/ggml-cpu/ops.cpp
    )
endif()

find_library(LOG_LIB log)

function(build_library target_name)
    add_library(
            ${target_name}
            SHARED
            ${SOURCE_FILES}
    )

    target_compile_definitions(${target_name} PUBLIC GGML_USE_CPU)

    # Add reproducible build definitions - ADDED
    target_compile_definitions(${target_name} PRIVATE GGML_NO_TIMESTAMPS=1)

    if (${target_name} STREQUAL "whisper_v8fp16_va")
        target_compile_options(${target_name} PRIVATE -march=armv8.2-a+fp16)
        set(GGML_COMPILE_OPTIONS                      -march=armv8.2-a+fp16)
    elseif (${target_name} STREQUAL "whisper_vfpv4")
        target_compile_options(${target_name} PRIVATE -mfpu=neon-vfpv4)
        set(GGML_COMPILE_OPTIONS                      -mfpu=neon-vfpv4)
    endif ()

    if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
        # Use consistent optimization flags - MODIFIED
        target_compile_options(${target_name} PRIVATE -O3 -DNDEBUG)
        target_compile_options(${target_name} PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
        target_compile_options(${target_name} PRIVATE -ffunction-sections -fdata-sections)

        target_link_options(${target_name} PRIVATE -Wl,--gc-sections)
        target_link_options(${target_name} PRIVATE -Wl,--exclude-libs,ALL)
        target_link_options(${target_name} PRIVATE -flto)

        # 16KB Page Size Support, linker flags for 16KB page size alignment on Android 15+ devices
        target_link_options(${target_name} PRIVATE -Wl,-z,max-page-size=16384)
    endif ()

    if (GGML_HOME)
        include(FetchContent)
        FetchContent_Declare(ggml SOURCE_DIR ${GGML_HOME})
        FetchContent_MakeAvailable(ggml)

        target_compile_options(ggml PRIVATE ${GGML_COMPILE_OPTIONS})
        # Add reproducible build flags to ggml as well - ADDED
        target_compile_definitions(ggml PRIVATE GGML_NO_TIMESTAMPS=1)
        target_link_libraries(${target_name} ${LOG_LIB} android ggml)
    else()
        target_link_libraries(${target_name} ${LOG_LIB} android)
    endif()

    # Strip .comment section for reproducible builds
    add_custom_command(TARGET ${target_name} POST_BUILD
            COMMAND ${CMAKE_OBJCOPY} --remove-section .comment $<TARGET_FILE:${target_name}>
            COMMENT "Removing .comment section from ${target_name} for reproducible builds"
    )

endfunction()

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    build_library("whisper_v8fp16_va")
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    build_library("whisper_vfpv4")
endif ()

build_library("whisper") # Default target

include_directories(${WHISPER_LIB_DIR})
include_directories(${WHISPER_LIB_DIR}/src)
include_directories(${WHISPER_LIB_DIR}/include)
include_directories(${WHISPER_LIB_DIR}/ggml/include)
include_directories(${WHISPER_LIB_DIR}/ggml/src)
include_directories(${WHISPER_LIB_DIR}/ggml/src/ggml-cpu)
