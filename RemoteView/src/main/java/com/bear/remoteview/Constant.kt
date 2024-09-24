package com.bear.remoteview

object Constant {

    val DEFAULT_CHANNEL = 0
    val TIMEOUT = 5000L

    val MAX_RECONNECT_TIME = 3
    val WAIT_TIME = 3000L

    val SERVICE_CONNECTED = 0
    val SERVICE_DISCONNECTED = 1
    val SERVICE_CONNECTING = 2

    object Request {
        //请求指令
        val CMDER = "cmder"

        //支持的指令
        val BIND_SERVICE = "bind_service" //绑定服务，用于service和client端互相发送消息
        val BIND_SURFACEPKG = "bind_surface_pkg" //绑定surfacepkg，用于远程渲染

        val SEND_MSG = "send_msg" //给service发送消息
        val LISTENER = "listener" //监听一些事件
    }

    object Parms {
        //参数
        val PROCESSNAME = "process_name"
        val REMOTEVIEW_ID = "remote_view_id"
        val CALLID = "call_id"

        val SUBCOMMANDER = "sub_cmd"


        val CLIENT_BINDER = "client_binder"
        val CHANNEL = "channel"
        val DISPLAY_ID = "display_id"
        val WIDTH = "width"
        val HEIGHT = "height"
        val HOST_TOKEN = "host_token"
        val SURFACEPKG = "surfacePkg"
    }

    object Response {
        val RESULT_CODE = "result_code"
        val RESULT_MSG = "result_msg"

        val SUCCESS = 0
        val FAILED = -1
        val TIMEOUT = -2
    }

}