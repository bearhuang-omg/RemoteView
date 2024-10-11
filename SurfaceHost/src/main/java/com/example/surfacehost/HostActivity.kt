package com.example.surfacehost

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.provider.SyncStateContract.Constants
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceControlViewHost
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.bear.remoteview.Constant
import com.bear.remoteviewhost.RemoteHost

class HostActivity : ComponentActivity() {

    val Tag = "HostActivity_"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)


        val textView = findViewById<TextView>(R.id.testTextView)
        textView.setOnClickListener {
            Toast.makeText(this, "testHost", Toast.LENGTH_SHORT).show()

        }
        RemoteHost.setClientMsgHandler { bundle, client ->
            Log.i(Tag, "recived client msg,${bundle}")
            val view = LayoutInflater.from(this).inflate(R.layout.show_layout, null)
            RemoteHost.setView(1, view, 560, 560)
            bundle.putInt(Constant.Response.RESULT_CODE, Constant.Response.SUCCESS)
            bundle.putString(Constant.Response.RESULT_MSG, "changescene success")
            client.binder.call(bundle)
        }

//        createSurfaceView()
    }

    fun createSurfaceView(){

        val root = findViewById<FrameLayout>(R.id.root)
        val handler = Handler()
        handler.post {
            val surfaceView = SurfaceView(root.context)
            surfaceView.setPadding(50, 100, 0, 0)
            root.addView(surfaceView)

            val surfaceControlViewHost =
                SurfaceControlViewHost(root.context, root.display, surfaceView.hostToken)

            val imageView = ImageView(this)
            imageView.background = resources.getDrawable(R.mipmap.test)

            surfaceControlViewHost.setView(imageView, 60, 60)
            val surfacePackage: SurfacePackage = surfaceControlViewHost.getSurfacePackage()!!
            surfaceView.setChildSurfacePackage(surfacePackage)


            surfaceView.setZOrderOnTop(true)
            surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        }
    }

}