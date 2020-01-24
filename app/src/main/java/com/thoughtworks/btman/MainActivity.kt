package com.thoughtworks.btman

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.piasy.rxandroidaudio.StreamAudioPlayer
import com.github.piasy.rxandroidaudio.StreamAudioRecorder
import com.thoughtworks.btman.definitions.APP_TAG
import com.thoughtworks.btman.utils.PermissionManager
import java.io.IOException
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var radioDeviceMic: RadioButton
    private lateinit var radioBluetoothMic: RadioButton

    private lateinit var mStreamAudioRecorder: StreamAudioRecorder
    private lateinit var mStreamAudioPlayer: StreamAudioPlayer

    private val recordFileName =
        Environment.getExternalStorageDirectory().absolutePath + "/record.3gp"
    private lateinit var mediaRecorder: MediaRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initPermissions()
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    private fun initUI() {
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        radioDeviceMic = findViewById(R.id.device_mic)
        radioBluetoothMic = findViewById(R.id.bluetooth_mic)

        btnStart.setOnClickListener {
            start()
        }

        btnStop.setOnClickListener {
            stop()
        }
    }

    private fun stop() {
        if (radioBluetoothMic.isChecked) {
            stopBluetooth()
        } else {
            stopDevice()
        }

        changeUIState(false)
    }

    private fun stopDevice() {
        mStreamAudioPlayer.release()
        mStreamAudioRecorder.stop()
    }

    private fun stopBluetooth() {
        mediaRecorder.stop()
        mediaRecorder.release()

        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (manager.isBluetoothScoOn) {
            manager.isBluetoothScoOn = false
            manager.stopBluetoothSco()
        }
    }

    private fun start() {
        if (radioBluetoothMic.isChecked) {
            if (startBluetooth()) {
                changeUIState(true)
            }
        } else {
            startDevice()
            changeUIState(true)
        }
    }

    private fun startBluetooth(): Boolean {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder.setOutputFile(recordFileName)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.prepare()
        } catch (e: IOException) {
            Toast.makeText(
                this@MainActivity,
                "mRecorder prepare failed",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        // start bluetooth mic
        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (!manager.isBluetoothScoAvailableOffCall) {
            Toast.makeText(
                this@MainActivity,
                "System does not support bluetooth record",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        manager.startBluetoothSco()

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val state = intent!!.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    Toast.makeText(
                        this@MainActivity,
                        "Bluetooth SOC connected",
                        Toast.LENGTH_SHORT
                    ).show()
                    manager.isBluetoothScoOn = true

                    mediaRecorder.start()

                    unregisterReceiver(this)
                }
            }
        }, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))

        return true
    }

    private fun startDevice() {
        mStreamAudioPlayer.init()
        startPlayback()
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
            initialize()
        }
        PermissionManager.requestPermissions(this)
    }

    private fun initialize() {
        mStreamAudioRecorder = StreamAudioRecorder.getInstance()
        mStreamAudioPlayer = StreamAudioPlayer.getInstance()

        initUI()
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
