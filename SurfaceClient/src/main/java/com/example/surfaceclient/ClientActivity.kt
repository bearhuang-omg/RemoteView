package com.example.surfaceclient

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

const val MSG_SURFACE_DISPLAY = 1
const val MSG_SURFACE_PACKAGE = 2

const val TAG = "ClientActivity"

class ClientActivity : ComponentActivity() {

    val surfaceview by lazy {
        findViewById<SurfaceView>(R.id.client_surfaceView)
    }

    val handler = InternalHandler(Looper.getMainLooper())
    val handleMessenger = Messenger(handler)
    var remoteMessenger: Messenger? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_layout)
        findViewById<TextView>(R.id.testTextView).setOnClickListener {
            Toast.makeText(this, "testClient", Toast.LENGTH_SHORT).show()
            bindSurfaceService()
        }
//        val root = findViewById<FrameLayout>(R.id.root)
        surfaceview.setZOrderOnTop(true)
        surfaceview.holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    inner class InternalHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_SURFACE_PACKAGE) {
                val data = msg.data
                val surfacePackage: SurfacePackage = data.getParcelable("surfacePkg")!!
                surfaceview.setChildSurfacePackage(surfacePackage)
            }
        }
    }

    fun bindSurfaceService() {
        Log.i(TAG,"bindSurfaceService")
        val intent = Intent().setComponent(
            ComponentName(
                "com.example.surfacehost",
                "com.example.surfacehost.HostService"
            )
        )
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.i(TAG, "service connected")
                handler.post {
                    remoteMessenger = Messenger(service)
                    val msg = obtainSurfaceDisplay(
                        surfaceview.hostToken,
                        surfaceview.display.displayId,
                        surfaceview.width,
                        surfaceview.height
                    )
                    msg?.replyTo = handleMessenger
                    remoteMessenger?.send(msg)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.i(TAG, "service disconnected")
            }

        }, Context.BIND_AUTO_CREATE)
    }

    fun obtainSurfaceDisplay(
        hostToken: IBinder?,
        displayId: Int,
        width: Int,
        height: Int
    ): Message? {
        val msg = Message.obtain()
        msg.what = MSG_SURFACE_DISPLAY
        val bundle = msg.data
        bundle.putBinder("hostToken", hostToken)
        bundle.putInt("displayId", displayId)
        bundle.putInt("width", width)
        bundle.putInt("height", height)
        return msg
    }

}