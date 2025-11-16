//
// Created by Shubham Panchal on 16/11/25.
//

#include <algorithm>
#include <array>
#include <cmath>
#include <queue>
#include <string>
#include <vector>

#define EMBEDDING_DIM 328

class VectorDBRecord {
  public:
    std::string                      text;
    std::array<float, EMBEDDING_DIM> embedding;
    float                            mag;

    static float
    computeMagnitude(const std::array<float, EMBEDDING_DIM>& vector) {
        float vectorMag = 0.0f;
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            vectorMag += vector[i] * vector[i];
        }
        return sqrt(vectorMag);
    }

    VectorDBRecord(std::string text, std::array<float, EMBEDDING_DIM> embedding) : text(text), embedding(embedding) {
        mag = computeMagnitude(embedding);
    }
};

class VectorDB {
    std::vector<VectorDBRecord> _records;

  public:
    void
    insertRecord(VectorDBRecord&& record) {
        _records.push_back(std::move(record));
    }

    std::vector<VectorDBRecord>
    nearestNeighbor(const std::array<float, EMBEDDING_DIM>& query, int k) {
        float queryMag = VectorDBRecord::computeMagnitude(query);

        auto comparator = [](const std::pair<float, const VectorDBRecord*>& a,
                             const std::pair<float, const VectorDBRecord*>& b) { return a.first > b.first; };

        std::priority_queue<std::pair<float, const VectorDBRecord*>,
                            std::vector<std::pair<float, const VectorDBRecord*>>, decltype(comparator)>
            top_k(comparator);
        for (const auto& record : _records) {
            float dot_product = 0.0f;
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                dot_product += query[i] * record.embedding[i];
            }
            float similarity = dot_product / (queryMag * record.mag);
            if (top_k.size() < (size_t)k) {
                top_k.push({ similarity, &record });
            } else if (similarity > top_k.top().first) {
                top_k.pop();
                top_k.push({ similarity, &record });
            }
        }
        std::vector<VectorDBRecord> result;
        while (!top_k.empty()) {
            result.push_back(*top_k.top().second);
            top_k.pop();
        }
        std::reverse(result.begin(), result.end());
        return result;
    }

    void
    clear() {
        _records.clear();
    }
};
