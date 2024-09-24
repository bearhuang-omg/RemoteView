package com.bear.remoteviewhost

import android.os.Bundle

object RemoteHost {

    private val TAG = "RemoteHost"
    private val callbackMap: HashMap<Int, (Bundle) -> Unit>

    init {
        callbackMap = HashMap()

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