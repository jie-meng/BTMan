package com.thoughtworks.btman

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.github.piasy.rxandroidaudio.StreamAudioPlayer
import com.github.piasy.rxandroidaudio.StreamAudioRecorder
import com.thoughtworks.btman.definitions.APP_TAG
import com.thoughtworks.btman.utils.PermissionManager
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var radioDeviceMic: RadioButton
    private lateinit var radioBluetoothMic: RadioButton

    private lateinit var mStreamAudioRecorder: StreamAudioRecorder
    private lateinit var mStreamAudioPlayer: StreamAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initPermissions()
    }

    private fun initUI() {
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        radioDeviceMic = findViewById(R.id.device_mic)
        radioBluetoothMic = findViewById(R.id.bluetooth_mic)

        btnStart.setOnClickListener {
            if (radioBluetoothMic.isChecked) {
                // start bluetooth mic
                val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                manager.isBluetoothScoOn = true
                manager.startBluetoothSco()
            }

            mStreamAudioPlayer.init()
            startPlayback()

            changeUIState(true)
        }

        btnStop.setOnClickListener {
            if (radioBluetoothMic.isChecked) {
                // stop bluetooth mic
                val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                manager.isBluetoothScoOn = false
                manager.stopBluetoothSco()
            }

            mStreamAudioPlayer.release()

            changeUIState(false)
        }
    }

    private fun changeUIState(start: Boolean) {
        btnStart.isEnabled = !start
        btnStop.isEnabled = start
        radioDeviceMic.isEnabled = !start
        radioBluetoothMic.isEnabled = !start
    }


    private fun initPermissions() {
        PermissionManager.addRequestPermissions(
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
        PermissionManager.setRequestFailCallback { exitProcess(0) }
        PermissionManager.addRequestCallback {
            start()
        }
        PermissionManager.requestPermissions(this)
    }

    private fun start() {
        initUI()

        mStreamAudioRecorder = StreamAudioRecorder.getInstance()
        mStreamAudioPlayer = StreamAudioPlayer.getInstance()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun startPlayback() {
        mStreamAudioRecorder.start(object : StreamAudioRecorder.AudioDataCallback {
            override fun onAudioData(data: ByteArray, size: Int) {
                mStreamAudioPlayer.play(data, size)
            }

            override fun onError() {
                Log.e(APP_TAG, "Record fail")
            }
        })
    }
}
