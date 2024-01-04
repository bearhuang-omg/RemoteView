package com.example.surfacehost

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceControlViewHost
import android.view.SurfaceControlViewHost.SurfacePackage
import android.widget.ImageView
import android.widget.Toast

const val MSG_SURFACE_DISPLAY = 1
const val MSG_SURFACE_PACKAGE = 2
class HostService:Service() {

    private val TAG = "HostService"

    private var messenger: Messenger? = null

    inner class InternalHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == MSG_SURFACE_DISPLAY){
                val bundle = msg.data
                val displayId = bundle.getInt("displayId")
                val width = bundle.getInt("width")
                val height = bundle.getInt("height")
                val hostToken = bundle.getBinder("hostToken")
                val display = getSystemService(DisplayManager::class.java)!!.getDisplay(displayId)
                val surfaceControlViewHost = SurfaceControlViewHost(this@HostService,display,hostToken)

                val root_view = LayoutInflater.from(this@HostService).inflate(R.layout.show_layout,null)
                surfaceControlViewHost.setView(root_view, width, height)
                msg.replyTo.send(obtainSurfacePkg(surfaceControlViewHost.surfacePackage!!))
            }
        }

        fun obtainSurfacePkg(pkg:SurfacePackage):Message {
            val msg = Message.obtain()
            msg.what = MSG_SURFACE_PACKAGE
            if (pkg != null) {
                val bundle = msg.data
                bundle.putParcelable("surfacePkg", pkg)
            }
            return msg
        }
    }
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG,"onCreate")
        messenger = Messenger(InternalHandler(Looper.getMainLooper()))
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG,"onBind")
        return messenger?.binder
    }

    fun getProcessName(context: Context): String {
        val pid = Process.myPid()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (appProcess in activityManager.runningAppProcesses) {
            if (appProcess.pid == pid) {
                return appProcess.processName
            }
        }
        return "process_not_found"
    }
}