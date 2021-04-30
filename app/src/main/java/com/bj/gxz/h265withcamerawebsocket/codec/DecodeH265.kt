package com.bj.gxz.h265withcamerawebsocket.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.IOException

/**
 * Created by guxiuzhong on 2021/1/23.
 */
class DecodeH265 {

    private var mediaCodec: MediaCodec? = null
    private val info = MediaCodec.BufferInfo()

    companion object {
        private const val TAG = "DecodeH265"
    }

    fun initDecoder(surface: Surface?, width: Int, height: Int) {
        try {
            // H265解码器
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            val mediaFormat =
                MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800_000)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)

            // 渲染到surface上
            mediaCodec?.configure(mediaFormat, surface, null, 0)
            mediaCodec?.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun decode(data: ByteArray) {
        val index = mediaCodec!!.dequeueInputBuffer(10_000)
        if (index >= 0) {
            // 送入数据
            val byteBuffer = mediaCodec!!.getInputBuffer(index)
            byteBuffer!!.clear()
            byteBuffer.put(data)
            mediaCodec!!.queueInputBuffer(
                index, 0, data.size,
                System.currentTimeMillis(), 0
            )
        }
        var outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(info, 10_000)
        while (outputBufferIndex >= 0) {
            // true渲染到surface上
            mediaCodec!!.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(info, 0)
        }
    }

    fun releaseDecoder() {
        try {
            if (mediaCodec != null) {
                mediaCodec!!.stop()
                mediaCodec!!.release()
                Log.d(TAG, "releaseDecoder ok")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
