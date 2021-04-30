package com.bj.gxz.h265withcamerawebsocket.util

/**
 * Created by guxiuzhong on 2021/1/23.
 */
object YUVUtil {

    fun nv21toNv12(nv21: ByteArray): ByteArray {
        val size = nv21.size
        val nv12 = ByteArray(size)
        val y_len = size * 2 / 3
        // Y
        System.arraycopy(nv21, 0, nv12, 0, y_len)
        var i = y_len
        // nv12和nv21是奇偶交替
        while (i < size - 1) {
            nv12[i] = nv21[i + 1]
            nv12[i + 1] = nv21[i]
            i += 2
        }
        return nv12
    }

    fun dataTo90(data: ByteArray, output: ByteArray, width: Int, height: Int) {
        val y_len = width * height
        // uv数据高为y数据高的一半
        val uvHeight = height shr 1 // kotlin 的shr 1 就是右移1位 height >> 1
        var k = 0
        for (j in 0 until width) {
            for (i in height - 1 downTo 0) {
                output[k++] = data[width * i + j]
            }
        }
        // uv
        var j = 0
        while (j < width) {
            for (i in uvHeight - 1 downTo 0) {
                output[k++] = data[y_len + width * i + j]
                output[k++] = data[y_len + width * i + j + 1]
            }
            j += 2
        }
    }
}
