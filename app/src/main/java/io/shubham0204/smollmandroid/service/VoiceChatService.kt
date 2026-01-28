/*
 * Copyright (C) 2024 Shubham Panchal
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

package io.shubham0204.smollmandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import io.shubham0204.smollmandroid.R
import io.shubham0204.smollmandroid.ui.screens.chat.ChatActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val LOG_TAG = "VoiceChatService"

/**
 * Foreground service that keeps voice chat active when the screen is locked.
 * Shows a persistent notification with a "Stop" action.
 * Uses a partial wake lock to keep CPU active for transcription.
 */
class VoiceChatService : Service(), KoinComponent {

    private val voiceChatServiceManager: VoiceChatServiceManager by inject()
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "voice_chat_channel"
        const val ACTION_STOP = "io.shubham0204.smollmandroid.STOP_VOICE_CHAT"

        fun start(context: Context) {
            Log.d(LOG_TAG, ">>> Starting VoiceChatService")
            val intent = Intent(context, VoiceChatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Log.d(LOG_TAG, ">>> Stopping VoiceChatService")
            context.stopService(Intent(context, VoiceChatService::class.java))
        }

        /**
         * Check if the app is exempt from battery optimization.
         * On Samsung devices, this is required to prevent the app from being frozen.
         */
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }

        /**
         * Request the user to disable battery optimization for this app.
         * This is required on Samsung and other OEM devices to prevent aggressive app killing.
         */
        fun requestBatteryOptimizationExemption(context: Context) {
            if (!isIgnoringBatteryOptimizations(context)) {
                Log.d(LOG_TAG, ">>> Requesting battery optimization exemption")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand, action=${intent?.action}")

        if (intent?.action == ACTION_STOP) {
            Log.d(LOG_TAG, "Stop action received")
            voiceChatServiceManager.requestStopService()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        voiceChatServiceManager.setServiceRunning(true)

        // Acquire partial wake lock to keep CPU active for transcription
        acquireWakeLock()

        Log.d(LOG_TAG, "Service started in foreground")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy")
        releaseWakeLock()
        voiceChatServiceManager.setServiceRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmolChat:VoiceChatWakeLock"
            ).apply {
                acquire(60 * 60 * 1000L) // 1 hour max, released when service stops
            }
            Log.d(LOG_TAG, "Wake lock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(LOG_TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_chat_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_chat_notification_text)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, VoiceChatService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.voice_chat_notification_title))
            .setContentText(getString(R.string.voice_chat_notification_text))
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.voice_chat_stop),
                stopPendingIntent
            )
            .build()
    }
}
