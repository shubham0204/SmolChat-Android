- App size is now reduced to half compared to the previous release. This is because the way
  `llama.cpp` was integrated into the project has been changed, allowing for better compiler
  optimizations and LTO (link-time optimization).

- The app allows benchmarking the model used in a chat. Go to `Settings > Benchmark Model` in any
  chat and click 'Start Benchmarking'. The benchmark results are `pp` (tokens/sec for prompt
  processing) and `tg` (tokens/sec for token generation).