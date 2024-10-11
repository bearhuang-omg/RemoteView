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
import com.bear.remoteview.RemoteView

const val MSG_SURFACE_DISPLAY = 1
const val MSG_SURFACE_PACKAGE = 2

const val TAG = "ClientActivity"

class ClientActivity : ComponentActivity() {


    var remoteView:RemoteView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_layout)
        findViewById<TextView>(R.id.testTextView).setOnClickListener {
            Toast.makeText(this, "testClient", Toast.LENGTH_SHORT).show()
            remoteView?.start(1)
        }
        remoteView = RemoteView(this)
        findViewById<FrameLayout>(R.id.container).addView(remoteView)


    }

    override fun onResume() {
        super.onResume()
//        remoteView?.postDelayed(object :Runnable{
//            override fun run() {
//                remoteView?.start(1)
//            }
//
//        },1000)

    }







}