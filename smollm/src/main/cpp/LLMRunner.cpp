#include "LLMRunner.h"

#include <android/log.h>
#include <cmath>
#include <cstring>
#include <iomanip>
#include <sstream>
#include <stdexcept>

#define TAG "[SmolLM-LLMRunner]"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace smollm {

LLMRunner::LLMRunner() = default;

LLMRunner::~LLMRunner() {
    free_resources();
}

void LLMRunner::free_resources() {
    for (auto& msg : m_messages) {
        free(const_cast<char*>(msg.role));
        free(const_cast<char*>(msg.content));
    }
    m_messages.clear();

    if (m_step_batch) {
        delete m_step_batch;
        m_step_batch = nullptr;
    }
    if (m_sampler) {
        llama_sampler_free(m_sampler);
        m_sampler = nullptr;
    }
    if (m_ctx) {
        llama_free(m_ctx);
        m_ctx = nullptr;
    }
    if (m_model) {
        llama_model_free(m_model);
        m_model = nullptr;
    }
}

bool LLMRunner::load_model(const std::string& model_path, const RunnerParams& params) {
    LOGi("Runner::load_model loading: %s (threads=%d, ctx=%ld, mmap=%d)",
         model_path.c_str(), params.nThreads, params.contextSize, params.useMmap);

    free_resources();
    m_params = params;

    // Initialize ggml dynamic backends (CPU/KleidiAI, Vulkan if available)
    ggml_backend_load_all();

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap  = params.useMmap;
    model_params.use_mlock = params.useMlock;

    m_model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (!m_model) {
        LOGe("Runner::load_model failed to load model from %s", model_path.c_str());
        return false;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx     = params.contextSize;
    ctx_params.n_batch   = params.contextSize;
    ctx_params.n_threads = params.nThreads;
    ctx_params.no_perf   = true;

    m_ctx = llama_init_from_model(m_model, ctx_params);
    if (!m_ctx) {
        LOGe("Runner::load_model llama_init_from_model returned null");
        llama_model_free(m_model);
        m_model = nullptr;
        return false;
    }

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;
    m_sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(m_sampler, llama_sampler_init_temp(params.temperature));
    llama_sampler_chain_add(m_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    if (params.chatTemplate.empty()) {
        const char* tmpl = llama_model_chat_template(m_model, nullptr);
        m_chat_template = tmpl ? tmpl : "";
    } else {
        m_chat_template = params.chatTemplate;
    }

    LOGi("Runner::load_model success");
    return true;
}

std::vector<llama_token> LLMRunner::tokenize(const std::string& prompt, bool add_special, bool parse_special) {
    if (!m_model) {
        LOGe("Runner::tokenize called with null model");
        return {};
    }
    return common_tokenize(llama_model_get_vocab(m_model), prompt, add_special, parse_special);
}

void LLMRunner::add_chat_message(const std::string& role, const std::string& message) {
    m_messages.push_back({strdup(role.c_str()), strdup(message.c_str())});
}

std::pair<std::string, bool> LLMRunner::format_chat_prompt(const std::string& user_query) {
    add_chat_message("user", user_query);

    std::vector<common_chat_msg> messages;
    for (const auto& msg : m_messages) {
        common_chat_msg cmsg;
        cmsg.role    = msg.role;
        cmsg.content = msg.content;
        messages.push_back(cmsg);
    }

    auto templates = common_chat_templates_init(m_model, m_chat_template.c_str());
    common_chat_templates_inputs inputs;
    inputs.messages = messages;
    inputs.use_jinja = true;
    inputs.chat_template_kwargs["tools"] = "[]";

    std::string prompt;
    bool used_jinja = true;
    try {
        prompt = common_chat_templates_apply(templates.get(), inputs).prompt;
    } catch (const std::exception& e) {
        LOGi("Jinja template formatting failed: %s, falling back to legacy", e.what());
        inputs.use_jinja = false;
        inputs.chat_template_kwargs.clear();
        prompt = common_chat_templates_apply(templates.get(), inputs).prompt;
        used_jinja = false;
    }

    return {prompt, used_jinja};
}

bool LLMRunner::generate(const std::vector<llama_token>& tokens, TokenCallback callback_stream) {
    if (!m_ctx || !m_model || !m_sampler || tokens.empty()) {
        return false;
    }

    m_generation_time_us = 0;
    m_generated_tokens_count = 0;
    m_accumulated_response.clear();
    m_utf8_token_cache.clear();

    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); ++i) {
        common_batch_add(batch, tokens[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(m_ctx, batch) != 0) {
        LOGe("Runner::generate prompt evaluation failed");
        llama_batch_free(batch);
        return false;
    }
    llama_batch_free(batch);

    llama_batch step_batch = llama_batch_init(1, 0, 1);
    const uint32_t context_size = llama_n_ctx(m_ctx);
    bool should_continue = true;

    while (should_continue) {
        m_n_ctx_used = llama_memory_seq_pos_max(llama_get_memory(m_ctx), 0) + 1;
        if (m_n_ctx_used >= context_size) {
            LOGi("Context size limit reached: %d >= %u", m_n_ctx_used, context_size);
            break;
        }

        auto t0 = ggml_time_us();
        llama_token token = llama_sampler_sample(m_sampler, m_ctx, -1);

        if (llama_vocab_is_eog(llama_model_get_vocab(m_model), token)) {
            break;
        }

        std::string piece = common_token_to_piece(m_ctx, token, true);
        auto t1 = ggml_time_us();
        m_generation_time_us += (t1 - t0);
        m_generated_tokens_count++;

        m_utf8_token_cache += piece;
        if (is_valid_utf8(m_utf8_token_cache.c_str())) {
            m_accumulated_response += m_utf8_token_cache;
            if (callback_stream) {
                should_continue = callback_stream(m_utf8_token_cache);
            }
            m_utf8_token_cache.clear();
        }

        if (!should_continue) {
            break;
        }

        common_batch_clear(step_batch);
        common_batch_add(step_batch, token, m_n_ctx_used, {0}, true);
        if (llama_decode(m_ctx, step_batch) != 0) {
            LOGe("llama_decode step failed");
            break;
        }
    }

    llama_batch_free(step_batch);

    if (m_params.storeChats && !m_accumulated_response.empty()) {
        add_chat_message("assistant", m_accumulated_response);
    }
    m_accumulated_response.clear();
    return true;
}

bool LLMRunner::start_completion(const std::string& query) {
    if (!m_params.storeChats) {
        for (auto& msg : m_messages) {
            free(const_cast<char*>(msg.role));
            free(const_cast<char*>(msg.content));
        }
        m_messages.clear();
    }

    m_generation_time_us = 0;
    m_generated_tokens_count = 0;
    m_accumulated_response.clear();
    m_utf8_token_cache.clear();

    auto [prompt, used_jinja] = format_chat_prompt(query);
    m_prompt_tokens = tokenize(prompt, true, true);

    if (m_step_batch) {
        delete m_step_batch;
    }
    m_step_batch = new llama_batch();
    m_step_batch->token    = m_prompt_tokens.data();
    m_step_batch->n_tokens = m_prompt_tokens.size();

    return used_jinja;
}

std::string LLMRunner::completion_loop() {
    if (!m_ctx || !m_model || !m_step_batch) {
        throw std::runtime_error("Runner not initialized for completion");
    }

    uint32_t context_size = llama_n_ctx(m_ctx);
    m_n_ctx_used = llama_memory_seq_pos_max(llama_get_memory(m_ctx), 0) + 1;
    if (m_n_ctx_used + m_step_batch->n_tokens > context_size) {
        throw std::runtime_error("Context size limit reached");
    }

    auto start = ggml_time_us();
    if (llama_decode(m_ctx, *m_step_batch) < 0) {
        throw std::runtime_error("llama_decode() failed");
    }

    m_curr_token = llama_sampler_sample(m_sampler, m_ctx, -1);
    if (llama_vocab_is_eog(llama_model_get_vocab(m_model), m_curr_token)) {
        if (m_params.storeChats && !m_accumulated_response.empty()) {
            add_chat_message("assistant", m_accumulated_response);
        }
        m_accumulated_response.clear();
        return "[EOG]";
    }

    std::string piece = common_token_to_piece(m_ctx, m_curr_token, true);
    auto end = ggml_time_us();
    m_generation_time_us += (end - start);
    m_generated_tokens_count += 1;
    m_utf8_token_cache += piece;

    m_step_batch->token    = &m_curr_token;
    m_step_batch->n_tokens = 1;

    if (is_valid_utf8(m_utf8_token_cache.c_str())) {
        m_accumulated_response += m_utf8_token_cache;
        std::string valid_piece = m_utf8_token_cache;
        m_utf8_token_cache.clear();
        return valid_piece;
    }

    return "";
}

void LLMRunner::stop_completion() {
    if (m_params.storeChats && !m_accumulated_response.empty()) {
        add_chat_message("assistant", m_accumulated_response);
    }
    m_accumulated_response.clear();
}

float LLMRunner::get_tokens_per_second() const {
    if (m_generation_time_us <= 0) return 0.0f;
    return static_cast<float>(m_generated_tokens_count) / (static_cast<float>(m_generation_time_us) / 1e6f);
}

int LLMRunner::get_context_size_used() const {
    return m_n_ctx_used;
}

bool LLMRunner::is_valid_utf8(const char* str) const {
    if (!str) return true;
    const auto* bytes = reinterpret_cast<const unsigned char*>(str);
    while (*bytes != 0x00) {
        int num = 0;
        if ((*bytes & 0x80) == 0x00)        num = 1;
        else if ((*bytes & 0xE0) == 0xC0)   num = 2;
        else if ((*bytes & 0xF0) == 0xE0)   num = 3;
        else if ((*bytes & 0xF8) == 0xF0)   num = 4;
        else return false;

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) return false;
            bytes += 1;
        }
    }
    return true;
}

