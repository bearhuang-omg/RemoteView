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
import com.bear.remoteview.Utils


const val TAG = "ClientActivity"

class ClientActivity : ComponentActivity() {


    val remoteView: RemoteView by lazy {
        findViewById(R.id.remoteview)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_layout)
        findViewById<TextView>(R.id.testTextView).setOnClickListener {
            Toast.makeText(this, "testClient", Toast.LENGTH_SHORT).show()
            remoteView.start(1)
            remoteView.sendMsg("changeScene", null) { result: Bundle? ->
                Log.i(TAG, "received result,${Utils.getBundleStr(result)}")
            }
        }
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