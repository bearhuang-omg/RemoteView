package com.bear.remoteviewhost

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class Service : Service() {

    private val TAG = "RemoteViewService"
    private val ipcService by lazy {
        IpcService(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG,"service created")
        RemoteHost.onServiceCreated(this,ipcService)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG,"service onbind")
        return ipcService.getServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG,"service unbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.i(TAG,"service destroy")
        super.onDestroy()
        RemoteHost.onServiceDestroyed()
    }
}