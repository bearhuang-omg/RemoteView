package com.bear.remoteview

import android.app.ActivityManager
import android.content.Context
import android.os.Process


object Utils {

    fun getProcessName(context: Context): String {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        val runningAppProcesses = manager!!.runningAppProcesses
        if (runningAppProcesses != null) {
            val pid = Process.myPid()
            for (processInfo in runningAppProcesses) {
                if (processInfo.pid == pid) {
                    return processInfo.processName
                }
            }
        }
        return ""
    }

    private var currentId = 0

    fun generateRemoteViewId(): Int {
        synchronized(Utils::class) {
            return currentId++
        }
    }

    private var callId = ThreadLocal.withInitial { 0 }

    fun generateCallId(): Int {
        val nextCallId = callId.get() + 1
        callId.set(nextCallId)
        return nextCallId
    }
}