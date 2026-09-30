#include "LLMInference.h"
#include <stdexcept>

LLMInference::LLMInference() : m_runner(std::make_unique<smollm::LLMRunner>()) {}

LLMInference::~LLMInference() = default;

void LLMInference::loadModel(const char* modelPath, float minP, float temperature, bool storeChats,
                            long contextSize, const char* chatTemplate, int nThreads,
                            bool useMmap, bool useMlock) {
    smollm::RunnerParams params;
    params.minP         = minP;
    params.temperature  = temperature;
    params.storeChats   = storeChats;
    params.contextSize  = contextSize;
    params.chatTemplate = chatTemplate ? chatTemplate : "";
    params.nThreads     = nThreads;
    params.useMmap      = useMmap;
    params.useMlock     = useMlock;

    if (!m_runner->load_model(modelPath, params)) {
        throw std::runtime_error("Runner::load_model() failed to load model from " + std::string(modelPath));
    }
}

void LLMInference::addChatMessage(const char* message, const char* role) {
    m_runner->add_chat_message(role, message);
}

float LLMInference::getResponseGenerationTime() const {
    return m_runner->get_tokens_per_second();
}

int LLMInference::getContextSizeUsed() const {
    return m_runner->get_context_size_used();
}

bool LLMInference::startCompletion(const char* query) {
    return m_runner->start_completion(query);
}

std::string LLMInference::completionLoop() {
    return m_runner->completion_loop();
}

void LLMInference::stopCompletion() {
    m_runner->stop_completion();
}

std::string LLMInference::benchModel(int pp, int tg, int pl, int nr) {
    return m_runner->bench_model(pp, tg, pl, nr);
}
