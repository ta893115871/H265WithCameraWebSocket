package com.bj.gxz.h265withcamerawebsocket.socket

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer

/**
 * Created by guxiuzhong@baidu.com on 2021/1/23.
 */
class LiveWebSocketClient : BaseWebSocket() {

    companion object {
        private const val TAG = "LiveWebSocketClient"
        private const val PORT = 30000
        // 另一台手机的IP
        private const val URL = "ws://192.168.1.3:$PORT"
    }

    private var myWebSocketClient: MyWebSocketClient? = null


    override fun sendData(bytes: ByteArray?) {
        if (myWebSocketClient?.isOpen == true) {
            myWebSocketClient?.send(bytes)
        }
    }

    override fun start() {
        try {
            val url = URI(URL)
            myWebSocketClient = MyWebSocketClient(url)
            myWebSocketClient?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun release() {
        try {
            myWebSocketClient?.close()
            h265ReceiveListener = null
            Log.d(TAG, "release ok")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private inner class MyWebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.i(TAG, "onOpen")
        }

        override fun onMessage(message: String?) {
            Log.i(TAG, "onMessage:$message")
        }

        override fun onMessage(bytes: ByteBuffer) {
            if (h265ReceiveListener != null) {
                val buf = ByteArray(bytes.remaining())
                bytes.get(buf)
                Log.i(TAG, "onMessage:" + buf.size)
                h265ReceiveListener?.onReceive(buf)
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            Log.i(TAG, "onClose: $reason ,code=$code")
        }

        override fun onError(ex: Exception?) {
            Log.i(TAG, "onError: ", ex)
        }
    }
}