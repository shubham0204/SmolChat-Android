#include "LLMRunner.h"

#include <android/log.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <iomanip>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

#define TAG "[SmolLM-LLMRunner]"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace smollm {

// ---------------------------------------------------------------------------
// UTF-8 helpers
// ---------------------------------------------------------------------------

// Drain all complete UTF-8 codepoints from `cache`, replacing invalid lead
// bytes with U+FFFD.  Incomplete-but-valid sequences stay in the cache.
static std::string drain_utf8(std::string& cache) {
    std::string out;
    size_t i = 0, n = cache.size();
    while (i < n) {
        unsigned char c = cache[i];
        size_t len = c < 0x80 ? 1 : (c & 0xE0) == 0xC0 ? 2 :
                     (c & 0xF0) == 0xE0 ? 3 : (c & 0xF8) == 0xF0 ? 4 : 0;
        bool ok = len > 0;
        size_t avail = std::min(len, n - i);
        for (size_t k = 1; ok && k < avail; ++k) ok = (cache[i + k] & 0xC0) == 0x80;
        if (!ok) { out += "\xEF\xBF\xBD"; ++i; continue; }
        if (i + len > n) break;   // valid but incomplete — wait for next token
        out.append(cache, i, len);
        i += len;
    }
    cache.erase(0, i);
    return out;
}

// Flush leftover bytes that will never be completed (end of generation).
// Emits one U+FFFD for each byte that cannot start a new sequence.
static std::string flush_utf8(std::string& cache) {
    // First drain anything that is actually valid.
    std::string out = drain_utf8(cache);
    // Replace remaining bytes one by one.
    while (!cache.empty()) {
        out += "\xEF\xBF\xBD";
        cache.erase(cache.begin());
        out += drain_utf8(cache);
    }
    return out;
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

LLMRunner::LLMRunner() = default;

LLMRunner::~LLMRunner() {
    m_is_stopping = true;
    // Acquire in canonical order to avoid inversion.
    std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);
    std::lock_guard<std::mutex> lock(m_mutex);
    free_resources();
}

void LLMRunner::pop_last_message() {
    if (m_messages.empty()) return;
    free(const_cast<char*>(m_messages.back().role));
    free(const_cast<char*>(m_messages.back().content));
    m_messages.pop_back();
}

// Remove a dangling user message that was added before generation failed.
// Only called under m_mutex.
void LLMRunner::pop_dangling_user() {
    if (!m_messages.empty()
            && std::string(m_messages.back().role) == "user"
            && m_accumulated_response.empty()) {
        pop_last_message();
    }
}

void LLMRunner::free_resources() {
    for (auto& msg : m_messages) {
        free(const_cast<char*>(msg.role));
        free(const_cast<char*>(msg.content));
    }
    m_messages.clear();
    if (m_step_batch) { delete m_step_batch; m_step_batch = nullptr; }
    if (m_sampler)    { llama_sampler_free(m_sampler); m_sampler = nullptr; }
    m_chat_templates.reset();
    if (m_ctx)   { llama_free(m_ctx);          m_ctx   = nullptr; }
    if (m_model) { llama_model_free(m_model);  m_model = nullptr; }
}

// ---------------------------------------------------------------------------
// load_model
// Canonical lock order: ctx_mutex first, mutex second.
// ---------------------------------------------------------------------------

