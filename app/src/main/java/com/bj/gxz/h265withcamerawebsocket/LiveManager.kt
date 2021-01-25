package com.bj.gxz.h265withcamerawebsocket

import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.bj.gxz.h265withcamerawebsocket.camera.CameraHelper
import com.bj.gxz.h265withcamerawebsocket.codec.DecodeH265
import com.bj.gxz.h265withcamerawebsocket.codec.EncodeH265
import com.bj.gxz.h265withcamerawebsocket.socket.BaseWebSocket
import com.bj.gxz.h265withcamerawebsocket.socket.LiveWebSocketClient
import com.bj.gxz.h265withcamerawebsocket.socket.LiveWebSocketServer

/**
 * Created by guxiuzhong@baidu.com on 2021/1/23.
 */
class LiveManager(private val localHolder: SurfaceHolder, private val remoteHolder: SurfaceHolder) {

    private lateinit var cameraHelper: CameraHelper
    private var webSocket: BaseWebSocket? = null

    private var encodeH265 = EncodeH265()
    private var decodeH265 = DecodeH265()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var remoteSurface: Surface? = null

    fun init(width: Int, height: Int) {
        cameraHelper = CameraHelper(localHolder, width, height)
        localHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // 1.SurfaceView创建好后，开启摄像头预览
                cameraHelper.startPreview()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraHelper.stopPreview()
            }
        })
        cameraHelper.setPreviewListener(object : CameraHelper.IPreviewListener {
            override fun onPreviewSize(width: Int, height: Int) {
                // 2.预览成功
                previewWidth = width
                previewHeight = height
            }

            override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
                // 3.编码视频
                if (webSocket != null) {
                    encodeH265.encodeFrame(data!!)
                }
            }
        })
        remoteHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //  SurfaceView创建好了
                remoteSurface = holder.surface
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }
        })
    }


    fun start(isServer: Boolean) {
        if (previewHeight == 0 || previewWidth == 0) {
            Log.e("H265", "previewHeight==0 || previewWidth==0")
            return
        }
        if (remoteSurface == null) {
            Log.e("H265", "remoteSurface==null")
            return
        }
        Log.i("H265", "previewWidth=${previewWidth},previewHeight=${previewHeight}")
        // 创建webSocket
        if (isServer) {
            webSocket = LiveWebSocketServer()
        } else {
            webSocket = LiveWebSocketClient()
        }
        webSocket!!.setH265ReceiveListener(object : BaseWebSocket.IH265ReceiveListener {
            override fun onReceive(data: ByteArray?) {
                decodeH265.decode(data!!)
            }
        })
        webSocket!!.start()

        // 编码器
        encodeH265.initEncoder(previewWidth, previewHeight)
        encodeH265.setH265DecodeListener(object : EncodeH265.IH265DecodeListener {
            override fun onDecode(data: ByteArray?) {
                webSocket?.sendData(data)
            }
        })
        // 解码器
        decodeH265.initDecoder(remoteSurface, previewWidth, previewHeight)
    }

    fun stop() {
        webSocket?.release()
        webSocket = null
        encodeH265.releaseEncoder()
        decodeH265.releaseDecoder()
    }
}