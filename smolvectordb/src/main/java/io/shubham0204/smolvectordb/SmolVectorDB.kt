package io.shubham0204.smolvectordb

class SmolVectorDB {
    companion object {
        init {
            System.loadLibrary("smolvectordb")
        }
    }

    private val handle: Long

    init {
        handle = initialize()
    }

    fun insertRecord(text: String, embedding: FloatArray) {
        insertRecord(handle, text, embedding)
    }

    fun nearestNeighbor(query: FloatArray, k: Int): List<String> {
        return nearestNeighbor(handle, query, k)
    }

    fun close() {
        close(handle)
    }

    private external fun initialize(): Long

    private external fun insertRecord(handle: Long, text: String, embedding: FloatArray)

    private external fun nearestNeighbor(handle: Long, query: FloatArray, k: Int): List<String>

    private external fun close(handle: Long)
}