bool LLMRunner::load_model(const std::string& model_path, const RunnerParams& params) {
    m_is_stopping  = true;   // signal any running decode to bail out
    m_is_generating = false;
    std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);   // 1st
    std::lock_guard<std::mutex> lock(m_mutex);            // 2nd
    m_is_stopping = false;

    LOGi("Runner::load_model loading: %s (threads=%d, ctx=%ld, mmap=%d)",
         model_path.c_str(), params.nThreads, params.contextSize, params.useMmap);

    free_resources();
    m_params = params;

    static std::once_flag s_backends_once;
    std::call_once(s_backends_once, [] { ggml_backend_load_all(); });

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap  = params.useMmap;
    model_params.use_mlock = params.useMlock;

    m_model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (!m_model) {
        LOGe("Runner::load_model failed to load model from %s", model_path.c_str());
        return false;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx           = params.contextSize > 0 ? (uint32_t)params.contextSize : 2048u;
    ctx_params.n_batch         = ctx_params.n_ctx;
    ctx_params.n_threads       = params.nThreads;
    ctx_params.n_threads_batch = params.nThreads;
    ctx_params.no_perf         = true;

    m_ctx = llama_init_from_model(m_model, ctx_params);
    if (!m_ctx) {
        LOGe("Runner::load_model llama_init_from_model returned null");
        llama_model_free(m_model); m_model = nullptr;
        return false;
    }

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;
    m_sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(m_sampler, llama_sampler_init_min_p(0.05f, 1));
    llama_sampler_chain_add(m_sampler, llama_sampler_init_temp(params.temperature));
    llama_sampler_chain_add(m_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    if (params.chatTemplate.empty()) {
        const char* tmpl = llama_model_chat_template(m_model, nullptr);
        m_chat_template_str = tmpl ? tmpl : "";
    } else {
        m_chat_template_str = params.chatTemplate;
    }
    try {
        m_chat_templates = common_chat_templates_init(m_model, m_chat_template_str);
    } catch (const std::exception& e) {
        LOGe("Runner::load_model: chat template init failed: %s", e.what());
        free_resources();
        return false;
    }

    LOGi("Runner::load_model success");
    return true;
}

// ---------------------------------------------------------------------------
// Tokenize / message helpers
// ---------------------------------------------------------------------------

std::vector<llama_token> LLMRunner::tokenize(const std::string& prompt,
                                             bool add_special, bool parse_special) {
    if (!m_model) { LOGe("Runner::tokenize called with null model"); return {}; }
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

    common_chat_templates_inputs inputs;
    inputs.messages = messages;
    inputs.use_jinja = true;
    inputs.chat_template_kwargs["tools"] = "[]";

    std::string prompt;
    bool used_jinja = true;
    try {
        prompt = common_chat_templates_apply(m_chat_templates.get(), inputs).prompt;
    } catch (const std::exception& e) {
        LOGi("Jinja failed (%s), falling back to legacy", e.what());
        inputs.use_jinja = false;
        inputs.chat_template_kwargs.clear();
        prompt = common_chat_templates_apply(m_chat_templates.get(), inputs).prompt;
        used_jinja = false;
    }
    return {prompt, used_jinja};
}

// ---------------------------------------------------------------------------
// generate()  — standalone (non-JNI-loop) path
// ---------------------------------------------------------------------------

bool LLMRunner::generate(const std::vector<llama_token>& tokens, TokenCallback callback_stream) {
    m_is_stopping = false;
    m_is_generating = true;

    // Clears m_is_generating on every exit path, including exceptions thrown
    // by callback_stream. (Local class: same access rights as this member.)
    struct GenGuard {
        LLMRunner* self;
        ~GenGuard() { self->m_is_generating = false; }
    } gen_guard{this};

    // Validity check, memory clear, and context_size capture all under the lock.
    uint32_t context_size = 0;
    {
        std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);
        if (!m_ctx || !m_model || !m_sampler || tokens.empty()) {
            return false;
        }
        llama_memory_clear(llama_get_memory(m_ctx), false);
        context_size = llama_n_ctx(m_ctx);
        // clear utf8 cache here since ctx_mutex owns it
        m_utf8_token_cache.clear();
    }

    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_generation_time_us     = 0;
        m_generated_tokens_count = 0;
        m_timed_tokens_count     = 0;
        m_accumulated_response.clear();
    }

    // Prefill
    {
        std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);
        if (m_is_stopping) return true;

        llama_batch batch = llama_batch_init((int32_t)tokens.size(), 0, 1);
        for (size_t i = 0; i < tokens.size(); ++i)
            common_batch_add(batch, tokens[i], (int32_t)i, {0}, false);
        batch.logits[batch.n_tokens - 1] = true;
        bool ok = (llama_decode(m_ctx, batch) == 0);
        llama_batch_free(batch);
        if (!ok) {
            LOGe("Runner::generate prompt evaluation failed");
            return false;
        }
    }

    llama_batch step_batch = llama_batch_init(1, 0, 1);
    // Frees step_batch on every exit path, including exceptions.
    struct BatchFree {
        llama_batch& b;
        ~BatchFree() { llama_batch_free(b); }
    } step_free{step_batch};

    bool should_continue = true;

    while (should_continue && !m_is_stopping) {
        std::string out;
        bool eog          = false;
        bool decode_failed = false;

        {
            std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);
            if (m_is_stopping) break;

            m_n_ctx_used = llama_memory_seq_pos_max(llama_get_memory(m_ctx), 0) + 1;
            if (m_n_ctx_used >= context_size) {
                LOGi("generate: context full (%u)", context_size); break;
            }

            auto t0 = ggml_time_us();
            llama_token token = llama_sampler_sample(m_sampler, m_ctx, -1);
            if (llama_vocab_is_eog(llama_model_get_vocab(m_model), token)) { eog = true; }
            else {
                std::string piece = common_token_to_piece(m_ctx, token, true);
                m_generated_tokens_count++;
                m_timed_tokens_count++;

                m_utf8_token_cache += piece;
                out = drain_utf8(m_utf8_token_cache);

                common_batch_clear(step_batch);
                common_batch_add(step_batch, token, m_n_ctx_used, {0}, true);
                decode_failed = (llama_decode(m_ctx, step_batch) != 0);
                if (decode_failed) LOGe("generate: decode step failed");
            }
            auto t1 = ggml_time_us();
            m_generation_time_us += (t1 - t0);
        }

        // Emit outside both locks.
        if (!out.empty()) {
            {
                std::lock_guard<std::mutex> lock(m_mutex);
                m_accumulated_response += out;
            }
            if (callback_stream) should_continue = callback_stream(out);
        }
        if (eog || decode_failed) break;
    }

    // Flush UTF-8 tail (cache owned by ctx_mutex).
    std::string tail;
    {
        std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);
        tail = flush_utf8(m_utf8_token_cache);
    }
    if (!tail.empty()) {
        {
            std::lock_guard<std::mutex> lock(m_mutex);
            m_accumulated_response += tail;
        }
        if (callback_stream && should_continue && !m_is_stopping) callback_stream(tail);
    }

    {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (!m_is_stopping && m_params.storeChats && !m_accumulated_response.empty())
            add_chat_message("assistant", m_accumulated_response);
    }
    return true;
}

