package io.shubham0204.smollmandroid.ui.screens.manage_asr

import android.content.Context
import com.ketch.Ketch
import com.ketch.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single

import com.ketch.NotificationConfig
import io.shubham0204.smollmandroid.R

@Single
class DownloadService(
    val context: Context
) {
    private var ketch: Ketch =
        Ketch
            .builder()
            .setNotificationConfig(
                NotificationConfig(
                    enabled = true,
                    smallIcon = R.drawable.ic_launcher_foreground
                )
            )
            .setOkHttpClient(
                OkHttpClient
                    .Builder()
                    .connectTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .build(),
            ).build(context)

    var defaultDownloadDir = context.filesDir.absolutePath

    fun startDownload(
        url: String,
        destDir: String,
        destFileName: String,
        onStart: () -> Unit,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        var retries = 0
        CoroutineScope(Dispatchers.IO).launch {
            val downloadId =
                ketch.download(
                    url,
                    destDir,
                    destFileName
                )
            ketch
                .observeDownloadById(downloadId)
                .flowOn(Dispatchers.IO)
                .collect { downloadModel ->
                    withContext(Dispatchers.Main) {
                        downloadModel?.let { ketchDownload ->
                            when (ketchDownload.status) {
                                Status.QUEUED -> onStart()
                                Status.STARTED -> onStart()
                                Status.PROGRESS -> onProgress(ketchDownload.progress)
                                Status.SUCCESS -> onSuccess()
                                Status.CANCELLED -> onFailure("Download Cancelled")
                                Status.FAILED -> {
                                    if (ketchDownload.progress == 100) {
                                        onSuccess()
                                    } else if (retries < 5) {
                                        retries++
                                        ketch.retry(downloadId)
                                    } else {
                                        onFailure(ketchDownload.failureReason ?: "Unknown error")
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
        }
    }
}