# Building llama.cpp for ARM-specific CPU flags

- llama.cpp can be compiled with Arm64-specific CPU extensions to accelerate inference on supported devices.
- Referring [llama.rn](https://github.com/mybigday/llama.rn), the React Native bindings for llama.cpp, we 
  build/compile multiple shared libraries, each 
  targeting a specific set of CPU extensions and a Arm64-v8 version. The app then, at runtime, loads the appropriate shared library by determining the CPU extensions available on the device and the Arm version using `System.loadLibrary`.
- To see how multiple shared libraries are compiled, check [`smollm/src/main/cpp/CMakeLists.txt`](https://github.com/shubham0204/SmolChat-Android/blob/main/smollm/src/main/cpp/CMakeLists.txt).
- To see how the app loads the appropriate shared library, check [`smollm/src/main/java/io/shubham0204/smollm/SmolLM.kt`](https://github.com/shubham0204/SmolChat-Android/blob/main/smollm/src/main/java/io/shubham0204/smollm/SmolLM.kt).

> [!NOTE]
> The APK contains multiple `.so` files for the `arm64-v8a` ABI. As the size of each `.so` file < 1 MB, the increase 
> in the size of the APK should be insignificant.

### CPU Extensions

We are compiling against the following [Arm64-specific CPU flags (feature modifiers)](https://gcc.gnu.org/onlinedocs/gcc/AArch64-Options.html#aarch64-feature-modifiers):

- `fp16`: Enable FP16 extension. This also enables floating-point instructions.

- `dotprod`: Enable the Dot Product extension. This also enables Advanced SIMD instructions.

- `i8mm`: Enable 8-bit Integer Matrix Multiply instructions. This also enables Advanced SIMD and floating-point instructions. This option is enabled by default for -march=armv8.6-a. Use of this option with architectures prior to Armv8.2-A is not supported.

- `sve`: Enable Scalable Vector Extension instructions. This also enables Advanced SIMD and floating-point instructions.

### Configuring CMake

- In [`smollm/src/main/cpp/CMakeLists.txt`](https://github.com/shubham0204/SmolChat-Android/blob/main/smollm/src/main/cpp/CMakeLists.txt), 
we list all source files from llama.cpp and `smollm/src/main/cpp` to compile them into a single target.

- Normally, we would compile the target `smollm` and link it dynamically with targets `llama`, `common` and `ggml` 
defined by llama.cpp. As we need to compile with different CPU flags, we combine the source files of all llama.cpp 
targets and our own JNI bindings into one single target `smollm` and then apply the CPU flags with `target_compile_options`.
