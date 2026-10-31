#include "LLMInference.h"
#include "reasoning-budget.h"
#include <android/log.h>
#include <cstring>
#include <iomanip>
#include <iostream>

#define TAG "[SmolLMAndroid-Cpp]"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

void
LLMInference::_rebuildSampler(llama_sampler* reasoningBudgetSampler) {
    if (_sampler) {
        llama_sampler_free(_sampler);
    }
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf                    = true; // disable performance metrics
    _sampler                                  = llama_sampler_chain_init(sampler_params);
    if (reasoningBudgetSampler) {
        llama_sampler_chain_add(_sampler, reasoningBudgetSampler);
    }
    if (_minP >= 0.01f) {
        llama_sampler_chain_add(_sampler, llama_sampler_init_min_p(_minP, 1));
    }
    llama_sampler_chain_add(_sampler, llama_sampler_init_temp(_temperature));
    llama_sampler_chain_add(_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
}

void
LLMInference::setReasoningBudget(int reasoningBudget) {
    _reasoningBudget = reasoningBudget;
}

void
LLMInference::loadModel(const char *model_path, float minP, float temperature, bool storeChats, long contextSize,
                        const char* chatTemplate, int nThreads, bool useMmap, bool useMlock, int reasoningBudget) {
    LOGi("loading model with"
         "\n\tmodel_path = %s"
         "\n\tminP = %f"
         "\n\ttemperature = %f"
         "\n\tstoreChats = %d"
         "\n\tcontextSize = %li"
         "\n\tchatTemplate = %s"
         "\n\tnThreads = %d"
         "\n\tuseMmap = %d"
         "\n\tuseMlock = %d"
         "\n\treasoningBudget = %d",
         model_path, minP, temperature, storeChats, contextSize, chatTemplate, nThreads, useMmap, useMlock,
         reasoningBudget);

    // load dynamic backends
    ggml_backend_load_all();

    // create an instance of llama_model
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = useMmap;
    model_params.use_mlock = useMlock;
    _model = llama_model_load_from_file(model_path, model_params);
    if (!_model) {
        LOGe("failed to load model from %s", model_path);
        throw std::runtime_error("loadModel() failed");
    }

    // create an instance of llama_context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_batch = contextSize;
    ctx_params.n_threads = nThreads;
    ctx_params.no_perf = true; // disable performance metrics
    _ctx = llama_init_from_model(_model, ctx_params);
    if (!_ctx) {
        LOGe("llama_new_context_with_model() returned null)");
        throw std::runtime_error("llama_new_context_with_model() returned null");
    }

    _minP            = minP;
    _temperature     = temperature;
    _reasoningBudget = reasoningBudget;
    _rebuildSampler(nullptr);

    _formattedMessages = std::vector<char>(llama_n_ctx(_ctx));
    _messages.clear();

    if (chatTemplate == nullptr) {
        _chatTemplate = llama_model_chat_template(_model, nullptr);
    } else {
        _chatTemplate = strdup(chatTemplate);
    }
    this->_storeChats = storeChats;
}

void
LLMInference::addChatMessage(const char *message, const char *role) {
    _messages.push_back({strdup(role), strdup(message)});
}

void
LLMInference::clearMessages() {
    for (llama_chat_message& message : _messages) {
        free(const_cast<char*>(message.role));
        free(const_cast<char*>(message.content));
    }
    _messages.clear();
    _response.clear();
    _cacheResponseTokens.clear();
    if (_ctx) {
        llama_memory_clear(llama_get_memory(_ctx), false);
    }
}

float
LLMInference::getResponseGenerationTime() const {
    return (float) _responseNumTokens / (_responseGenerationTime / 1e6);
}

int
LLMInference::getContextSizeUsed() const {
    return _nCtxUsed;
}

bool
LLMInference::startCompletion(const char *query) {
    llama_memory_clear(llama_get_memory(_ctx), false);
    _cacheResponseTokens.clear();

    if (!_storeChats) {
        _formattedMessages.clear();
        _formattedMessages = std::vector<char>(llama_n_ctx(_ctx));
    }
    _responseGenerationTime = 0;
    _responseNumTokens = 0;
    addChatMessage(query, "user");
    // apply the chat-template
    std::vector<common_chat_msg> messages;
    for (const llama_chat_message& message : _messages) {
        common_chat_msg msg;
        msg.role    = message.role;
        msg.content = message.content;
        messages.push_back(msg);
    }
    auto templates = common_chat_templates_init(_model, _chatTemplate ? _chatTemplate : "");

    common_chat_templates_inputs inputs;
    inputs.messages = messages;

    // Try Jinja rendering first with tools defined to prevent "tojson on Undefined" errors.
    // If Jinja fails (e.g. unsupported filters like lstrip), fall back to legacy rendering.
    inputs.use_jinja = true;
    inputs.enable_thinking               = (_reasoningBudget != 0);
    inputs.chat_template_kwargs["tools"] = "[]";

    common_chat_params chatParams;
    bool usedJinja = true;
    try {
        chatParams = common_chat_templates_apply(templates.get(), inputs);
    } catch (const std::exception &e) {
        LOGe("Jinja template failed: %s — retrying with legacy renderer", e.what());
        inputs.use_jinja = false;
        inputs.chat_template_kwargs.clear();
        chatParams = common_chat_templates_apply(templates.get(), inputs);
        usedJinja = false;
    }

    std::string        prompt = chatParams.prompt;
    const llama_vocab* vocab  = llama_model_get_vocab(_model);

    llama_sampler* rbudgetSampler = nullptr;
    if (_reasoningBudget >= 0) {
        std::string startTag = chatParams.thinking_start_tag.empty() ? "<think>" : chatParams.thinking_start_tag;
        std::string endTag   = chatParams.thinking_end_tag.empty() ? "</think>" : chatParams.thinking_end_tag;

        // Check if the generation prompt (or prompt tail) already opened a thinking block without closing it
        const std::string& checkRegion = !chatParams.generation_prompt.empty() ? chatParams.generation_prompt : prompt;
        size_t             lastStart   = checkRegion.rfind(startTag);
        size_t             lastEnd     = checkRegion.rfind(endTag);
        bool               unclosedThinkingInPrompt
            = (lastStart != std::string::npos) && (lastEnd == std::string::npos || lastEnd < lastStart);

        if (_reasoningBudget == 0 && unclosedThinkingInPrompt) {
            // Template hardcoded an unclosed thinking start tag (e.g., DeepSeek-R1-Distill).
            // Immediately close the thinking block in the prompt so 0 thinking tokens are generated.
            prompt += "\n" + endTag + "\n\n";
            unclosedThinkingInPrompt = false;
        }

        std::vector<llama_token> startTokens  = common_tokenize(vocab, startTag, false, true);
        std::vector<llama_token> endTokens    = common_tokenize(vocab, endTag, false, true);
        std::vector<llama_token> forcedTokens = common_tokenize(vocab, "\n" + endTag + "\n\n", false, true);
        if (forcedTokens.empty()) {
            forcedTokens = endTokens;
        }
        common_reasoning_budget_state initialState
            = unclosedThinkingInPrompt ? REASONING_BUDGET_COUNTING : REASONING_BUDGET_IDLE;

        if (!startTokens.empty() && !endTokens.empty()) {
            rbudgetSampler = common_reasoning_budget_init(vocab, startTokens, endTokens, forcedTokens, _reasoningBudget,
                                                          initialState);
        }
    }
    _rebuildSampler(rbudgetSampler);

    _promptTokens = common_tokenize(vocab, prompt, true, true);

    // create a llama_batch containing a single sequence
    // see llama_batch_init for more details
    delete _batch;
    _batch = new llama_batch();
    _batch->token = _promptTokens.data();
    _batch->n_tokens = _promptTokens.size();

    return usedJinja;
}

// taken from:
// https://github.com/ggerganov/llama.cpp/blob/master/examples/llama.android/llama/src/main/cpp/llama-android.cpp#L38
bool
LLMInference::_isValidUtf8(const char *response) {
    if (!response) {
        return true;
    }
    const unsigned char *bytes = (const unsigned char *) response;
    int num;
    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            // U+0000 to U+007F
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            // U+0080 to U+07FF
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            // U+0800 to U+FFFF
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            // U+10000 to U+10FFFF
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }
    return true;
}

