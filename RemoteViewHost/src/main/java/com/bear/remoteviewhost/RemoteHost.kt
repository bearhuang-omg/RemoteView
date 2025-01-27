package com.bear.remoteviewhost

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Contacts.Intents.UI
import android.view.LayoutInflater
import android.view.SurfaceControlViewHost
import android.view.View
import android.widget.ImageView
import com.bear.remoteview.Constant
import com.bear.remoteview.Utils


object RemoteHost {

    private var mContext: Context? = null
    private var ipcService: IpcService? = null
    private val mSurfaceControllerMap = HashMap<Int, SurfaceControlViewHost>()
    private var outHandlerClientMsg: ((Bundle, IpcService.RemoteClient) -> Unit)? = null

    internal fun onServiceCreated(context: Context, ipcService: IpcService) {
        this.mContext = context
        this.ipcService = ipcService
        this.ipcService?.handleClientCall { bundle: Bundle ->
            innerHandleClientMsg(bundle)
        }
    }

    internal fun onServiceDestroyed() {
        this.mContext = null
        this.ipcService?.release()
        this.ipcService = null
    }

    private fun innerHandleClientMsg(bundle: Bundle) {
        val parms = bundle.getBundle(Constant.Request.PARAMS)
        val identity = parms?.getInt(Constant.Parms.IDENTITY)
        val subCmd = parms?.getString(Constant.Parms.SUBCOMMANDER)
        if (subCmd == Constant.Request.BIND_SURFACEPKG && identity != null) {//绑定surfacepkg，内部处理
            Utils.UI {
                if (!mSurfaceControllerMap.containsKey(identity)) {
                    val displayId = parms.getInt(Constant.Parms.DISPLAY_ID)
                    val hostToken = parms.getBinder(Constant.Parms.HOST_TOKEN)
                    val display =
                        mContext!!.getSystemService(DisplayManager::class.java)!!
                            .getDisplay(displayId)
                    val surfaceControlViewHost =
                        SurfaceControlViewHost(mContext!!, display, hostToken)
                    mSurfaceControllerMap[identity] = surfaceControlViewHost
                }
                val surfacePkg: SurfaceControlViewHost.SurfacePackage? =
                    mSurfaceControllerMap[identity]?.surfacePackage
                val client = ipcService?.getClient(identity)
                if (surfacePkg != null) {
                    client?.binder?.let {
                        val response = Bundle()
                        response.putAll(bundle)
                        parms.putParcelable(Constant.Parms.SURFACEPKG, surfacePkg)
                        response.putBundle(Constant.Request.PARAMS, parms)
                        response.putInt(Constant.Response.RESULT_CODE, Constant.Response.SUCCESS)
                        response.putString(Constant.Response.RESULT_MSG, "success")
                        it.call(response)
                    }
                } else {
                    client?.binder?.let {
                        val response = Bundle()
                        response.putAll(bundle)
                        response.putInt(Constant.Response.RESULT_CODE, Constant.Response.FAILED)
                        response.putString(Constant.Response.RESULT_MSG, "surfacepkg is null")
                        it.call(response)
                    }
                }
            }
        } else {
            identity?.let {
                val remoteClient = ipcService?.getClient(it)
                remoteClient?.let {
                    this.outHandlerClientMsg?.invoke(bundle, remoteClient)
                }
            }
        }
    }

    fun setClientMsgHandler(msgHandler: (Bundle, IpcService.RemoteClient) -> Unit) {
        this.outHandlerClientMsg = msgHandler
    }

    fun setView(identity: Int, view: View, width: Int, height: Int) {
        Utils.UI {
            mSurfaceControllerMap[identity]?.let {
                it.setView(view, width, height)
            }
        }
    }

    fun sendMsgToClient(identity: Int, bundle: Bundle) {
        ipcService?.sendMsg(identity, bundle)
    }

    fun sendEvent(event: String, bundle: Bundle?) {
        ipcService?.sendEvent(event, bundle)
    }

}