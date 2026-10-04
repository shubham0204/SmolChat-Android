#pragma once

#include "chat.h"
#include "common.h"
#include "llama.h"

#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <mutex>
#include <atomic>
#include <algorithm>

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

    bool load_model(const std::string& model_path, const RunnerParams& params);
    std::vector<llama_token> tokenize(const std::string& prompt, bool add_special = true, bool parse_special = true);
    bool generate(const std::vector<llama_token>& tokens, TokenCallback callback_stream);

    void add_chat_message(const std::string& role, const std::string& message);
    std::pair<std::string, bool> format_chat_prompt(const std::string& user_query);

    // start_completion: throws std::runtime_error on error, returns false if jinja fallback was used
    bool start_completion(const std::string& query);
    std::string completion_loop();
    void stop_completion();

    float get_tokens_per_second() const;
    int get_context_size_used() const;
    std::string bench_model(int pp, int tg, int pl, int nr);

private:
    void free_resources();
    void pop_last_message();  // free + pop_back helper
    void pop_dangling_user(); // if last message is "user" and response is empty, remove it

    llama_model*   m_model   = nullptr;
    llama_context* m_ctx     = nullptr;
    llama_sampler* m_sampler = nullptr;
    common_chat_templates_ptr m_chat_templates;

    RunnerParams m_params;
    std::string  m_chat_template_str;

    std::vector<llama_chat_message> m_messages;
    std::vector<llama_token>        m_prompt_tokens;
    llama_batch*                    m_step_batch  = nullptr;
    llama_token                     m_curr_token  = 0;

    std::string m_accumulated_response;
    std::string m_utf8_token_cache;

    int64_t  m_generation_time_us    = 0;
    long     m_generated_tokens_count = 0;
    long     m_timed_tokens_count    = 0;
    uint32_t m_n_ctx_used            = 0;

    // m_ctx_mutex:  held for the lifetime of every llama_decode call
    // m_mutex:      protects m_accumulated_response and m_messages
    // Lock order everywhere: m_ctx_mutex first, m_mutex second.
    std::mutex m_ctx_mutex;
    std::mutex m_mutex;
    std::atomic<bool> m_is_stopping{false};
    std::atomic<bool> m_is_generating{false};
};

} // namespace smollm
