package com.bear.remoteviewhost

import android.content.Context


object RemoteHost {

    private var mContext: Context? = null
    private var ipcService: IpcService? = null

    internal fun onServiceCreated(context: Context, ipcService: IpcService) {
        this.mContext = context
        this.ipcService = ipcService
    }

    internal fun onServiceDestroyed() {
        this.mContext = null
        this.ipcService = null
    }

}