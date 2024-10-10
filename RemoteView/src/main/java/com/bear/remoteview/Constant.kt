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

        //参数
        val PARAMS = "params"

        //一级指令
        val SEND_TO_SERVICE_MSG = "send_to_service_msg" //client端给service发送消息，callback当中保持一致
        val SEND_TO_CLIENT_MSG = "send_to_client_msg" //给client端发送消息，callback当中保持一致


        //二级指令
        val LISTENER = "listen"//客户端监听某个事件
        val UNLISTEN = "unListen"//客户端取消监听某个事件
        val BIND_CLIENT = "bind_client" //绑定服务，用于service和client端互相发送消息
        val BIND_SURFACEPKG = "bind_surface_pkg" //绑定surfacepkg，用于远程渲染

    }

    object Parms {
        //公共参数
        val PROCESSNAME = "process_name"
        val IDENTITY = "identity" //身份标记
        val CALLID = "call_id"

        val SUBCOMMANDER = "sub_cmd"


        val EVENT = "event"
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
        val RESULTS = "results"

        val SUCCESS = 0
        val FAILED = -1
        val TIMEOUT = -2
        val SERVICE_DISCONNECT = -3
    }

}