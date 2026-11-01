- OpenAI-compatible HTTP server that could list models and perform LLM inference.
- Added a button to copy code when code blocks are rendered in the chat.
- Models are downloading directly instead of using the Android system's download service.
- Optimized Markdown rendering to reduce CPU usage when rendering long chats.
- Support global chat settings that are applicable to all new chats by default.
- Fix measurement of tokens/sec in the chat. It now shows the token generation rather than
  accounting for prompt processing time.
- min-p from chat settings is now passed to the llama.cpp
- Sync with upstream llama.cpp