#include "RecursiveTextSplitter.h"

std::vector<std::string>
RecursiveTextSplitter::splitWithDelimiter(const std::string& text, const std::string& delimiter) {
    size_t                   posStart        = 0;
    size_t                   posEnd          = 0;
    size_t                   delimiterLength = delimiter.size();
    std::string              token;
    std::vector<std::string> tokens;
    while ((posEnd = text.find(delimiter, posStart)) != std::string::npos) {
        token = text.substr(posStart, posEnd - posStart);
        tokens.push_back(token);
        posStart = posEnd + delimiterLength;
    }
    tokens.push_back(text.substr(posStart));
    return tokens;
}

std::vector<std::string>
RecursiveTextSplitter::split(const std::string& text, size_t chunkSize) {
    std::vector<std::string> parts = { text };
    for (const std::string& delimiter : _delimiters) {
        std::vector<std::string> splitParts;
        for (size_t i = 0; i < parts.size(); i++) {
            if (parts[i].size() > chunkSize) {
                std::vector<std::string> tokens = splitWithDelimiter(parts[i], delimiter);
                splitParts.insert(splitParts.end(), tokens.begin(), tokens.end());
            } else {
                splitParts.push_back(parts[i]);
            }
        }
        std::vector<std::string> mergedSplitParts;
        if (splitParts.size() > 1) {
            std::string prevPart = splitParts[0];
            for (size_t i = 1; i < splitParts.size(); i++) {
                std::string mergedPart = prevPart + delimiter + splitParts[i];
                if (mergedPart.size() < chunkSize) {
                    prevPart = mergedPart;
                } else {
                    mergedSplitParts.push_back(mergedPart);
                    prevPart = splitParts[i];
                }
            }
        }
        parts = mergedSplitParts;
    }
    return parts;
}