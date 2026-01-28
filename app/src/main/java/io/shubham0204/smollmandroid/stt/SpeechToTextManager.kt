package io.shubham0204.smollmandroid.stt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Process
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.squti.androidwaverecorder.WaveRecorder
import com.whispercpp.whisper.WhisperCallback
import com.whispercpp.whisper.WhisperContext
import io.shubham0204.smollmandroid.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val LOG_TAG = "SpeechToTextManager"

sealed class STTState {
    data object Idle : STTState()
    data object Recording : STTState()
    data object Transcribing : STTState()
    data class Error(val message: String) : STTState()
}

@Single
class SpeechToTextManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var waveRecorder: WaveRecorder? = null
    private var whisperContext: WhisperContext? = null
    private var currentRecordingPath: String? = null

    private val _state = MutableStateFlow<STTState>(STTState.Idle)
    val state: StateFlow<STTState> = _state

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText

    // Flow for streaming transcription chunks - emits the FULL transcription each time
    // extraBufferCapacity=1 allows tryEmit to work without suspending
    private val _streamingTranscription = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val streamingTranscription: SharedFlow<String> = _streamingTranscription

    // Flow to signal that silence was detected and auto-submit should happen
    // extraBufferCapacity=1 allows tryEmit to work without suspending
    private val _silenceDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val silenceDetected: SharedFlow<Unit> = _silenceDetected

    private val modelsPath: File? = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    private var isModelLoaded = false
    private var loadedModelName: String? = null

    // For streaming transcription
    private var streamingJob: Job? = null
    private var silenceDetectionJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var isStreamingMode = false
    private var audioBuffer = mutableListOf<Short>()
    private val audioBufferLock = Any()

    // Interval for periodic transcription (in milliseconds)
    private val transcriptionIntervalMs = 1500L

    // Audio recording parameters
    private val sampleRate = 16000

    // Regex to filter out Whisper noise/silence markers
    // These markers should not be considered as "speech" for auto-submit purposes
    private val noiseMarkerRegex = Regex(
        """\[.*?]|\(.*?\)|<\|.*?\|>""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Cleans transcription by removing Whisper noise markers like [empty audio], [BLANK_AUDIO],
     * [noise], [music], (silence), etc. Returns only the actual spoken words.
     */
    private fun cleanTranscription(text: String): String {
        return noiseMarkerRegex.replace(text, "").trim()
    }

    /**
     * Checks if the transcription contains only noise markers (no real speech).
     */
    private fun isOnlyNoiseMarkers(text: String): Boolean {
        return cleanTranscription(text).isBlank()
    }

    fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isModelAvailable(modelFileName: String? = null): Boolean {
        val selectedModel = modelFileName ?: preferencesManager.selectedWhisperModel
        val modelFile = File(modelsPath, selectedModel)
        return modelFile.exists()
    }

    /**
     * Returns a list of available Whisper model files in the models directory.
     * Whisper models typically have .bin extension and contain "ggml" in the name.
     */
    fun getAvailableModels(): List<String> {
        return modelsPath?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".bin") && it.name.contains("ggml") }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun getSelectedModelName(): String = preferencesManager.selectedWhisperModel

    fun setSelectedModel(modelFileName: String) {
        // If a different model is being selected, we need to reload
        if (loadedModelName != modelFileName && isModelLoaded) {
            scope.launch {
                whisperContext?.release()
                whisperContext = null
                isModelLoaded = false
                loadedModelName = null
            }
        }
        preferencesManager.selectedWhisperModel = modelFileName
    }

    fun loadModel(modelFileName: String? = null, onComplete: (Boolean) -> Unit = {}) {
        val selectedModel = modelFileName ?: preferencesManager.selectedWhisperModel
        scope.launch {
            try {
                // If model is already loaded and it's the same model, just return success
                if (isModelLoaded && loadedModelName == selectedModel) {
                    withContext(Dispatchers.Main) {
                        onComplete(true)
                    }
                    return@launch
                }

                // If a different model is loaded, release it first
                if (isModelLoaded && loadedModelName != selectedModel) {
                    whisperContext?.release()
                    whisperContext = null
                    isModelLoaded = false
                    loadedModelName = null
                }

                val modelFile = File(modelsPath, selectedModel)
                if (!modelFile.exists()) {
                    Log.e(LOG_TAG, "Model file not found: ${modelFile.absolutePath}")
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                    return@launch
                }

                Log.d(LOG_TAG, "Loading Whisper model from: ${modelFile.absolutePath}")
                whisperContext = WhisperContext.createContextFromFile(modelFile.absolutePath)
                isModelLoaded = true
                loadedModelName = selectedModel
                Log.d(LOG_TAG, "Whisper model loaded successfully")

                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to load Whisper model", e)
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    // Callback for when silence is detected and auto-submit should happen
    // This is called directly from the transcription coroutine scope to avoid
    // issues with frozen ViewModel coroutines on Samsung devices
    // @Volatile ensures visibility across threads (set from Main, read from IO)
    @Volatile
    private var onSilenceDetectedCallback: ((String) -> Unit)? = null

    /**
     * Set a callback that will be called when silence is detected.
     * The callback receives the final transcription text.
     * This is called directly from the IO dispatcher, so the callback
     * should handle any necessary thread dispatching.
     */
    fun setOnSilenceDetectedCallback(callback: ((String) -> Unit)?) {
        Log.d(LOG_TAG, ">>> setOnSilenceDetectedCallback called, callback is ${if (callback != null) "NOT NULL" else "NULL"}")
        onSilenceDetectedCallback = callback
    }

    /**
     * Start streaming recording with periodic transcription.
     * Transcribed text will be emitted via streamingTranscription Flow.
     * When transcription stops changing for the configured duration, silenceDetected will emit.
     */
    @Suppress("MissingPermission")
    fun startStreamingRecording(language: String = "en", autoSubmitDelayMs: Long = 2000L) {
        if (!hasRecordingPermission()) {
            _state.value = STTState.Error("Recording permission not granted")
            return
        }

        try {
            isStreamingMode = true
            synchronized(audioBufferLock) {
                audioBuffer.clear()
            }

            // Initialize AudioRecord for direct audio capture
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            audioRecord?.startRecording()
            _state.value = STTState.Recording
            Log.d(LOG_TAG, "Streaming recording started with AudioRecord")

            // Start audio capture job
            streamingJob = scope.launch {
                val readBuffer = ShortArray(bufferSize / 2)

                while (isActive && _state.value == STTState.Recording) {
                    val readCount = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0

                    if (readCount > 0) {
                        // Add samples to buffer
                        synchronized(audioBufferLock) {
                            for (i in 0 until readCount) {
                                audioBuffer.add(readBuffer[i])
                            }
                        }
                    }

                    delay(10) // Small delay to prevent tight loop
                }
            }

            // Start periodic transcription job with auto-submit detection
            silenceDetectionJob = scope.launch {
                var lastCleanTranscription = ""  // Only real spoken words (no noise markers)
                var lastRawTranscription = ""    // Full transcription including markers
                var lastSpeechChangeTime = System.currentTimeMillis()
                var autoSubmitTriggered = false

                Log.d(LOG_TAG, "Starting periodic transcription loop")
                delay(transcriptionIntervalMs) // Initial delay before first transcription
                while (isActive && _state.value == STTState.Recording) {
                    Log.d(LOG_TAG, "Periodic transcription tick, buffer size: ${audioBuffer.size}")
                    val rawTranscription = transcribeCurrentBuffer(language)
                    val cleanedTranscription = cleanTranscription(rawTranscription)

                    Log.d(LOG_TAG, "Raw: '$rawTranscription' | Clean: '$cleanedTranscription'")

                    if (rawTranscription.isNotBlank()) {
                        // Check if the CLEANED transcription (real speech) has changed
                        if (cleanedTranscription != lastCleanTranscription && cleanedTranscription.isNotBlank()) {
                            // Real speech changed - reset timer and emit
                            lastCleanTranscription = cleanedTranscription
                            lastRawTranscription = rawTranscription
                            lastSpeechChangeTime = System.currentTimeMillis()
                            autoSubmitTriggered = false
                            Log.d(LOG_TAG, "Speech changed, emitting: $cleanedTranscription")
                            // Emit the cleaned transcription (without noise markers)
                            _streamingTranscription.tryEmit(cleanedTranscription)
                        } else if (lastCleanTranscription.isNotBlank()) {
                            // Real speech hasn't changed - check if we should auto-submit
                            // Only auto-submit if we have actual spoken words
                            val timeSinceLastSpeech = System.currentTimeMillis() - lastSpeechChangeTime
                            Log.d(LOG_TAG, "Speech unchanged for ${timeSinceLastSpeech}ms (threshold: ${autoSubmitDelayMs}ms)")
                            if (timeSinceLastSpeech >= autoSubmitDelayMs && !autoSubmitTriggered) {
                                Log.d(LOG_TAG, ">>> SILENCE DETECTED after speech - triggering auto-submit")
                                autoSubmitTriggered = true

                                // Call the callback directly from this coroutine scope
                                // This bypasses the ViewModel's coroutine which may be frozen
                                val callback = onSilenceDetectedCallback
                                Log.d(LOG_TAG, ">>> onSilenceDetectedCallback is ${if (callback != null) "NOT NULL" else "NULL"}")
                                if (callback != null) {
                                    Log.d(LOG_TAG, ">>> Calling onSilenceDetectedCallback directly")
                                    // Stop recording and get final transcription (cleaned)
                                    val finalTranscription = lastCleanTranscription
                                    stopStreamingRecordingInternal()
                                    Log.d(LOG_TAG, ">>> Recording stopped, calling callback with: $finalTranscription")
                                    callback(finalTranscription)
                                    Log.d(LOG_TAG, ">>> Callback returned")
                                    return@launch // Exit the loop since we stopped recording
                                } else {
                                    // Fallback to flow emission for backward compatibility
                                    val emitted = _silenceDetected.tryEmit(Unit)
                                    Log.d(LOG_TAG, ">>> silenceDetected.tryEmit result: $emitted, subscribers: ${_silenceDetected.subscriptionCount.value}")
                                }
                            }
                        } else {
                            Log.d(LOG_TAG, "Only noise markers detected, waiting for speech...")
                        }
                    } else {
                        Log.d(LOG_TAG, "Transcription was blank")
                    }

                    delay(transcriptionIntervalMs)
                }
                Log.d(LOG_TAG, "Periodic transcription loop ended, isActive=$isActive, state=${_state.value}")
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start streaming recording", e)
            _state.value = STTState.Error("Failed to start recording: ${e.message}")
        }
    }

    /**
     * Transcribe the current audio buffer without stopping recording.
     * Returns the full transcription text.
     */
    private suspend fun transcribeCurrentBuffer(language: String): String {
        val audioData: FloatArray
        synchronized(audioBufferLock) {
            if (audioBuffer.size < sampleRate) { // At least 1 second of audio
                return ""
            }
            // Convert Short buffer to Float array
            audioData = FloatArray(audioBuffer.size)
            for (i in audioBuffer.indices) {
                audioData[i] = audioBuffer[i] / 32768.0f
            }
        }

        Log.d(LOG_TAG, "Periodic transcription of ${audioData.size} samples")

        // Boost thread priority to reduce CPU throttling when screen is locked
        val originalPriority = Process.getThreadPriority(Process.myTid())
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Failed to boost thread priority: ${e.message}")
        }

        return try {
            val result = whisperContext?.transcribeData(
                data = audioData,
                language = language,
                printTimestamp = false,
                callback = object : WhisperCallback {
                    override fun onNewSegment(startMs: Long, endMs: Long, text: String) {
                        Log.d(LOG_TAG, "Streaming segment: $text")
                    }

                    override fun onProgress(progress: Int) {
                        // Ignore progress for streaming
                    }

                    override fun onComplete() {
                        Log.d(LOG_TAG, "Streaming transcription chunk complete")
                    }
                }
            ) ?: ""

            result.trim()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error during periodic transcription", e)
            ""
        } finally {
            // Restore original thread priority
            try {
                Process.setThreadPriority(originalPriority)
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Failed to restore thread priority: ${e.message}")
            }
        }
    }

    /**
     * Internal method to stop recording without final transcription.
     * Used by the silence detection callback.
     */
    private fun stopStreamingRecordingInternal() {
        streamingJob?.cancel()
        streamingJob = null
        silenceDetectionJob?.cancel()
        silenceDetectionJob = null
        isStreamingMode = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            waveRecorder?.stopRecording()
            waveRecorder = null
            synchronized(audioBufferLock) {
                audioBuffer.clear()
            }
            _state.value = STTState.Idle
            Log.d(LOG_TAG, ">>> Recording stopped internally")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop recording internally", e)
        }
    }

    /**
     * Stop streaming recording and perform final transcription.
     */
    fun stopStreamingRecording(
        language: String = "en",
        onComplete: (String) -> Unit
    ) {
        streamingJob?.cancel()
        streamingJob = null
        silenceDetectionJob?.cancel()
        silenceDetectionJob = null
        isStreamingMode = false

        try {
            // Stop AudioRecord
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            // Also stop WaveRecorder if it was used (for non-streaming mode fallback)
            waveRecorder?.stopRecording()
            waveRecorder = null

            _state.value = STTState.Transcribing
            Log.d(LOG_TAG, "Streaming recording stopped, final transcription")

            scope.launch {
                // Get audio data from buffer
                val audioData: FloatArray
                val bufferEmpty: Boolean
                synchronized(audioBufferLock) {
                    bufferEmpty = audioBuffer.isEmpty()
                    if (bufferEmpty) {
                        audioData = FloatArray(0)
                    } else {
                        audioData = FloatArray(audioBuffer.size)
                        for (i in audioBuffer.indices) {
                            audioData[i] = audioBuffer[i] / 32768.0f
                        }
                        audioBuffer.clear()
                    }
                }

                if (bufferEmpty) {
                    Log.d(LOG_TAG, "Buffer empty, completing with empty string")
                    _state.value = STTState.Idle
                    onComplete("")
                    return@launch
                }

                if (audioData.size < sampleRate) { // Less than 1 second
                    Log.d(LOG_TAG, "Audio too short (${audioData.size} samples), completing with empty string")
                    _state.value = STTState.Idle
                    onComplete("")
                    return@launch
                }

                Log.d(LOG_TAG, "Starting final transcription of ${audioData.size} samples")
                val result = whisperContext?.transcribeData(
                    data = audioData,
                    language = language,
                    printTimestamp = false,
                    callback = object : WhisperCallback {
                        override fun onNewSegment(startMs: Long, endMs: Long, text: String) {
                            Log.d(LOG_TAG, "Final segment: $text")
                        }

                        override fun onProgress(progress: Int) {}
                        override fun onComplete() {}
                    }
                ) ?: ""

                val finalText = result.trim()
                Log.d(LOG_TAG, "Final transcription complete: $finalText")

                // StateFlow is thread-safe, no need for Main dispatcher
                _state.value = STTState.Idle
                // Call callback directly - caller handles any needed dispatching
                onComplete(finalText)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop streaming recording", e)
            _state.value = STTState.Error("Failed to stop recording: ${e.message}")
            onComplete("")
        }
    }

    fun startRecording() {
        if (!hasRecordingPermission()) {
            _state.value = STTState.Error("Recording permission not granted")
            return
        }

        try {
            val recordingFile = generateRecordingFile()
            currentRecordingPath = recordingFile.absolutePath

            waveRecorder = WaveRecorder(recordingFile.absolutePath)
                .configureWaveSettings {
                    sampleRate = 16000  // Whisper expects 16kHz
                    channels = AudioFormat.CHANNEL_IN_MONO
                    audioEncoding = AudioFormat.ENCODING_PCM_16BIT
                }

            waveRecorder?.startRecording()
            _state.value = STTState.Recording
            Log.d(LOG_TAG, "Recording started: ${recordingFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start recording", e)
            _state.value = STTState.Error("Failed to start recording: ${e.message}")
        }
    }

    fun stopRecordingAndTranscribe(
        language: String = "en",
        onTranscriptionComplete: (String) -> Unit
    ) {
        // If in streaming mode, use the streaming stop method
        if (isStreamingMode) {
            stopStreamingRecording(language, onTranscriptionComplete)
            return
        }

        try {
            waveRecorder?.stopRecording()
            waveRecorder = null

            val recordingPath = currentRecordingPath
            if (recordingPath == null) {
                _state.value = STTState.Error("No recording file found")
                onTranscriptionComplete("")
                return
            }

            _state.value = STTState.Transcribing
            Log.d(LOG_TAG, "Recording stopped, starting transcription")

            scope.launch {
                transcribeAudio(recordingPath, language, onTranscriptionComplete)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop recording", e)
            _state.value = STTState.Error("Failed to stop recording: ${e.message}")
            onTranscriptionComplete("")
        }
    }

    fun cancelRecording() {
        streamingJob?.cancel()
        streamingJob = null
        silenceDetectionJob?.cancel()
        silenceDetectionJob = null
        isStreamingMode = false

        try {
            // Stop AudioRecord if used
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            // Stop WaveRecorder if used
            waveRecorder?.stopRecording()
            waveRecorder = null

            // Clear the audio buffer
            synchronized(audioBufferLock) {
                audioBuffer.clear()
            }

            // Delete the recording file if any
            currentRecordingPath?.let { path ->
                File(path).delete()
            }
            currentRecordingPath = null

            _state.value = STTState.Idle
            Log.d(LOG_TAG, "Recording cancelled")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to cancel recording", e)
        }
    }

    private suspend fun transcribeAudio(
        audioPath: String,
        language: String,
        onComplete: (String) -> Unit
    ) {
        try {
            if (whisperContext == null) {
                Log.e(LOG_TAG, "Whisper context not initialized")
                withContext(Dispatchers.Main) {
                    _state.value = STTState.Error("Whisper model not loaded")
                    onComplete("")
                }
                return
            }

            val audioData = readWavFileAsFloatArray(audioPath)
            if (audioData.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _state.value = STTState.Error("Failed to read audio file")
                    onComplete("")
                }
                return
            }

            Log.d(LOG_TAG, "Transcribing ${audioData.size} samples")

            val transcriptionBuilder = StringBuilder()

            val result = whisperContext?.transcribeData(
                data = audioData,
                language = language,
                printTimestamp = false,
                callback = object : WhisperCallback {
                    override fun onNewSegment(startMs: Long, endMs: Long, text: String) {
                        transcriptionBuilder.append(text)
                        Log.d(LOG_TAG, "Segment: $text")
                    }

                    override fun onProgress(progress: Int) {
                        Log.d(LOG_TAG, "Transcription progress: $progress%")
                    }

                    override fun onComplete() {
                        Log.d(LOG_TAG, "Transcription complete")
                    }
                }
            ) ?: ""

            // Clean up the recording file
            File(audioPath).delete()
            currentRecordingPath = null

            val finalText = result.trim()
            Log.d(LOG_TAG, "Transcription result: $finalText")

            withContext(Dispatchers.Main) {
                _transcribedText.value = finalText
                _state.value = STTState.Idle
                onComplete(finalText)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Transcription failed", e)
            withContext(Dispatchers.Main) {
                _state.value = STTState.Error("Transcription failed: ${e.message}")
                onComplete("")
            }
        }
    }

    private fun readWavFileAsFloatArray(filePath: String): FloatArray {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(LOG_TAG, "WAV file does not exist: $filePath")
                return FloatArray(0)
            }

            FileInputStream(file).use { fis ->
                val headerBytes = ByteArray(44)
                val headerRead = fis.read(headerBytes)
                if (headerRead < 44) {
                    Log.e(LOG_TAG, "WAV header too short")
                    return FloatArray(0)
                }

                // Read the data size from the header (bytes 40-43)
                val dataSize = ByteBuffer.wrap(headerBytes, 40, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int

                // For streaming, the file might still be growing, so read what's available
                val availableBytes = file.length().toInt() - 44
                val bytesToRead = minOf(dataSize, availableBytes)

                if (bytesToRead <= 0) {
                    return FloatArray(0)
                }

                val audioBytes = ByteArray(bytesToRead)
                fis.read(audioBytes)

                // Convert 16-bit PCM to float array
                val samples = bytesToRead / 2
                val floatArray = FloatArray(samples)
                val byteBuffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN)

                for (i in 0 until samples) {
                    val sample = byteBuffer.short.toInt()
                    floatArray[i] = sample / 32768.0f
                }

                floatArray
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to read WAV file", e)
            FloatArray(0)
        }
    }

    private fun generateRecordingFile(): File {
        val fileName = "stt_recording_${System.currentTimeMillis()}.wav"
        return File(context.cacheDir, fileName)
    }

    fun release() {
        streamingJob?.cancel()
        streamingJob = null
        silenceDetectionJob?.cancel()
        silenceDetectionJob = null
        isStreamingMode = false

        scope.launch {
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                waveRecorder?.stopRecording()
                waveRecorder = null
                whisperContext?.release()
                whisperContext = null
                isModelLoaded = false
                synchronized(audioBufferLock) {
                    audioBuffer.clear()
                }
                currentRecordingPath?.let { File(it).delete() }
                currentRecordingPath = null
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to release resources", e)
            }
        }
    }
}