// ---------------------------------------------------------------------------
// start_completion
// Canonical lock order: ctx_mutex first, mutex second.
// ---------------------------------------------------------------------------

bool LLMRunner::start_completion(const std::string& query) {
    m_is_stopping = true;                                   // ask any running loop to bail
    std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);     // 1st — matches completion_loop
    std::lock_guard<std::mutex> lock(m_mutex);             // 2nd
    m_is_stopping = false;

    if (!m_ctx || !m_model)
        throw std::runtime_error("Model not loaded");

    const uint32_t context_size = llama_n_ctx(m_ctx);
    const uint32_t reserve      = std::min(256u, context_size / 4u);

    // Fail fast on an oversized query BEFORE touching history, so a query that
    // can never fit doesn't first trim away the whole conversation.
    // (Lower bound: ignores chat-template overhead.)
    if (tokenize(query, false, false).size() + reserve >= context_size)
        throw std::runtime_error("Query too large for context");

    // Save any interrupted turn before we clear state.
    if (m_params.storeChats && !m_accumulated_response.empty()) {
        add_chat_message("assistant", m_accumulated_response);
    } else {
        pop_dangling_user();
    }

    llama_memory_clear(llama_get_memory(m_ctx), false);    // safe: ctx_lock held
    // Invalidate any stale step_batch so a stray completion_loop throws "not initialized"
    // rather than decoding freed m_prompt_tokens storage.
    if (m_step_batch) { delete m_step_batch; m_step_batch = nullptr; }
    m_is_generating = false;  // reset; set true again at the very end on success

    if (!m_params.storeChats) {
        for (auto& msg : m_messages) {
            free(const_cast<char*>(msg.role));
            free(const_cast<char*>(msg.content));
        }
        m_messages.clear();
    }

    m_generation_time_us     = 0;
    m_generated_tokens_count = 0;
    m_timed_tokens_count     = 0;
    m_accumulated_response.clear();
    m_utf8_token_cache.clear();

    auto first_trimmable = [&]() -> size_t {
        return (!m_messages.empty()
                && std::string(m_messages[0].role) == "system") ? 1u : 0u;
    };

    bool used_jinja = false;
    while (true) {
        std::pair<std::string, bool> r;
        try {
            r = format_chat_prompt(query);
        } catch (...) {
            pop_last_message();
            throw;
        }
        used_jinja      = r.second;
        m_prompt_tokens = tokenize(r.first, true, true);
        
        // Remove double BOS if Jinja added one and add_special=true also added one.
        if (m_prompt_tokens.size() >= 2 && m_prompt_tokens[0] == m_prompt_tokens[1] &&
            m_prompt_tokens[0] == llama_vocab_bos(llama_model_get_vocab(m_model))) {
            m_prompt_tokens.erase(m_prompt_tokens.begin());
        }
        
        pop_last_message();       // remove trial "user" message

        if (m_prompt_tokens.size() + reserve < context_size) {
            add_chat_message("user", query);
            break;
        }

        const size_t first = first_trimmable();
        if (m_messages.size() <= first)
            throw std::runtime_error("Prompt too large for context even after trimming");

        // Drop the oldest complete turn starting at `first`.
        size_t end = first + 1;
        while (end < m_messages.size()
               && std::string(m_messages[end].role) != "user") ++end;
        for (size_t i = first; i < end; ++i) {
            free(const_cast<char*>(m_messages[i].role));
            free(const_cast<char*>(m_messages[i].content));
        }
        m_messages.erase(m_messages.begin() + first,
                          m_messages.begin() + end);
    }

    if (m_step_batch) delete m_step_batch;
    m_step_batch           = new llama_batch();
    m_step_batch->token    = m_prompt_tokens.data();
    m_step_batch->n_tokens = (int32_t)m_prompt_tokens.size();

    m_is_generating = true;   // set last — only after everything succeeded
    return used_jinja;
}

