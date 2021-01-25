package com.bj.gxz.h265withcamerawebsocket.camera

import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder

/**
 * Created by guxiuzhong@baidu.com on 2021/1/23.
 */
class CameraHelper(
    private val holder: SurfaceHolder,
    private var width: Int,
    private var height: Int
) : Camera.PreviewCallback {

    companion object {
        var TAG: String = "CameraHelper"
    }

    lateinit var camera: Camera
    lateinit var buffer: ByteArray

    fun startPreview() {
        // 临时用后置摄像头，重点是编解码和数据的传输
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        val parameters: Camera.Parameters = camera.parameters
        // 摄像头默认NV21
        Log.e(TAG, "previewFormat:" + parameters.previewFormat)

        setPreviewSize(parameters)
        camera.setParameters(parameters)

        camera.setPreviewDisplay(holder)
        // 由于硬件安装是横着的，如果是后置摄像头&&正常竖屏的情况下需要旋转90度
        // 只是预览旋转了，数据没有旋转
        camera.setDisplayOrientation(90)
        // 让摄像头回调一帧的数据大小
        buffer = ByteArray(width * height * 3 / 2)
        // onPreviewFrame回调的数据大小就是buffer.length
        camera.addCallbackBuffer(buffer)
        camera.setPreviewCallbackWithBuffer(this)
        camera.startPreview()
        previewListener?.onPreviewSize(width, height)
    }

    private fun setPreviewSize(parameters: Camera.Parameters) {
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        var size = supportedPreviewSizes[0]
        Log.d(TAG, "支持 ${size.width}X${size.height}")
        supportedPreviewSizes.removeAt(0)
        var m: Int = Math.abs(size.width * size.height - width * height)

        val iterator: Iterator<Camera.Size> = supportedPreviewSizes.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            Log.d(TAG, "支持 " + next.width + "x" + next.height)
            val n: Int = Math.abs(next.height * next.width - width * height)
            if (n < m) {
                m = n
                size = next
            }
        }
        width = size.width
        height = size.height
        parameters.setPreviewSize(width, height)
        Log.d(TAG, "设置预览分辨率 width:${width} height:${height}")
    }

    fun stopPreview() {
        if (camera != null) {
            camera.stopPreview()
            camera.release()
            Log.d(TAG, "camera release ok")
        }
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        // 摄像头的原始数据yuv
        previewListener?.onPreviewFrame(data, camera)
        camera!!.addCallbackBuffer(data)
    }

    private var previewListener: IPreviewListener? = null

    fun setPreviewListener(previewListener: IPreviewListener?) {
        this.previewListener = previewListener
    }

    interface IPreviewListener {
        fun onPreviewSize(width: Int, height: Int)
        fun onPreviewFrame(data: ByteArray?, camera: Camera?)
    }
}