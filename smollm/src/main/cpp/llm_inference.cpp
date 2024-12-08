#include "llm_inference.h"
#include "common.h"
#include <cstring>
#include <iostream>
#include <android/log.h>

#define TAG "llama-android.cpp"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)


void LLMInference::load_model(const char *model_path, float min_p, float temperature, bool store_chats) {
    // create an instance of llama_model
    llama_model_params model_params = llama_model_default_params();
    model = llama_load_model_from_file(model_path, model_params);

    if (!model) {
        LOGe("failed to load model from %s", model_path);
        throw std::runtime_error("load_model() failed");
    }

    // create an instance of llama_context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;            // take context size from the model GGUF file
    ctx_params.no_perf = true;          // disable performance metrics
    ctx = llama_new_context_with_model(model, ctx_params);

    if (!ctx) {
        LOGe("llama_new_context_with_model() returned null)");
        throw std::runtime_error("llama_new_context_with_model() returned null");
    }

    // initialize sampler
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;      // disable performance metrics
    sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_min_p(min_p, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    formatted = std::vector<char>(llama_n_ctx(ctx));
    messages.clear();
    this->store_chats = store_chats;
}

void LLMInference::add_chat_message(const char *message, const char *role) {
    messages.push_back({strdup(role), strdup(message)});
}

float LLMInference::get_response_generation_time() {
    return (float)response_num_tokens / (response_generation_time / 1e6);
}

void LLMInference::start_completion(const char *query) {
    if (!store_chats) {
        prev_len = 0;
        formatted.clear();
    }
    response_generation_time = 0;
    response_num_tokens = 0;
    add_chat_message(query, "user");
    int new_len = llama_chat_apply_template(
            model,
            nullptr,
            messages.data(),
            messages.size(),
            true,
            formatted.data(),
            formatted.size()
    );
    if (new_len > (int)formatted.size()) {
        formatted.resize(new_len);
        new_len = llama_chat_apply_template(model, nullptr, messages.data(), messages.size(), true, formatted.data(), formatted.size());
    }
    if (new_len < 0) {
        throw std::runtime_error("llama_chat_apply_template() in LLMInference::start_completion() failed");
    }
    std::string prompt(formatted.begin() + prev_len, formatted.begin() + new_len);
    std::vector<llama_token> prompt_tokens = common_tokenize(model, prompt, true, true);

    // create a llama_batch containing a single sequence
    // see llama_batch_init for more details
    batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
}

// taken from:
// https://github.com/ggerganov/llama.cpp/blob/master/examples/llama.android/llama/src/main/cpp/llama-android.cpp#L38
bool LLMInference::is_valid_utf8(const char *response) {
    if (!response) {
        return true;
    }
    const unsigned char * bytes = (const unsigned char *)response;
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


std::string LLMInference::completion_loop() {
    // check if the length of the inputs to the model
    // have exceeded the context size of the model
    int context_size = llama_n_ctx(ctx);
    int n_ctx_used = llama_get_kv_cache_used_cells(ctx);
    if (n_ctx_used + batch.n_tokens > context_size) {
        std::cerr << "context size exceeded" << '\n';
        exit(0);
    }

    auto start = ggml_time_us();
    // run the model
    if (llama_decode(ctx, batch) < 0) {
        throw std::runtime_error("llama_decode() failed");
    }

    // sample a token and check if it is an EOG (end of generation token)
    // convert the integer token to its correspond word-piece
    curr_token = llama_sampler_sample(sampler, ctx, -1);
    if (llama_token_is_eog(model, curr_token)) {
        return "[EOG]";
    }
    std::string piece = common_token_to_piece(ctx, curr_token, true);
    auto end = ggml_time_us();
    response_generation_time += (end - start);
    response_num_tokens += 1;
    cache_response_tokens += piece;

    // re-init the batch with the newly predicted token
    // key, value pairs of all previous tokens have been cached
    // in the KV cache
    batch = llama_batch_get_one(&curr_token, 1);

    if (is_valid_utf8(cache_response_tokens.c_str())) {
        response += cache_response_tokens;
        std::string valid_utf8_piece = cache_response_tokens;
        cache_response_tokens.clear();
        return valid_utf8_piece;
    }

    return "";
}


void LLMInference::stop_completion() {
    if (store_chats) {
        add_chat_message(strdup(response.c_str()), "assistant");
    }
    response.clear();
    prev_len = llama_chat_apply_template(
            model,
            nullptr,
            messages.data(),
            messages.size(),
            false,
            nullptr,
            0
    );
    if (prev_len < 0) {
        throw std::runtime_error("llama_chat_apply_template() in LLMInference::stop_completion() failed");
    }
}


LLMInference::~LLMInference() {
    // free memory held by the message text in messages
    // (as we had used strdup() to create a malloc'ed copy)
    for (llama_chat_message &message: messages) {
        delete message.content;
    }
    llama_kv_cache_clear(ctx);
    llama_sampler_free(sampler);
    llama_free(ctx);
    llama_free_model(model);
}