std::string
LLMInference::completionLoop() {
    // check if the length of the inputs to the model
    // have exceeded the context size of the model
    uint32_t contextSize = llama_n_ctx(_ctx);
    _nCtxUsed = llama_memory_seq_pos_max(llama_get_memory(_ctx), 0) + 1;
    if (_nCtxUsed + _batch->n_tokens > contextSize) {
        throw std::runtime_error("context size reached");
    }

    auto start = ggml_time_us();
    // run the model
    if (llama_decode(_ctx, *_batch) < 0) {
        throw std::runtime_error("llama_decode() failed");
    }

    // sample a token and check if it is an EOG (end of generation token)
    // convert the integer token to its corresponding word-piece
    _currToken = llama_sampler_sample(_sampler, _ctx, -1);
    if (llama_vocab_is_eog(llama_model_get_vocab(_model), _currToken)) {
        addChatMessage(strdup(_response.data()), "assistant");
        _response.clear();
        return "[EOG]";
    }
    std::string piece = common_token_to_piece(_ctx, _currToken, true);
    auto end = ggml_time_us();
    _responseGenerationTime += (end - start);
    _responseNumTokens += 1;
    _cacheResponseTokens += piece;

    // re-init the batch with the newly predicted token
    // key, value pairs of all previous tokens have been cached
    // in the KV cache
    _batch->token = &_currToken;
    _batch->n_tokens = 1;

    if (_isValidUtf8(_cacheResponseTokens.c_str())) {
        _response += _cacheResponseTokens;
        std::string valid_utf8_piece = _cacheResponseTokens;
        _cacheResponseTokens.clear();
        return valid_utf8_piece;
    }

    return "";
}

