package com.bear.remoteviewhost

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.SurfaceControlViewHost
import android.view.SurfaceControlViewHost.SurfacePackage
import com.bear.remoteview.Constant
import com.bear.remoteview.Constant.Request
import com.bear.remoteview.RemoteCall

object RemoteHost {

    data class RemoteClient(val remoteViewId: String, val processName: String, val binder: RemoteCall)

    private val TAG = "RemoteHost"
    private var mContext: Context? = null
    private var handler: Handler
    private var handlerThread: HandlerThread
    private val mClientMap = HashMap<String, RemoteClient>()
    private val mSurfaceControllerMap = HashMap<Int, SurfaceControlViewHost>()

    init {
        handlerThread = HandlerThread(TAG)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    private val mServiceBinder = object : RemoteCall.Stub() {
        override fun call(bundle: Bundle?) {
            handler.post {
                handleClientCall(bundle)
            }
        }
    }

    private fun handleClientCall(bundle: Bundle?) {
        if (bundle == null) {
            return
        }
        if (mContext == null){
            return
        }
        val cmd = bundle.getString(Request.CMDER)
        val remoteViewId = bundle.getString(Constant.Parms.REMOTEVIEW_ID)
        val processName = bundle.getString(Constant.Parms.PROCESSNAME)
        val callId = bundle.getInt(Constant.Parms.CALLID)
        when (cmd) {
            Request.BIND_SERVICE -> {//绑定服务
                val clientBinder = bundle.getBinder(Constant.Parms.CLIENT_BINDER)
                val clientCall = RemoteCall.Stub.asInterface(clientBinder)
                if (processName != null && clientBinder != null && remoteViewId != null) {
                    mClientMap[remoteViewId] = RemoteClient(remoteViewId, processName, clientCall)
                    clientBinder.linkToDeath(object : IBinder.DeathRecipient {
                        override fun binderDied() {
                            mClientMap.remove(remoteViewId)
                        }

                    }, 0)

                    val response = Bundle()
                    response.putInt(Constant.Parms.CALLID,callId)
                    response.putInt(Constant.Response.RESULT_CODE, Constant.Response.SUCCESS)
                    response.putString(Constant.Response.RESULT_MSG, "success")
                    clientCall.call(response)
                }
            }

            Request.BIND_SURFACEPKG -> {//绑定surfacepkg
                val channel = bundle.getInt(Constant.Parms.CHANNEL)
                if (!mSurfaceControllerMap.containsKey(channel)) {
                    val displayId = bundle.getInt(Constant.Parms.DISPLAY_ID)
                    val hostToken = bundle.getBinder(Constant.Parms.HOST_TOKEN)
                    val display =
                        mContext!!.getSystemService(DisplayManager::class.java)!!.getDisplay(displayId)
                    val surfaceControlViewHost = SurfaceControlViewHost(mContext!!, display, hostToken)
                    mSurfaceControllerMap[channel] = surfaceControlViewHost
                }
                val surfacePkg: SurfacePackage? = mSurfaceControllerMap[channel]?.surfacePackage
                val clientCallback = mClientMap[remoteViewId]?.binder
                if (surfacePkg != null) {
                    clientCallback?.let {
                        val response = Bundle()
                        response.putInt(Constant.Parms.CALLID,callId)
                        response.putInt(Constant.Response.RESULT_CODE, Constant.Response.SUCCESS)
                        response.putString(Constant.Response.RESULT_MSG, "success")
                        response.putParcelable(Constant.Parms.SURFACEPKG, surfacePkg)
                        it.call(response)
                    }
                } else {
                    clientCallback?.let {
                        val response = Bundle()
                        response.putInt(Constant.Parms.CALLID,callId)
                        response.putInt(Constant.Response.RESULT_CODE, Constant.Response.FAILED)
                        response.putString(Constant.Response.RESULT_MSG, "surfacepkg is null")
                        it.call(response)
                    }
                }
            }
        }
    }

    internal fun getServiceBinder():IBinder {
        return mServiceBinder
    }

    internal fun onServiceCreated(context: Context) {
        this.mContext = context
    }

    internal fun onServiceDestroyed() {
        this.mContext = null
    }

//    fun setMsgHandler(channel: Int, block: (Bundle) -> Unit) {
//        ServiceController.getHandler().post {
//            callbackMap[channel] = block
//        }
//    }
//
//    fun removeMsgHandler(channel: Int) {
//        ServiceController.getHandler().post {
//            callbackMap.remove(channel)
//        }
//    }
//
//
//    fun handleMsg(channel: Int, bundle: Bundle) {
//        if (callbackMap.containsKey(channel)) {
//            callbackMap[channel]?.invoke(bundle)
//        }
//    }
}