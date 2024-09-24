package com.bear.remoteviewhost

import android.app.Service
import android.content.Intent
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

class Service : Service() {

    data class RemoteClient(val remoteViewId: String, val processName: String, val binder: RemoteCall)

    private val TAG = "RemoteViewService"

    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private val mClientMap = HashMap<String, RemoteClient>()
    private val mSurfaceControllerMap = HashMap<Int, SurfaceControlViewHost>()
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
                        getSystemService(DisplayManager::class.java)!!.getDisplay(displayId)
                    val surfaceControlViewHost = SurfaceControlViewHost(this, display, hostToken)
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


    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread(TAG)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    override fun onBind(intent: Intent?): IBinder {
        return mServiceBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}