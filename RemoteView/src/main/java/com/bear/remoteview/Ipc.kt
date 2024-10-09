package com.bear.remoteview

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log

class Ipc(val context: Context, val remoteViewId: String) {

    val TAG = "IPC"
    val PKG = "com.bear.remoteviewhost"
    val SERVICE_CLASS = "com.bear.remoteviewhost.Service"
    val RETRY_GAP_TIME = 3000L
    val MAX_RECONNECT_WAIT_TIME = 10 * 60 * 1000L //服务重连最长的时间间隔
    val MAX_CALL_QUEUE = 500 //pendding队列最多存放请求数

    val MSG_SERVICE_RECONNECT = -1 //重连的消息
    val MSG_REBIND_CLIENT = -2 //重新建立client和service的连接通道


    private var mHandler: Handler
    private var mHandlerThread: HandlerThread
    private var mReconnectTime = 0
    private var mServiceConnectState = Constant.SERVICE_DISCONNECTED
    private var mServiceBinder: RemoteCall? = null //服务端的binder
    private var mClientBinder: RemoteCall //客户端的binder
    private val mProcessName: String by lazy {
        Utils.getProcessName(context)
    }

    private val mCallbackMap = HashMap<Int, (Bundle) -> Unit>()
    private val mListenerMap = HashMap<String, (Bundle) -> Unit>()
    private val mPendingTask = ArrayDeque<Runnable>()

    init {
        mHandlerThread = HandlerThread(TAG)
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper) { msg ->
            when {
                msg.what == MSG_SERVICE_RECONNECT -> {
                    reConnect()
                }

                msg.what == MSG_REBIND_CLIENT -> {
                    bindClient()
                }

                msg.what >= 0 -> {//表示是调用超时的消息
                    val callback = mCallbackMap.get(msg.what)
                    callback?.let {
                        mCallbackMap.remove(msg.what)
                        val timeoutResult = Bundle()
                        timeoutResult.putInt(
                            Constant.Response.RESULT_CODE,
                            Constant.Response.TIMEOUT
                        )
                        timeoutResult.putString(Constant.Response.RESULT_MSG, "timeout")
                        it.invoke(timeoutResult)
                    }
                }
            }
            return@Handler true
        }
        mClientBinder = object : RemoteCall.Stub() {
            override fun call(bundle: Bundle?) {
                post {
                    //服务端调用客户端会走到这里
                    val callId = bundle?.getInt(Constant.Parms.CALLID)
                    callId?.let {
                        mHandler.removeMessages(callId)
                        mCallbackMap[it]?.invoke(bundle)
                        mCallbackMap.remove(callId)
                    }
                }
            }
        }
    }

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            post {
                Log.i(TAG, "service connected")
                mHandler.removeMessages(MSG_SERVICE_RECONNECT)
                mServiceConnectState = Constant.SERVICE_CONNECTED
                mServiceBinder = RemoteCall.Stub.asInterface(service)
                bindClient()
                mPendingTask.forEach {
                    mHandler.post(it)
                }
                mPendingTask.clear()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            post {
                Log.i(TAG, "service disconnected")
                mServiceConnectState = Constant.SERVICE_DISCONNECTED
                mHandler.removeMessages(MSG_REBIND_CLIENT)
                reConnect()
            }
        }
    }

    private fun post(runnable: Runnable) {
        if (mHandler.getLooper() != Looper.myLooper()) {
            mHandler.post(runnable)
        } else {
            runnable.run()
        }
    }

    private fun reConnect() {
        mHandler.removeMessages(MSG_SERVICE_RECONNECT)
        bindService()
        mReconnectTime++
        val gapTime = Math.min(mReconnectTime * RETRY_GAP_TIME, MAX_RECONNECT_WAIT_TIME)
        mHandler.sendEmptyMessageDelayed(MSG_SERVICE_RECONNECT, gapTime)
    }

    fun bindService() {
        post {
            val intent = Intent().setComponent(ComponentName(PKG, SERVICE_CLASS))
            context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun callService(bundle: Bundle, callback: ((result: Bundle?) -> Unit)?) {
        post {
            if (mServiceConnectState == Constant.SERVICE_CONNECTED) {
                realCallService(bundle, callback)
            } else {
                //若服务未连接，则先将请求加入到pendding队列当中
                if (mPendingTask.size > MAX_CALL_QUEUE) {
                    val first = mPendingTask.removeFirst()
                    first.run()
                } else {
                    val task = Runnable {
                        realCallService(bundle, callback)
                    }
                    mPendingTask.addLast(task)
                    mHandler.postDelayed(object :Runnable{
                        override fun run() {
                            if (mPendingTask.contains(task)){
                                task.run()
                            }
                        }
                    },Constant.TIMEOUT)
                }
            }
        }
    }

    private fun realCallService(bundle: Bundle, callback: ((result: Bundle?) -> Unit)?) {
        if (mServiceConnectState == Constant.SERVICE_CONNECTED) {
            val params = bundle.getBundle(Constant.Request.PARAMS) ?: Bundle()
            val callId = Utils.generateCallId()
            params.putString(Constant.Parms.REMOTEVIEW_ID, remoteViewId)
            params.putString(Constant.Parms.PROCESSNAME, mProcessName)
            params.putInt(Constant.Parms.CALLID, callId)
            bundle.putBundle(Constant.Request.PARAMS, params)
            callback?.let {
                mCallbackMap[callId] = callback
            }
            mServiceBinder?.call(bundle)

            //超时回调
            val msg = Message.obtain()
            msg.what = callId
            msg.data = bundle
            mHandler.sendMessageDelayed(msg, Constant.TIMEOUT)
        } else {
            callback?.let {
                val serviceDisconnectResult = Bundle()
                serviceDisconnectResult.putInt(
                    Constant.Response.RESULT_CODE,
                    Constant.Response.SERVICE_DISCONNECT
                )
                serviceDisconnectResult.putString(
                    Constant.Response.RESULT_MSG,
                    "service disconnect"
                )
                it.invoke(serviceDisconnectResult)
            }
        }
    }

    private fun bindClient() {
        val bundle = Bundle()
        bundle.putString(Constant.Request.CMDER, Constant.Request.BIND_SERVICE)
        val params = Bundle()
        params.putBinder(Constant.Parms.CLIENT_BINDER, mClientBinder.asBinder())
        bundle.putBundle(Constant.Request.PARAMS, params)
        callService(bundle) { result: Bundle? ->
            val code = result?.getInt(Constant.Response.RESULT_CODE)
            val msg = result?.getString(Constant.Response.RESULT_MSG)
            Log.i(TAG, "bind remote service, code:$code , msg: $msg")
            if (code == Constant.Response.SUCCESS) {
                mHandler.removeMessages(MSG_REBIND_CLIENT)
            } else {
                mHandler.sendEmptyMessageDelayed(MSG_REBIND_CLIENT, RETRY_GAP_TIME)
            }
        }
    }


}