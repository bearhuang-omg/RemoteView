package com.bear.remoteview

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

class RemoteView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val TAG = "RemoteView"

    private val mLifecycleScope by lazy {
        var scope: CoroutineScope? = this.findViewTreeLifecycleOwner()?.lifecycleScope
        if (scope == null) {
            scope = GlobalScope
        }
        scope!!
    }

    private var ipcClient: IpcClient? = null
    private var mIdentity: Int = 0
    private var mSurfaceView: SurfaceView


    init {
        mSurfaceView = object : SurfaceView(context) {
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
            }
        }
        this.addView(mSurfaceView)
        mSurfaceView.setZOrderOnTop(true)
        mSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        val layoutParams = mSurfaceView.layoutParams
        layoutParams.height = LayoutParams.MATCH_PARENT
        layoutParams.width = LayoutParams.MATCH_PARENT
        mSurfaceView.layoutParams = layoutParams
    }

    fun start(identity: Int) {
        mIdentity = identity
        ipcClient = IpcClient(context, identity)
        ipcClient?.bindService()
        ipcClient?.setConnectStateChangeListener { connectState ->
            if (connectState == Constant.ConnectState.SERVICE_CONNECTED) {
                bindSurfacePkg()
            }
        }
        Log.i(TAG, "width:${mSurfaceView.width},height:${mSurfaceView.height}")
    }

    private fun bindSurfacePkg() {
        val bundle = Bundle()
        bundle.putString(Constant.Parms.SUBCOMMANDER, Constant.Request.BIND_SURFACEPKG)
        bundle.putBinder(Constant.Parms.HOST_TOKEN, mSurfaceView.hostToken)
        bundle.putInt(Constant.Parms.DISPLAY_ID, mSurfaceView.display.displayId)
        ipcClient?.call(bundle) { result: Bundle? ->
            val code = result?.getInt(Constant.Response.RESULT_CODE)
            val msg = result?.getString(Constant.Response.RESULT_MSG)
            Log.i(TAG, "bind surface pkg, code:$code , msg: $msg")
            if (code == Constant.Response.SUCCESS) {
                Utils.UI {
                    val parms = result.getBundle(Constant.Request.PARAMS)
                    val surfacePkg: SurfacePackage? = parms?.getParcelable(Constant.Parms.SURFACEPKG)
                    surfacePkg?.let {
                        mSurfaceView.setChildSurfacePackage(it)
                    }
                }
            } else {
                Log.i(TAG, "bind surfacepkg failed")
            }
        }
    }


    fun sendMsg(commander: String, params: Bundle?, callback: ((result: Bundle?) -> Unit)? = null) {
        val innerParams = params ?: Bundle()
        innerParams.putString(Constant.Parms.SUBCOMMANDER, commander)
        ipcClient?.call(innerParams, callback)
    }

    fun onListen(event: String, callback: ((result: Bundle?) -> Unit)) {
        ipcClient?.onListen(event, callback)
    }

    fun offListen(event: String) {
        ipcClient?.offListen(event)
    }

    fun release() {
        ipcClient?.release()
    }
}