package com.bj.gxz.h265withcamerawebsocket

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var liveManager: LiveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        // localSurfaceView透明
        localSurfaceView.holder.setFormat(PixelFormat.TRANSPARENT)
        // localSurfaceView放置在顶层，即始终位于最上层
        localSurfaceView.setZOrderOnTop(true)
        // 简单处理下权限
        requestPermission()
        liveManager = LiveManager(localSurfaceView.holder, remoteSurfaceView.holder)
        // 因为是H265,你可以采用4K的分辨率
        liveManager.init(540, 960)
    }

    fun start(view: View) {
        Log.i("H265", "checkbox.isChecked=${checkbox.isChecked}")
        liveManager.start(checkbox.isChecked)
    }

    fun stop(view: View) {
        liveManager.stop()
    }


    fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA
                ), 1
            )
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        liveManager.stop()
    }
}