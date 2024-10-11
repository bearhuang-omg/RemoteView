package com.example.surfacehost

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceControlViewHost
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
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
//            val imageView = ImageView(this)
//            imageView.background = resources.getDrawable(R.mipmap.test)
//            RemoteHost.setView(1,imageView,60,60)
        }
        RemoteHost.setClientMsgHandler { bundle: Bundle ->
            Log.i(Tag,"recived client msg,${bundle}")
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