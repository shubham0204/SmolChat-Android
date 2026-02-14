package io.shubham0204.smollmandroid.llm.speech2text

import ai.moonshine.voice.JNI
import ai.moonshine.voice.TranscriptEvent
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ASRModel
import io.shubham0204.smollmandroid.ui.screens.manage_asr.ASRModelArch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.File

@Single
class AudioTranscriptionService(private val context: Context) {

    private val micTranscriber: MyTranscriber = MyTranscriber()
    private val logTag = "[AudioTranscriptionService]"
    private var model: ASRModel? = null

    fun startTranscription(asrModel: ASRModel, onLineComplete: (String) -> Unit): Error? {
        if (model?.name != asrModel.name) {
            Log.d(logTag, "Loading model: $asrModel")
            model = asrModel
            val modelDir = File(context.filesDir, asrModel.name)
            micTranscriber.loadFromFiles(
                modelDir.absolutePath,
                when (asrModel.arch) {
                    ASRModelArch.TINY -> JNI.MOONSHINE_MODEL_ARCH_TINY
                    ASRModelArch.BASE -> JNI.MOONSHINE_MODEL_ARCH_BASE
                },
            )
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
        try {
            micTranscriber.stop()
        } catch (e: UnsatisfiedLinkError) {
            Log.d(logTag, "Attempted to stop transcription without starting it.")
        }
    }

    private fun checkIfAudioRecordingPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    sealed interface Error {
        data class AudioRecordingPermissionNotGranted(val message: String) : Error
    }
}