void
LLMInference::stopCompletion() {
    if (_storeChats && !_response.empty()) {
        addChatMessage(_response.c_str(), "assistant");
    }
    _response.clear();
}

LLMInference::~LLMInference() {
    // free memory held by the message text in messages
    // (as we had used strdup() to create a malloc'ed copy)
    for (llama_chat_message &message: _messages) {
        free(const_cast<char *>(message.role));
        free(const_cast<char *>(message.content));
    }
    llama_free(_ctx);
    llama_model_free(_model);
    delete _batch;
    llama_sampler_free(_sampler);
}

std::string
LLMInference::benchModel(int pp, int tg, int pl, int nr) {
    g_batch     = llama_batch_init(pp, 0, pl);
    auto pp_avg = 0.0;
    auto tg_avg = 0.0;
    auto pp_std = 0.0;
    auto tg_std = 0.0;

    const uint32_t n_ctx = llama_n_ctx(this->_ctx);
    LOGi("n_ctx = %d", n_ctx);

    int i, j;
    int nri;
    for (nri = 0; nri < nr; nri++) {
        LOGi("Benchmark prompt processing (pp = %d)", pp);

        common_batch_clear(g_batch);

        const int n_tokens = pp;
        for (i = 0; i < n_tokens; i++) {
            common_batch_add(g_batch, 1, i, { 0 }, false);
        }

        g_batch.logits[g_batch.n_tokens - 1] = true;
        llama_memory_clear(llama_get_memory(this->_ctx), false);

        const auto t_pp_start = ggml_time_us();
        if (llama_decode(this->_ctx, g_batch) != 0) {
            LOGe("llama_decode() failed during prompt processing");
        }
        const auto t_pp_end = ggml_time_us();

        // bench text generation

        LOGi("Benchmark text generation (tg = %d)", tg);

        llama_memory_clear(llama_get_memory(this->_ctx), false);
        const auto t_tg_start = ggml_time_us();
        for (i = 0; i < tg; i++) {
            common_batch_clear(g_batch);
            for (j = 0; j < pl; j++) {
                common_batch_add(g_batch, 0, i, { j }, true);
            }

            if (llama_decode(this->_ctx, g_batch) != 0) {
                LOGe("llama_decode() failed during text generation");
            }
        }
        const auto t_tg_end = ggml_time_us();

        llama_memory_clear(llama_get_memory(this->_ctx), false);

        const auto t_pp = double(t_pp_end - t_pp_start) / 1000000.0;
        const auto t_tg = double(t_tg_end - t_tg_start) / 1000000.0;

        const auto speed_pp = double(pp) / t_pp;
        const auto speed_tg = double(pl * tg) / t_tg;

        pp_avg += speed_pp;
        tg_avg += speed_tg;

        pp_std += speed_pp * speed_pp;
        tg_std += speed_tg * speed_tg;

        LOGi("pp %f t/s, tg %f t/s", speed_pp, speed_tg);
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

    char model_desc[128];
    llama_model_desc(this->_model, model_desc, sizeof(model_desc));

    const auto model_size     = double(llama_model_size(this->_model)) / 1024.0 / 1024.0 / 1024.0;
    const auto model_n_params = double(llama_model_n_params(this->_model)) / 1e9;

    std::vector<std::string> backends;
    for (size_t i = 0; i < ggml_backend_reg_count(); i++) {
        auto*       reg  = ggml_backend_reg_get(i);
        std::string name = ggml_backend_reg_name(reg);
        if (name != "CPU") {
            backends.push_back(ggml_backend_reg_name(reg));
        }
    }
    std::ostringstream str;
    for (size_t i = 0; i < backends.size(); i++) {
        str << backends[i];
        if (i < backends.size() - 1) {
            str << ",";
        }
    }
    const auto backend = str.str();

    std::stringstream result;
    result << std::setprecision(3);
    result << "| model | size | params | backend | test | t/s |\n";
    result << "| --- | --- | --- | --- | --- | --- |\n";
    result << "| " << model_desc << " | " << model_size << "GiB | " << model_n_params << "B | " << backend << " | pp "
           << pp << " | " << pp_avg << " ± " << pp_std << " |\n";
    result << "| " << model_desc << " | " << model_size << "GiB | " << model_n_params << "B | " << backend << " | tg "
           << tg << " | " << tg_avg << " ± " << tg_std << " |\n";
    return result.str();
}
