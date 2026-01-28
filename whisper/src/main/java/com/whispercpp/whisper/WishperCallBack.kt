package com.whispercpp.whisper

import androidx.annotation.Keep

interface WhisperCallback {
    fun onNewSegment(startMs: Long, endMs: Long, text: String)
    fun onProgress(progress: Int)
    fun onComplete()
}

@Keep
class WishperCallBack : WhisperCallback {
    override fun onNewSegment(startMs: Long, endMs: Long, text: String) {
        println(text)
    }

    override fun onProgress(progress: Int) {
        println(progress)
    }

    override fun onComplete() {
        println("Completed")
    }
}