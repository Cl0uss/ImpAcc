package com.example.impacc.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.overlay.ScreenOverlayManager

class ScreenControlAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: ScreenOverlayManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeApplication: ScreenApplication? = null
    private var lastRealPackageName: String? = null

    private val refreshOverlayRunnable = Runnable {
        activeApplication?.let { application ->
            if (::overlayManager.isInitialized) {
                overlayManager.showOverlay(application)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        overlayManager = ScreenOverlayManager(this)
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null) {
            return
        }

        if (
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val eventPackageName = event.packageName
            ?.toString()
            ?.takeIf { packageName ->
                packageName.isNotBlank()
            }
            ?: return

        if (isTemporarySystemPackage(eventPackageName)) {
            restoreOverlayAfterSystemWindow()
            return
        }

        if (eventPackageName == packageName) {
            lastRealPackageName = eventPackageName
            activeApplication = null
            removeOverlay()
            return
        }

        val selectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = eventPackageName
            )

        lastRealPackageName = eventPackageName

        if (selectedApplication != null) {
            activeApplication = selectedApplication

            if (::overlayManager.isInitialized) {
                overlayManager.showOverlay(selectedApplication)
            }
        } else {
            activeApplication = null
            removeOverlay()
        }
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacksAndMessages(null)
        removeOverlay()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        removeOverlay()

        super.onDestroy()
    }

    private fun restoreOverlayAfterSystemWindow() {
        mainHandler.removeCallbacks(refreshOverlayRunnable)

        mainHandler.postDelayed(
            refreshOverlayRunnable,
            OVERLAY_RESTORE_DELAY_MILLISECONDS
        )
    }

    private fun removeOverlay() {
        mainHandler.removeCallbacks(refreshOverlayRunnable)

        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }
    }

    private fun isTemporarySystemPackage(
        packageName: String
    ): Boolean {
        return packageName in TEMPORARY_SYSTEM_PACKAGES
    }

    companion object {

        private const val OVERLAY_RESTORE_DELAY_MILLISECONDS =
            250L

        private val TEMPORARY_SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.samsung.android.systemui",
            "com.samsung.android.app.soundassistant",
            "com.samsung.android.app.cocktailbarservice"
        )
    }
}