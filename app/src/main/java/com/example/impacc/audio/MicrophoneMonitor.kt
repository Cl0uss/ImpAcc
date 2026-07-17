package com.example.impacc.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration


class MicrophoneMonitor(
    context: Context,
    private val ringerModeController: RingerModeController
) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var isMonitoring = false

    private var microphoneActive = false


    private val recordingCallback =
        object : AudioManager.AudioRecordingCallback() {

            override fun onRecordingConfigChanged(
                configs: List<AudioRecordingConfiguration>
            ) {

                if (!isMonitoring) {
                    return
                }

                val active = configs.isNotEmpty()

                if (active && !microphoneActive) {

                    microphoneActive = true
                    ringerModeController.mutePhone()

                } else if (!active && microphoneActive) {

                    microphoneActive = false
                    ringerModeController.restorePhone()

                }
            }
        }


    fun start() {

        if (isMonitoring) {
            return
        }

        isMonitoring = true

        audioManager.registerAudioRecordingCallback(
            recordingCallback,
            null
        )
    }


    fun stop() {

        if (!isMonitoring) {
            return
        }

        isMonitoring = false

        audioManager.unregisterAudioRecordingCallback(
            recordingCallback
        )

        if (microphoneActive) {

            microphoneActive = false
            ringerModeController.restorePhone()

        }
    }
}