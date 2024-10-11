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

class IpcClient(val context: Context, val identity: Int) {

    val TAG = "IPC"
    val PKG = "com.example.surfacehost"
    val SERVICE_CLASS = "com.bear.remoteviewhost.Service"
    val RETRY_GAP_TIME = 3000L
    val MAX_RECONNECT_WAIT_TIME = 10 * 60 * 1000L //服务重连最长的时间间隔
    val MAX_CALL_QUEUE = 500 //pendding队列最多存放请求数

    val MSG_SERVICE_RECONNECT = -1 //重连的消息
    val MSG_REBIND_CLIENT = -2 //重新建立client和service的连接通道


    private var mHandler: Handler
    private var mHandlerThread: HandlerThread
    private var mReconnectTime = 0
    private var mServiceConnectState = Constant.SERVICE_DISCONNECTED //服务的连接状态
    private var mServiceBinder: RemoteCall? = null //服务端的binder
    private var mClientBinder: RemoteCall //客户端的binder
    private val mProcessName: String by lazy {
        Utils.getProcessName(context)
    }

    private val mCallbackMap = HashMap<Int, (Bundle) -> Unit>()
    private val mListenerMap = HashMap<String, (Bundle) -> Unit>()
    private val mPendingTask = ArrayDeque<Runnable>()
    private var mServiceCall: ((Bundle) -> Unit)? = null

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
                        timeoutResult.putString(
                            Constant.Request.CMDER,
                            Constant.Request.SEND_TO_SERVICE_MSG
                        )
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
                    Log.i(TAG,"recieved service call, ${Utils.getBundleStr(bundle)}")
                    val cmd = bundle?.getString(Constant.Request.CMDER)
                    if (cmd == null) {
                        return@post
                    }
                    try {
                        when (cmd) {
                            Constant.Request.SEND_TO_SERVICE_MSG -> {//客户端给服务端发消息的回调
                                val callId = bundle.getBundle(Constant.Request.PARAMS)
                                    ?.getInt(Constant.Parms.CALLID)
                                callId?.let {
                                    mHandler.removeMessages(it)
                                    mCallbackMap[it]?.invoke(bundle)
                                    mCallbackMap.remove(it)
                                }
                            }

                            Constant.Request.SEND_TO_CLIENT_MSG -> {//服务端主动给客户端发送的消息
                                val parms = bundle.getBundle(Constant.Request.PARAMS)
                                val subCommander = parms?.getString(Constant.Parms.SUBCOMMANDER)
                                if (subCommander == Constant.Request.LISTENER) {
                                    val event = parms.getString(Constant.Parms.EVENT)
                                    mListenerMap[event]?.invoke(bundle)
                                } else {
                                    mServiceCall?.invoke(bundle)
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, ex.toString())
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
                mServiceBinder = RemoteCall.Stub.asInterface(service)
                bindClient()
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

    /**
     * 绑定服务
     */
    fun bindService() {
        post {
            val intent = Intent().setComponent(ComponentName(PKG, SERVICE_CLASS))
            context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * 调用服务端
     */
    fun call(params: Bundle, callback: ((result: Bundle?) -> Unit)? = null) {
        post {
            if (mServiceConnectState == Constant.SERVICE_CONNECTED ) {
                realCallService(params, callback)
            } else {
                //若服务未连接，则先将请求加入到pendding队列当中
                if (mPendingTask.size > MAX_CALL_QUEUE) {
                    val first = mPendingTask.removeFirst()
                    first.run()
                } else {
                    val task = Runnable {
                        if (mServiceConnectState == Constant.SERVICE_CONNECTED) {
                            realCallService(params, callback)
                        } else {
                            callback?.let {
                                val result = Bundle()
                                result.putString(
                                    Constant.Request.CMDER,
                                    Constant.Request.SEND_TO_SERVICE_MSG
                                )
                                result.putBundle(Constant.Request.PARAMS, params)
                                result.putInt(
                                    Constant.Response.RESULT_CODE,
                                    Constant.Response.SERVICE_DISCONNECT
                                )
                                result.putString(
                                    Constant.Response.RESULT_MSG,
                                    "service disconnect"
                                )
                                it.invoke(result)
                            }
                        }
                    }
                    mPendingTask.addLast(task)
                    mHandler.postDelayed(object : Runnable {
                        override fun run() {
                            if (mPendingTask.contains(task)) {
                                task.run()
                                mPendingTask.remove(task)
                            }
                        }
                    }, Constant.TIMEOUT)
                }
            }
        }
    }

    fun handleServiceCall(serviceCall: (Bundle) -> Unit) {
        this.mServiceCall = serviceCall
    }

    /**
     * 监听某个事件
     */
    fun onListen(event: String, callback: ((result: Bundle?) -> Unit)) {
        post {
            mListenerMap[event] = callback
            val params = Bundle()
            params.putString(Constant.Parms.SUBCOMMANDER, Constant.Request.LISTENER)
            params.putString(Constant.Parms.EVENT, event)
            call(params)
        }
    }

    /**
     * 取消监听某个事件
     */
    fun offListen(event: String) {
        post {
            mListenerMap.remove(event)
            val params = Bundle()
            params.putString(Constant.Parms.SUBCOMMANDER, Constant.Request.UNLISTEN)
            params.putString(Constant.Parms.EVENT, event)
            call(params)
        }
    }

    private fun realCallService(params: Bundle, callback: ((result: Bundle?) -> Unit)?) {
        val bundle = Bundle()
        bundle.putString(Constant.Request.CMDER, Constant.Request.SEND_TO_SERVICE_MSG)
        val callId = Utils.generateCallId()
        params.putInt(Constant.Parms.IDENTITY, identity)
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
    }

    private fun bindClient() {
        val params = Bundle()
        params.putString(Constant.Parms.SUBCOMMANDER, Constant.Request.BIND_CLIENT)
        params.putBinder(Constant.Parms.CLIENT_BINDER, mClientBinder.asBinder())
        realCallService(params) { result: Bundle? ->
            val code = result?.getInt(Constant.Response.RESULT_CODE)
            val msg = result?.getString(Constant.Response.RESULT_MSG)
            Log.i(TAG, "bind remote service, code:$code , msg: $msg")
            if (code == Constant.Response.SUCCESS) {
                mServiceConnectState = Constant.SERVICE_CONNECTED
                mHandler.removeMessages(MSG_REBIND_CLIENT)
                mPendingTask.forEach {
                    mHandler.post(it)
                }
                mPendingTask.clear()
            } else {
                mHandler.sendEmptyMessageDelayed(MSG_REBIND_CLIENT, RETRY_GAP_TIME)
            }
        }
    }


}