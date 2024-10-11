package com.bear.remoteview

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        var nextCallId = 0
        if (callId.get() < Int.MAX_VALUE) {
            nextCallId = callId.get() + 1
        }
        callId.set(nextCallId)
        return nextCallId
    }

    fun getBundleStr(bundle: Bundle?): String {
        if (bundle == null) {
            return ""
        } else {
            val strBuilder = StringBuilder()
            strBuilder.append("[")
            val keys = bundle.keySet()
            var index = 0
            keys.forEach { key ->
                if (index > 0) {
                    strBuilder.append(",")
                }
                val value = bundle.get(key)
                if (value is Bundle) {
                    val str = getBundleStr(value)
                    strBuilder.append(key)
                    strBuilder.append("=")
                    strBuilder.append(str)
                } else {
                    strBuilder.append(key)
                    strBuilder.append("=")
                    strBuilder.append(value)
                }
                index++
            }
            strBuilder.append("]")
            return strBuilder.toString()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    fun UI(runnable: Runnable) {
        mainHandler.post(runnable)
    }
}