/*
 * Copyright (C) 2025 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.screens.manage_server.ManageServerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OpenAIServerService : Service() {

    private val serverManager: OpenAIServerManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVER -> {
                serverManager.stopServer()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                val host = intent.getStringExtra(EXTRA_HOST) ?: "0.0.0.0"
                val modelId = intent.getLongExtra(EXTRA_MODEL_ID, -1L)
                val contextSize = intent.getLongExtra(EXTRA_CONTEXT_SIZE, 2048L)
                val numThreads = intent.getIntExtra(EXTRA_NUM_THREADS, 4)

                val config = ServerConfig(
                    port = port,
                    host = host,
                    modelId = modelId,
                    contextSize = contextSize,
                    numThreads = numThreads,
                )

                val ip = OpenAIServerManager.detectLocalIpAddress()
                val displayHost = if (host == "0.0.0.0") ip else host
                val endpointUrl = "http://$displayHost:$port/v1"
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(
                        getString(
                            R.string.server_notification_starting,
                            endpointUrl
                        )
                    ),
                )
                acquireWakeLock()

                serviceScope.launch {
                    val result = serverManager.startServer(config)
                    if (result.isSuccess) {
                        val modelName =
                            serverManager.state.value.loadedModelName
                                ?: getString(R.string.server_status_none)
                        updateNotification(
                            getString(
                                R.string.server_notification_listening,
                                endpointUrl,
                                modelName
                            )
                        )
                    } else {
                        releaseWakeLock()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serverManager.stopServer()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmolChat::OpenAIServerWakeLock",
        ).apply {
            acquire(4 * 60 * 60 * 1000L) // up to 4 hours
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.server_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.server_notification_channel_desc)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, ManageServerActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, OpenAIServerService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.server_notification_channel_name))
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.server_action_stop),
                stopPendingIntent,
            )
            .build()
    }

    companion object {
        const val CHANNEL_ID = "smolchat_openai_server_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_SERVER = "io.shubham0204.smollmandroid.action.START_OPENAI_SERVER"
        const val ACTION_STOP_SERVER = "io.shubham0204.smollmandroid.action.STOP_OPENAI_SERVER"

        const val EXTRA_PORT = "extra_port"
        const val EXTRA_HOST = "extra_host"
        const val EXTRA_MODEL_ID = "extra_model_id"
        const val EXTRA_CONTEXT_SIZE = "extra_context_size"
        const val EXTRA_NUM_THREADS = "extra_num_threads"

        fun start(context: Context, config: ServerConfig) {
            val intent = Intent(context, OpenAIServerService::class.java).apply {
                action = ACTION_START_SERVER
                putExtra(EXTRA_PORT, config.port)
                putExtra(EXTRA_HOST, config.host)
                putExtra(EXTRA_MODEL_ID, config.modelId)
                putExtra(EXTRA_CONTEXT_SIZE, config.contextSize)
                putExtra(EXTRA_NUM_THREADS, config.numThreads)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OpenAIServerService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
    }
}