std::string LLMRunner::bench_model(int pp, int tg, int pl, int nr) {
    llama_batch g_batch = llama_batch_init(pp, 0, pl);
    auto pp_avg = 0.0;
    auto tg_avg = 0.0;
    auto pp_std = 0.0;
    auto tg_std = 0.0;

    const uint32_t n_ctx = llama_n_ctx(m_ctx);
    LOGi("bench_model: n_ctx = %u", n_ctx);

    // WARMUP PASS: prime the CPU caches and KleidiAI kernels
    LOGi("bench_model: running warmup pass");
    common_batch_clear(g_batch);
    for (int i = 0; i < pp; i++) {
        common_batch_add(g_batch, 1, i, {0}, false);
    }
    g_batch.logits[g_batch.n_tokens - 1] = true;
    llama_decode(m_ctx, g_batch);
    llama_memory_clear(llama_get_memory(m_ctx), false);

    // BENCHMARK LOOP
    for (int nri = 0; nri < nr; nri++) {
        common_batch_clear(g_batch);
        for (int i = 0; i < pp; i++) {
            common_batch_add(g_batch, 1, i, {0}, false);
        }
        g_batch.logits[g_batch.n_tokens - 1] = true;
        
        const auto t_pp_start = ggml_time_us();
        llama_decode(m_ctx, g_batch);
        const auto t_pp_end = ggml_time_us();

        llama_memory_clear(llama_get_memory(m_ctx), false);
        const auto t_tg_start = ggml_time_us();
        for (int i = 0; i < tg; i++) {
            common_batch_clear(g_batch);
            for (int j = 0; j < pl; j++) {
                common_batch_add(g_batch, 0, i, {j}, true);
            }
            llama_decode(m_ctx, g_batch);
        }
        const auto t_tg_end = ggml_time_us();

        llama_memory_clear(llama_get_memory(m_ctx), false);

        const auto t_pp = double(t_pp_end - t_pp_start) / 1000000.0;
        const auto t_tg = double(t_tg_end - t_tg_start) / 1000000.0;
        const auto speed_pp = double(pp) / t_pp;
        const auto speed_tg = double(pl * tg) / t_tg;

        pp_avg += speed_pp;
        tg_avg += speed_tg;
        pp_std += speed_pp * speed_pp;
        tg_std += speed_tg * speed_tg;
    }

    llama_batch_free(g_batch);

    pp_avg /= double(nr);
    tg_avg /= double(nr);
    if (nr > 1) {
        pp_std = sqrt(pp_std / double(nr - 1) - pp_avg * pp_avg * double(nr) / double(nr - 1));
        tg_std = sqrt(tg_std / double(nr - 1) - tg_avg * tg_avg * double(nr) / double(nr - 1));
    } else {
        pp_std = 0;
        tg_std = 0;
    }
    
    // TTFT is basically the time it takes to do 1 prompt processing pass (in milliseconds)
    double ttft_ms = (double(pp) / pp_avg) * 1000.0;

    char model_desc[128];
    llama_model_desc(m_model, model_desc, sizeof(model_desc));

    const auto model_size     = double(llama_model_size(m_model)) / 1024.0 / 1024.0 / 1024.0;
    const auto model_n_params = double(llama_model_n_params(m_model)) / 1e9;

    std::vector<std::string> backends;
    for (size_t i = 0; i < ggml_backend_reg_count(); i++) {
        auto* reg = ggml_backend_reg_get(i);
        std::string name = ggml_backend_reg_name(reg);
        if (name != "CPU") {
            backends.push_back(name);
        }
    }
    std::ostringstream str;
    for (size_t i = 0; i < backends.size(); i++) {
        str << backends[i];
        if (i < backends.size() - 1) str << ",";
    }

    std::stringstream result;
    result << std::setprecision(3);
    result << "| model | size | params | backend | test | t/s |\n";
    result << "| --- | --- | --- | --- | --- | --- |\n";
    result << "| " << model_desc << " | " << model_size << "GiB | " << model_n_params << "B | " << str.str() << " | pp "
           << pp << " | " << pp_avg << " ± " << pp_std << " |\n";
    result << "| " << model_desc << " | " << model_size << "GiB | " << model_n_params << "B | " << str.str() << " | tg "
           << tg << " | " << tg_avg << " ± " << tg_std << " |\n";
    result << "\n**TTFT (Time To First Token)**: " << ttft_ms << " ms\n";
    return result.str();
}

} // namespace smollm

