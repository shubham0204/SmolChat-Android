#pragma once

#include "LLMRunner.h"
#include <memory>
#include <string>

class LLMInference {
private:
    std::unique_ptr<smollm::LLMRunner> m_runner;

public:
    LLMInference();
    ~LLMInference();

    void loadModel(const char* modelPath, float minP, float temperature, bool storeChats, long contextSize,
                   const char* chatTemplate, int nThreads, bool useMmap, bool useMlock);

    std::string benchModel(int pp, int tg, int pl, int nr);

    void addChatMessage(const char* message, const char* role);

    float getResponseGenerationTime() const;

    int getContextSizeUsed() const;

    // Returns true if Jinja template was used, false if legacy fallback was needed.
    bool startCompletion(const char* query);

    std::string completionLoop();

    void stopCompletion();

    smollm::LLMRunner* getRunner() { return m_runner.get(); }
};