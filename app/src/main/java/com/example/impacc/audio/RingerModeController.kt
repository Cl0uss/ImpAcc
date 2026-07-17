package com.example.impacc.audio

import android.content.Context
import android.media.AudioManager


class RingerModeController(
    context: Context
) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Stores the original phone ringer mode
    private var savedRingerMode: Int? = null

    // Indicates whether the app changed the ringer mode
    private var changedByApp = false


    fun mutePhone() {

        // Do not apply changes twice
        if (changedByApp) {
            return
        }

        val currentMode = audioManager.ringerMode

        // Phone is already silent
        if (currentMode == AudioManager.RINGER_MODE_SILENT) {
            return
        }

        // Save current mode before switching to silent
        savedRingerMode = currentMode

        // Switch phone to silent mode
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        // Remember that the app changed the mode
        changedByApp = true
    }

    fun restorePhone() {

        // Nothing to restore if the app did not change the mode
        if (!changedByApp) {
            return
        }

        savedRingerMode?.let { previousMode ->

            // Restore previous phone mode
            audioManager.ringerMode = previousMode
        }

        savedRingerMode = null
        changedByApp = false
    }


    fun isSilentMode(): Boolean {

        return audioManager.ringerMode ==
                AudioManager.RINGER_MODE_SILENT
    }
}