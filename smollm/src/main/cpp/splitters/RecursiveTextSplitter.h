#ifndef SMOLCHAT_ANDROID_RECURSIVETEXTSPLITTER_H
#define SMOLCHAT_ANDROID_RECURSIVETEXTSPLITTER_H

#include <string>
#include <vector>

class RecursiveTextSplitter {
    std::vector<std::string> _delimiters;

    std::vector<std::string> splitWithDelimiter(const std::string& text, const std::string& delimiter);

  public:
    RecursiveTextSplitter(std::vector<std::string> delimiters) : _delimiters(delimiters) {}

    std::vector<std::string> split(const std::string& text, size_t chunkSize);
};

#endif // SMOLCHAT_ANDROID_RECURSIVETEXTSPLITTER_H