// ---------------------------------------------------------------------------
// completion_loop
// Canonical lock order: ctx_mutex first, mutex second.
// ---------------------------------------------------------------------------

std::string LLMRunner::completion_loop() {
    if (m_is_stopping) return "[EOG]";

    // Validity check must be done after acquiring the lock.
    std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);     // 1st
    if (m_is_stopping) return "[EOG]";

    if (!m_ctx || !m_model || !m_step_batch)
        throw std::runtime_error("Runner not initialized for completion");

    const uint32_t context_size = llama_n_ctx(m_ctx);
    m_n_ctx_used = llama_memory_seq_pos_max(llama_get_memory(m_ctx), 0) + 1;

    // Context full: save partial reply gracefully instead of throwing.
    if (m_n_ctx_used + (uint32_t)m_step_batch->n_tokens > context_size) {
        LOGi("completion_loop: context full, saving partial reply");
        std::lock_guard<std::mutex> lock(m_mutex);         // 2nd
        std::string tail = flush_utf8(m_utf8_token_cache);
        if (!tail.empty()) m_accumulated_response += tail;
        if (m_params.storeChats && !m_accumulated_response.empty()) {
            add_chat_message("assistant", m_accumulated_response);
        } else {
            pop_dangling_user();
        }
        m_accumulated_response.clear();
        m_is_generating = false;
        return "[EOG]";
    }

    // Prefill in chunks so stop_completion() and load_model() aren't blocked
    // for seconds while a large prompt is being evaluated.
    constexpr int32_t kPrefillChunk = 512;
    auto start = ggml_time_us();
    llama_batch work        = *m_step_batch;
    const bool still_filling = work.n_tokens > kPrefillChunk;
    if (still_filling) work.n_tokens = kPrefillChunk;

    if (llama_decode(m_ctx, work) != 0) {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (m_params.storeChats && !m_accumulated_response.empty())
            add_chat_message("assistant", m_accumulated_response);
        else
            pop_dangling_user();
        m_accumulated_response.clear();
        m_is_generating = false;
        throw std::runtime_error("llama_decode() failed");
    }

    if (still_filling) {
        // Advance the token pointer so the next call continues where we left off.
        m_step_batch->token    += kPrefillChunk;
        m_step_batch->n_tokens -= kPrefillChunk;
        return "";   // still prefilling; caller loops, stop flag is re-checked each call
    }

    m_curr_token = llama_sampler_sample(m_sampler, m_ctx, -1);

    if (llama_vocab_is_eog(llama_model_get_vocab(m_model), m_curr_token) || m_is_stopping) {
        std::lock_guard<std::mutex> lock(m_mutex);         // 2nd
        std::string tail = flush_utf8(m_utf8_token_cache);
        if (!tail.empty()) m_accumulated_response += tail;
        if (!m_is_stopping && m_params.storeChats && !m_accumulated_response.empty()) {
            add_chat_message("assistant", m_accumulated_response);
        } else if (m_accumulated_response.empty()) {
            pop_dangling_user();                           // empty reply — clean up user msg
        }
        m_accumulated_response.clear();
        m_is_generating = false;
        return "[EOG]";
    }

    std::string piece = common_token_to_piece(m_ctx, m_curr_token, true);
    auto end = ggml_time_us();
    if (m_generated_tokens_count > 0) {
        m_generation_time_us += (end - start);
        m_timed_tokens_count++;
    }
    m_generated_tokens_count++;

    // utf8 cache is always modified while ctx_mutex is held
    m_utf8_token_cache += piece;
    std::string out = drain_utf8(m_utf8_token_cache);

    m_step_batch->token    = &m_curr_token;
    m_step_batch->n_tokens = 1;

    if (!out.empty()) {
        std::lock_guard<std::mutex> lock(m_mutex);         // 2nd
        m_accumulated_response += out;
        return out;
    }
    return "";
}

