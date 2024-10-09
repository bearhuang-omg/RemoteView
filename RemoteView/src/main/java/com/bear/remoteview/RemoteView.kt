package com.bear.remoteview

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RemoteView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val TAG = "RemoteView"

    private var mServiceConnectState = Constant.SERVICE_DISCONNECTED
    private var mSurfaceView: SurfaceView
    private var mChannel = Constant.DEFAULT_CHANNEL
    private var mHandler: Handler
    private var mHandlerThread: HandlerThread

    private var mServiceBinder: RemoteCall? = null //服务端的binder
    private var mClientBinder: RemoteCall //客户端的binder
    private val mProcessName: String by lazy {
        Utils.getProcessName(context)
    }
    private val mRemoteViewId: String by lazy {
        mProcessName + ":" + Utils.generateRemoteViewId()
    }
    private val mLifecycleScope by lazy {
        var scope: CoroutineScope? = this.findViewTreeLifecycleOwner()?.lifecycleScope
        if (scope == null) {
            scope = GlobalScope
        }
        scope!!
    }
    private val mCallbackMap = HashMap<Int, (Bundle) -> Unit>()
    private val mListenerMap = HashMap<String, (Bundle) -> Unit>()
    private val mPendingTask = ArrayList<Runnable>()

    init {
        mSurfaceView = object : SurfaceView(context) {
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
            }
        }
        this.addView(mSurfaceView)
        mHandlerThread = HandlerThread(TAG)
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
        mClientBinder = object : RemoteCall.Stub() {
            override fun call(bundle: Bundle?) {
                //服务端调用客户端会走到这里
                val callId = bundle?.getInt(Constant.Parms.CALLID)
                callId?.let {
                    mCallbackMap[it]?.invoke(bundle)
                }
            }
        }
    }

    fun start(channel: Int = Constant.DEFAULT_CHANNEL) {
        mChannel = channel
        mLifecycleScope.launch {
            bindService()
            val bindServiceResult = bindClient()
            if (bindServiceResult) {
                bindSurfacePkg()
            }
        }
    }

    private suspend fun bindService(): Boolean {
        return suspendCoroutine { continuation ->
            val intent = Intent().setComponent(
                ComponentName(
                    "com.bear.remoteviewhost",
                    "com.bear.remoteviewhost.Service"
                )
            )
            context.bindService(intent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.i(TAG, "service connected")
                    mServiceConnectState = Constant.SERVICE_CONNECTED
                    mServiceBinder = RemoteCall.Stub.asInterface(service)
                    continuation.resume(true)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.i(TAG, "service disconnected")
                    mServiceConnectState = Constant.SERVICE_DISCONNECTED
                    continuation.resume(false)
                }

            }, Context.BIND_AUTO_CREATE)
        }
    }

    private fun callService(bundle: Bundle, callback: ((result: Bundle?) -> Unit)?) {
        mHandler.post {
            val callId = Utils.generateCallId()
            bundle.putString(Constant.Parms.REMOTEVIEW_ID, mRemoteViewId)
            bundle.putString(Constant.Parms.PROCESSNAME, mProcessName)
            bundle.putInt(Constant.Parms.CALLID, callId)
            callback?.let {
                mCallbackMap[callId] = callback
            }
            mServiceBinder?.call(bundle)
            mHandler.postDelayed(object : Runnable {
                override fun run() {
                    mCallbackMap.remove(callId)
                    val timeoutResult = Bundle()
                    timeoutResult.putInt(Constant.Response.RESULT_CODE, Constant.Response.TIMEOUT)
                    timeoutResult.putString(Constant.Response.RESULT_MSG, "timeout")
                    callback?.invoke(timeoutResult)
                }
            }, Constant.TIMEOUT)
        }
    }

    private suspend fun bindClient(): Boolean {
        return suspendCoroutine { continuation ->
            val bundle = Bundle()
            bundle.putString(Constant.Request.CMDER, Constant.Request.BIND_SERVICE)
            bundle.putBinder(Constant.Parms.CLIENT_BINDER, mClientBinder.asBinder())
            callService(bundle) { result: Bundle? ->
                val code = result?.getInt(Constant.Response.RESULT_CODE)
                val msg = result?.getString(Constant.Response.RESULT_MSG)
                Log.i(TAG, "bind remote service, code:$code , msg: $msg")
                if (code == Constant.Response.SUCCESS) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    private suspend fun bindSurfacePkg(): Boolean {
        return suspendCoroutine { continuation ->
            val bundle = Bundle()
            bundle.putString(Constant.Request.CMDER, Constant.Request.BIND_SURFACEPKG)
            bundle.putBinder(Constant.Parms.HOST_TOKEN, mSurfaceView.hostToken)
            bundle.putInt(Constant.Parms.DISPLAY_ID, mSurfaceView.display.displayId)
            bundle.putInt(Constant.Parms.CHANNEL, mChannel)
            callService(bundle) { result: Bundle? ->
                val code = result?.getInt(Constant.Response.RESULT_CODE)
                val msg = result?.getString(Constant.Response.RESULT_MSG)
                Log.i(TAG, "bind surface pkg, code:$code , msg: $msg")
                if (code == Constant.Response.SUCCESS) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    private fun reConnectService() {
        if (mServiceConnectState == Constant.SERVICE_DISCONNECTED) {
            mServiceConnectState = Constant.SERVICE_CONNECTING
            mLifecycleScope.launch {
                for (i in 0..Constant.MAX_RECONNECT_TIME) {
                    bindService()
                    if (mServiceConnectState == Constant.SERVICE_CONNECTED) {
                        break
                    } else {
                        delay(Constant.WAIT_TIME)
                    }
                }
                if (mServiceConnectState == Constant.SERVICE_CONNECTED) {
                    bindClient()
                    mPendingTask.forEach { task ->
                        mHandler.post(task)
                    }
                    mPendingTask.clear()
                }
                if (mServiceConnectState == Constant.SERVICE_CONNECTING) {
                    mServiceConnectState = Constant.SERVICE_DISCONNECTED
                }
            }
        }
    }


    fun sendMsg(commander: String, params: Bundle, callback: ((result: Bundle?) -> Unit)? = null) {
        handler.post {
            val runnable = object : Runnable {
                override fun run() {
                    val bundle = Bundle()
                    bundle.putString(Constant.Request.CMDER, Constant.Request.SEND_MSG)
                    bundle.putString(Constant.Parms.SUBCOMMANDER, commander)
                    bundle.putAll(params)
                    callService(bundle, callback)
                }
            }
            if (mServiceConnectState == Constant.SERVICE_DISCONNECTED) {
                runnable.run()
            } else {
                mPendingTask.add(runnable)
                reConnectService()
            }
        }
    }

    fun onListen(event: String, callback: ((result: Bundle?) -> Unit)) {
        mHandler.post {
            val runnalbe = object : Runnable {
                override fun run() {
                    val callId = Utils.generateCallId()
                    val bundle = Bundle()
                    bundle.putString(Constant.Request.CMDER, Constant.Request.LISTENER)
                    bundle.putString(Constant.Parms.SUBCOMMANDER, event)
                    bundle.putString(Constant.Parms.REMOTEVIEW_ID, mRemoteViewId)
                    bundle.putString(Constant.Parms.PROCESSNAME, mProcessName)
                    bundle.putInt(Constant.Parms.CALLID, callId)
                    mListenerMap[event] = callback
                    mServiceBinder?.call(bundle)
                }
            }
            if (mServiceConnectState == Constant.SERVICE_CONNECTED) {
                runnalbe.run()
            } else {
                mPendingTask.add(runnalbe)
                reConnectService()
            }
        }
    }

    fun offListen(event: String) {
        mHandler.post {
            mListenerMap.remove(event)
        }
    }

    fun release() {
        mListenerMap.clear()
        mCallbackMap.clear()
    }
}