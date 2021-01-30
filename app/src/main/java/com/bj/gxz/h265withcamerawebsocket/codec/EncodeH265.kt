package com.bj.gxz.h265withcamerawebsocket.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.bj.gxz.h265withcamerawebsocket.util.YUVUtil
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * Created by guxiuzhong@baidu.com on 2021/1/23.
 */
class EncodeH265 {
    companion object {
        private const val NAL_I = 19
        private const val NAL_VPS = 32
        private const val FPS = 15
        private const val TAG = "EncodeH265"
    }

    private var mediaCodec: MediaCodec? = null

    // nv21转换成nv12的数据
    private var nv12: ByteArray? = null

    // 旋转之后的yuv数据
    private var yuv: ByteArray? = null
    private var frameIndex: Long = 0
    private val info = MediaCodec.BufferInfo()
    private var vps_sps_pps_buf: ByteArray? = null
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    fun initEncoder(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
        try {
            // H265编码器 video/hevc
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            // 宽高对调的原因是：后置摄像头旋转了90度，yuv数据也旋转了90度
            val mediaFormat =
                MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, height, width)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800_000)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            mediaFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()

            val bufferLength = width * height * 3 / 2
            yuv = ByteArray(bufferLength)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun encodeFrame(nv21: ByteArray) {
        // nv21 格式转换成nv12
        nv12 = YUVUtil.nv21toNv12(nv21)
        // 数据旋转90度
        YUVUtil.dataTo90(nv12!!, yuv!!, previewWidth, previewHeight)

        // 开始编码
        val inputBufferIndex: Int = mediaCodec!!.dequeueInputBuffer(10_1000)
        if (inputBufferIndex >= 0) {
            val byteBuffer: ByteBuffer? = mediaCodec?.getInputBuffer(inputBufferIndex)
            byteBuffer?.clear()
            byteBuffer?.put(yuv!!)
            // PTS
            // 1。 +132的目的是解码端初始化播放器需要时间，防止播放器首帧没有播放的问题，不一定是132us
            // 2。 frameIndex初始值=1，不加132，也可以。
            val presentationTimeUs = 132 + frameIndex * 1000_000 / FPS
            mediaCodec?.queueInputBuffer(inputBufferIndex, 0, yuv!!.size, presentationTimeUs, 0)
            frameIndex++
        }

        var outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(info, 10_000)
        while (outputBufferIndex >= 0) {
            val byteBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex)
            dealFrame(byteBuffer!!)
            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(info, 0)
        }

    }

    private fun dealFrame(byteBuffer: ByteBuffer) {
        // H265的nalu的分割符的下一个字节的类型
        var offset = 4
        if (byteBuffer[2].toInt() == 0x1) {
            offset = 3
        }
        // VPS,SPS,PPS...  H265的nalu头是2个字节，中间的6位bit是nalu类型

        // 0x7E的二进制的后8位是 0111  1110
        // java版本
        // int naluType = (byteBuffer.get(offset) & 0x7E) >> 1;
        val naluType = byteBuffer[offset].and(0x7E).toInt().shr(1)
        Log.d(TAG, "naluType=$naluType")
        // 保存下VPS,SPS,PPS的数据
        if (NAL_VPS == naluType) {
            vps_sps_pps_buf = ByteArray(info.size)
            byteBuffer.get(vps_sps_pps_buf!!)
            Log.d(TAG, "vps_sps_pps_buf size =${vps_sps_pps_buf?.size}")
        } else if (NAL_I == naluType) {
            // 因为是网络传输，所以在每个i帧之前先发送VPS,SPS,PPS
            val bytes = ByteArray(info.size)
            byteBuffer.get(bytes)
            val newBuf = ByteArray(info.size + vps_sps_pps_buf!!.size)
            System.arraycopy(vps_sps_pps_buf!!, 0, newBuf, 0, vps_sps_pps_buf!!.size)
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf!!.size, bytes.size)

            // 发送
            Log.d(TAG, "send I帧:${newBuf.size}")
            h265DecodeListener?.onDecode(newBuf)
        } else {
            // 其它bp帧数据
            val bytes = ByteArray(info.size)
            byteBuffer.get(bytes)

            // 发送
            Log.d(TAG, "send P/B帧:${bytes.size}")
            h265DecodeListener?.onDecode(bytes)
        }
    }

    fun releaseEncoder() {
        mediaCodec?.stop()
        mediaCodec?.release()
        Log.d(TAG, "releaseEncoder ok")
    }

    private var h265DecodeListener: IH265DecodeListener? = null

    fun setH265DecodeListener(l: IH265DecodeListener?) {
        this.h265DecodeListener = l
    }

    interface IH265DecodeListener {
        fun onDecode(data: ByteArray?)
    }
}