package com.bear.remoteviewhost

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.bear.remoteview.RemoteCall

class Service : Service() {

    private val TAG = "RemoteViewService"
    private val ipcService by lazy {
        IpcService(this)
    }

    override fun onCreate() {
        super.onCreate()
        RemoteHost.onServiceCreated(this,ipcService)
    }

    override fun onBind(intent: Intent?): IBinder {
        return ipcService.getServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        RemoteHost.onServiceDestroyed()
    }
}