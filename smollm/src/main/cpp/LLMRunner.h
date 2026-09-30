#pragma once

#include "chat.h"
#include "common.h"
#include "llama.h"

#include <functional>
#include <memory>
#include <string>
#include <vector>

namespace smollm {

struct RunnerParams {
    float minP = 0.1f;
    float temperature = 0.8f;
    bool storeChats = true;
    long contextSize = 1024;
    std::string chatTemplate;
    int nThreads = 4;
    bool useMmap = true;
    bool useMlock = false;
};

class LLMRunner {
public:
    using TokenCallback = std::function<bool(const std::string& token_piece)>;

    LLMRunner();
    ~LLMRunner();

    // Standardized Runner lifecycle:
    // 1. Runner::load_model(model_path, params)
    bool load_model(const std::string& model_path, const RunnerParams& params);

    // 2. Runner::tokenize(prompt)
    std::vector<llama_token> tokenize(const std::string& prompt, bool add_special = true, bool parse_special = true);

    // 3. Runner::generate(tokens, callback_stream)
    bool generate(const std::vector<llama_token>& tokens, TokenCallback callback_stream);

    // Conversation management
    void add_chat_message(const std::string& role, const std::string& message);
    std::pair<std::string, bool> format_chat_prompt(const std::string& user_query);

    // Step-by-step completion methods for existing JNI interface
    bool start_completion(const std::string& query);
    std::string completion_loop();
    void stop_completion();

    // Metrics and benchmarking
    float get_tokens_per_second() const;
    int get_context_size_used() const;
    std::string bench_model(int pp, int tg, int pl, int nr);

private:
    bool is_valid_utf8(const char* str) const;
    void free_resources();

    llama_model*   m_model = nullptr;
    llama_context* m_ctx = nullptr;
    llama_sampler* m_sampler = nullptr;

    RunnerParams m_params;
    std::string  m_chat_template;

    std::vector<llama_chat_message> m_messages;
    std::vector<llama_token>        m_prompt_tokens;
    llama_batch*                    m_step_batch = nullptr;
    llama_token                     m_curr_token = 0;

    std::string m_accumulated_response;
    std::string m_utf8_token_cache;

    int64_t m_generation_time_us = 0;
    long    m_generated_tokens_count = 0;
    int     m_n_ctx_used = 0;
};

} // namespace smollm

