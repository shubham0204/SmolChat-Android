package io.shubham0204.smollmandroid.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.shubham0204.smollm_server.LlamaServer

class LlamaServerService : Service() {
    private val llamaServer = LlamaServer()

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        llamaServer.start()
        return START_STICKY
    }

    override fun onDestroy() {
        llamaServer.stop()
        super.onDestroy()
    }
}
