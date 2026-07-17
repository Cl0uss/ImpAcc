package com.example.impacc.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.overlay.ScreenOverlayManager

class ScreenControlAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: ScreenOverlayManager

    private var lastForegroundPackage: String? = null

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
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val foregroundPackage =
            event.packageName?.toString()
                ?: return

        if (foregroundPackage == packageName) {
            lastForegroundPackage = foregroundPackage

            if (::overlayManager.isInitialized) {
                overlayManager.removeOverlay()
            }

            return
        }

        val selectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = foregroundPackage
            )

        if (selectedApplication == null) {
            lastForegroundPackage = foregroundPackage

            if (::overlayManager.isInitialized) {
                overlayManager.removeOverlay()
            }

            return
        }

        lastForegroundPackage = foregroundPackage

        if (::overlayManager.isInitialized) {
            overlayManager.showOverlay(
                application = selectedApplication
            )
        }
    }

    override fun onInterrupt() {
        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }
    }

    override fun onDestroy() {
        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }

        super.onDestroy()
    }
}