// ---------------------------------------------------------------------------
// stop_completion
// ---------------------------------------------------------------------------

void LLMRunner::stop_completion() {
    m_is_stopping = true;
    // Don't touch m_utf8_token_cache here — it's owned by ctx_mutex.
    // completion_loop's EOG path will flush it on the next call.
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_params.storeChats && !m_accumulated_response.empty()) {
        add_chat_message("assistant", m_accumulated_response);
    } else {
        pop_dangling_user();
    }
    m_accumulated_response.clear();
    m_is_generating = false;
}

// ---------------------------------------------------------------------------
// Metrics
// ---------------------------------------------------------------------------

float LLMRunner::get_tokens_per_second() const {
    if (m_generation_time_us <= 0 || m_timed_tokens_count == 0) return 0.0f;
    return static_cast<float>(m_timed_tokens_count) /
           (static_cast<float>(m_generation_time_us) / 1e6f);
}

int LLMRunner::get_context_size_used() const {
    return (int)m_n_ctx_used;
}

// ---------------------------------------------------------------------------
// bench_model
// ---------------------------------------------------------------------------

std::string LLMRunner::bench_model(int pp, int tg, int pl, int nr) {
    if (pp <= 0 || tg <= 0 || nr <= 0)
        return "bench_model: pp, tg and nr must all be > 0";
    if (m_is_generating) return "Cannot benchmark while a completion is active.";

    std::lock_guard<std::mutex> ctx_lock(m_ctx_mutex);
    // Re-check under the lock: start_completion may have finished in between.
    if (m_is_generating) return "Cannot benchmark while a completion is active.";
    if (!m_ctx || !m_model) return "Model not loaded";

    if (pl > 1) {
        LOGe("bench_model: clamping pl to 1 (n_seq_max == 1)");
        pl = 1;
    }

    // Clamp pp and tg to hardware limits.
    const uint32_t n_batch = llama_n_batch(m_ctx);
    const uint32_t n_ctx   = llama_n_ctx(m_ctx);
    if ((uint32_t)pp > n_batch) { LOGi("bench_model: clamping pp %d -> %u", pp, n_batch); pp = (int)n_batch; }
    if ((uint32_t)tg > n_ctx)   { LOGi("bench_model: clamping tg %d -> %u", tg, n_ctx);  tg = (int)n_ctx;   }

    llama_batch g_batch = llama_batch_init(pp, 0, pl);
    double pp_avg = 0, tg_avg = 0, pp_std = 0, tg_std = 0;

    LOGi("bench_model: warmup pass");
    llama_memory_clear(llama_get_memory(m_ctx), false);
    common_batch_clear(g_batch);
    for (int i = 0; i < pp; i++) common_batch_add(g_batch, 1, i, {0}, false);
    g_batch.logits[g_batch.n_tokens - 1] = true;
    if (llama_decode(m_ctx, g_batch) != 0) LOGe("bench_model: warmup decode failed");

    int completed = 0;
    for (int nri = 0; nri < nr; nri++) {
        llama_memory_clear(llama_get_memory(m_ctx), false);
        common_batch_clear(g_batch);
        for (int i = 0; i < pp; i++) common_batch_add(g_batch, 1, i, {0}, false);
        g_batch.logits[g_batch.n_tokens - 1] = true;

        const auto t_pp_start = ggml_time_us();
        if (llama_decode(m_ctx, g_batch) != 0) { LOGe("bench_model: pp decode failed"); break; }
        const auto t_pp_end = ggml_time_us();

        llama_memory_clear(llama_get_memory(m_ctx), false);
        const auto t_tg_start = ggml_time_us();
        bool tg_failed = false;
        for (int i = 0; i < tg; i++) {
            common_batch_clear(g_batch);
            for (int j = 0; j < pl; j++) common_batch_add(g_batch, 0, i, {j}, true);
            if (llama_decode(m_ctx, g_batch) != 0) { tg_failed = true; break; }
        }
        const auto t_tg_end = ggml_time_us();
        if (tg_failed) { LOGe("bench_model: tg decode failed"); break; }

        llama_memory_clear(llama_get_memory(m_ctx), false);

        const double t_pp = double(t_pp_end - t_pp_start) / 1e6;
        const double t_tg = double(t_tg_end - t_tg_start) / 1e6;
        const double s_pp = double(pp) / t_pp;
        const double s_tg = double(pl * tg) / t_tg;
        pp_avg += s_pp; tg_avg += s_tg;
        pp_std += s_pp * s_pp; tg_std += s_tg * s_tg;
        completed++;
    }
    llama_batch_free(g_batch);

    if (completed == 0) return "Benchmark failed to complete any iterations.";

    pp_avg /= completed; tg_avg /= completed;
    if (completed > 1) {
        pp_std = std::sqrt(std::max(0.0, pp_std/(completed-1) - pp_avg*pp_avg*completed/(completed-1)));
        tg_std = std::sqrt(std::max(0.0, tg_std/(completed-1) - tg_avg*tg_avg*completed/(completed-1)));
    } else { pp_std = tg_std = 0; }

    const double ttft_ms = (double(pp) / pp_avg) * 1000.0;

    char model_desc[128];
    llama_model_desc(m_model, model_desc, sizeof(model_desc));
    const double model_size = double(llama_model_size(m_model))    / (1024.0*1024.0*1024.0);
    const double model_npar = double(llama_model_n_params(m_model)) / 1e9;

    std::vector<std::string> backends;
    for (size_t i = 0; i < ggml_backend_reg_count(); i++) {
        auto* reg = ggml_backend_reg_get(i);
        std::string name = ggml_backend_reg_name(reg);
        if (name != "CPU") backends.push_back(name);
    }
    std::ostringstream back;
    for (size_t i = 0; i < backends.size(); i++) {
        back << backends[i];
        if (i + 1 < backends.size()) back << ",";
    }

    std::stringstream result;
    result << std::setprecision(3)
           << "| model | size | params | backend | test | t/s |\n"
           << "| --- | --- | --- | --- | --- | --- |\n"
           << "| " << model_desc << " | " << model_size << "GiB | " << model_npar
           << "B | " << back.str() << " | pp " << pp << " | "
           << pp_avg << " ± " << pp_std << " |\n"
           << "| " << model_desc << " | " << model_size << "GiB | " << model_npar
           << "B | " << back.str() << " | tg " << tg << " | "
           << tg_avg << " ± " << tg_std << " |\n"
           << "\n**TTFT (Time To First Token)**: " << ttft_ms << " ms\n";
    return result.str();
}

} // namespace smollm
