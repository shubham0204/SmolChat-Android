package io.shubham0204.smollmandroid.llm

import ai.moonshine.voice.JNI
import ai.moonshine.voice.TranscriptEvent
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.File

@Single
class AudioTranscriptionService(private val context: Context) {
    private val micTranscriber: MyTranscriber = MyTranscriber()
    private val modelDirName = "moonshine-asr"
    private val modelDir = File(context.filesDir, modelDirName)
    private val logTag = "[AudioTranscriptionService]"

    init {
        micTranscriber.loadFromFiles(
            modelDir.absolutePath,
            JNI.MOONSHINE_MODEL_ARCH_BASE,
        )
    }

    fun startTranscription(onLineComplete: (String) -> Unit): Error? {
        if (!checkIfModelsDownloaded()) {
            Log.e(logTag, "Models for audio transcription are not downloaded.")
            return Error.ModelNotDownloaded("Models for audio transcription are not downloaded.")
        }
        if (!checkIfAudioRecordingPermissionGranted()) {
            Log.e(logTag, "Permission to record audio was not granted.")
            return Error.AudioRecordingPermissionNotGranted(
                "Permission to record audio was not granted."
            )
        }
        micTranscriber.addListener { event ->
            if (event is TranscriptEvent.LineCompleted) {
                Log.d(logTag, "ASR Line completed: ${event.line.text}")
                CoroutineScope(Dispatchers.Main).launch {
                    onLineComplete(event.line.text)
                }
            }
        }
        Log.d(logTag, "Starting transcription...")
        micTranscriber.onMicPermissionGranted()
        micTranscriber.start()
        return null
    }

    fun stopTranscription() {
        Log.d(logTag, "Stopping transcription...")
        micTranscriber.stop()
    }

    private fun checkIfModelsDownloaded(): Boolean {
        return modelDir.exists()
    }

    private fun checkIfAudioRecordingPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    sealed interface Error {
        data class AudioRecordingPermissionNotGranted(val message: String) : Error

        data class ModelNotDownloaded(val message: String) : Error
    }
}
