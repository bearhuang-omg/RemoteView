package com.bear.remoteviewhost

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import com.bear.remoteview.Constant
import com.bear.remoteview.RemoteCall

class IpcService(val context: Context) {

    data class RemoteClient(
        val identity: String,
        val processName: String,
        val binder: RemoteCall
    )

    private val TAG = "IpcService"
    private var handler: Handler
    private var handlerThread: HandlerThread
    private val mClientMap = HashMap<String, RemoteClient>()
    private val mListenerMap = HashMap<String, HashSet<String>>()
    private var mClientCall: ((Bundle) -> Unit)? = null

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
        val params = bundle.getBundle(Constant.Request.PARAMS)
        if (params == null) {
            return
        }
        val subCmd = params.getString(Constant.Parms.SUBCOMMANDER)
        val identity = params.getString(Constant.Parms.IDENTITY)
        val processName = params.getString(Constant.Parms.PROCESSNAME)
        when (subCmd) {
            Constant.Request.BIND_CLIENT -> {//绑定服务
                val clientBinder = params.getBinder(Constant.Parms.CLIENT_BINDER)
                val clientCall = RemoteCall.Stub.asInterface(clientBinder)
                if (processName != null && clientBinder != null && identity != null) {
                    mClientMap[identity] = RemoteClient(identity, processName, clientCall)
                    clientBinder.linkToDeath(object : IBinder.DeathRecipient {
                        override fun binderDied() {
                            mClientMap.remove(identity)
                            mListenerMap.forEach { key, value ->
                                var tobeRemoved: String? = null
                                value.forEach { item ->
                                    if (item == identity) {
                                        tobeRemoved = item
                                    }
                                }
                                if (value.contains(tobeRemoved)) {
                                    value.remove(tobeRemoved)
                                }
                            }
                        }
                    }, 0)
                    val response = bundle
                    response.putInt(Constant.Response.RESULT_CODE, Constant.Response.SUCCESS)
                    response.putString(Constant.Response.RESULT_MSG, "success")
                    clientCall.call(response)
                }
            }

            Constant.Request.LISTENER -> {//监听某个事件
                val event = params.getString(Constant.Parms.EVENT)
                if (event == null || identity == null) {
                    return
                }
                var listeners = mListenerMap[event]
                if (listeners == null) {
                    listeners = HashSet()
                }
                listeners.add(identity)
                mListenerMap[event] = listeners
            }

            Constant.Request.UNLISTEN -> {//取消监听某个事件
                val event = params.getString(Constant.Parms.EVENT)
                if (event == null || identity == null) {
                    return
                }
                mListenerMap[event]?.let {
                    it.remove(identity)
                }
            }

            else -> {
                mClientCall?.invoke(bundle)
            }
        }
    }

    fun sendEvent(event: String, params: Bundle?) {
        val bundle = Bundle()
        val innerParams = Bundle()
        bundle.putString(Constant.Request.CMDER, Constant.Request.SEND_TO_CLIENT_MSG)
        innerParams.putAll(params)
        innerParams.putString(Constant.Parms.SUBCOMMANDER, Constant.Request.LISTENER)
        bundle.putBundle(Constant.Request.PARAMS, innerParams)
        mListenerMap[event]?.forEach { item ->
            mClientMap[item]?.binder?.call(bundle)
        }
    }

    fun sendMsg(identity: String, params: Bundle) {
        val bundle = Bundle()
        bundle.putString(Constant.Request.CMDER, Constant.Request.SEND_TO_CLIENT_MSG)
        bundle.putBundle(Constant.Request.PARAMS, params)
        mClientMap[identity]?.binder?.call(bundle)
    }

    fun handleClientCall(clientCall: (Bundle) -> Unit) {
        this.mClientCall = clientCall
    }

    internal fun getServiceBinder(): IBinder {
        return mServiceBinder
    }
}