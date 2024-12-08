#include "llama.h"
#include <string>
#include <vector>
#include <functional>
#include <jni.h>

class LLMInference {

    struct CachedMessage {
        std::string role;
        std::string message;
        std::vector<llama_token> tokens;
    };

    llama_context* ctx;
    llama_model* model;
    llama_sampler* sampler;
    llama_batch batch;
    std::string response;
    std::vector<llama_chat_message> messages;
    llama_token curr_token;
    std::string cache_response_tokens;

    std::vector<char> formatted;
    int prev_len = 0;
    bool store_chats;

    int64_t response_generation_time = 0;
    long response_num_tokens = 0;

    bool is_valid_utf8(const char* response);

    public:

    void load_model(const char* model_path, float min_p, float temperature, bool store_chats);

    void add_chat_message(const char* message, const char* role);

    float get_response_generation_time();

    void start_completion(const char* query);

    std::string completion_loop();

    void stop_completion();

    ~LLMInference();

};