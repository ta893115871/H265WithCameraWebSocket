package com.bj.gxz.h265withcamerawebsocket.socket

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Created by guxiuzhong on 2021/1/23.
 */
class LiveWebSocketServer : BaseWebSocket() {

    companion object {
        private const val TAG = "LiveWebSocketServer"
        private const val PORT = 30000
    }

    private var webSocket: WebSocket? = null

    private val webSocketServer: WebSocketServer = object :
        WebSocketServer(InetSocketAddress(PORT)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            Log.d(TAG, "onOpen")
            webSocket = conn
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            Log.d(TAG, "onClose")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            Log.d(TAG, "onMessage:$message")
        }

        override fun onMessage(conn: WebSocket, message: ByteBuffer) {
            super.onMessage(conn, message)
            if (h265ReceiveListener != null) {
                val buf = ByteArray(message.remaining())
                message[buf]
                Log.d(TAG, "onMessage:" + buf.size)
                h265ReceiveListener?.onReceive(buf)
            }
        }

        override fun onError(conn: WebSocket, ex: Exception) {
            Log.d(TAG, "onError ", ex)
        }

        override fun onStart() {
            Log.d(TAG, "onStart")
        }
    }

    override fun sendData(bytes: ByteArray?) {
        if (webSocket?.isOpen == true) {
            webSocket?.send(bytes)
        }
    }

    override fun start() {
        webSocketServer.start()
    }

    override fun release() {
        try {
            webSocket?.close()
            webSocketServer.stop()
            h265ReceiveListener = null
            Log.d(TAG, "release ok")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
