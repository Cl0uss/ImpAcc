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

    private lateinit var foregroundApplicationDetector:
            ForegroundApplicationDetector

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var displayedApplication:
            ScreenApplication? = null

    private var lastAccessibilityPackageName:
            String? = null

    private var pendingExitPackageName:
            String? = null

    private var pendingExitStartedAt:
            Long = 0L

    private val foregroundCheckRunnable =
        object : Runnable {

            override fun run() {
                checkForegroundApplication()

                mainHandler.postDelayed(
                    this,
                    FOREGROUND_CHECK_INTERVAL_MILLISECONDS
                )
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        overlayManager =
            ScreenOverlayManager(this)

        foregroundApplicationDetector =
            ForegroundApplicationDetector(this)

        mainHandler.removeCallbacks(
            foregroundCheckRunnable
        )

        mainHandler.post(
            foregroundCheckRunnable
        )
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null) {
            return
        }

        val eventPackageName =
            event.packageName
                ?.toString()
                ?.takeIf { packageName ->
                    packageName.isNotBlank()
                }
                ?: return

        lastAccessibilityPackageName =
            eventPackageName

        val selectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = eventPackageName
            )

        if (selectedApplication != null) {
            cancelPendingExit()

            showApplicationOverlay(
                selectedApplication
            )
        }

        requestImmediateForegroundCheck()
    }

    override fun onInterrupt() {
        stopMonitoring()
    }

    override fun onDestroy() {
        stopMonitoring()

        super.onDestroy()
    }

    private fun checkForegroundApplication() {
        if (
            !::overlayManager.isInitialized ||
            !::foregroundApplicationDetector.isInitialized
        ) {
            return
        }

        val usagePackageName =
            foregroundApplicationDetector
                .getForegroundPackageName()
                ?: return

        val selectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = usagePackageName
            )

        if (selectedApplication != null) {
            cancelPendingExit()

            showApplicationOverlay(
                selectedApplication
            )

            return
        }

        val currentApplication =
            displayedApplication
                ?: return

        val accessibilityPackageName =
            lastAccessibilityPackageName
                ?: return

        /*
         * Do not remove the overlay unless UsageStats and Accessibility
         * confirm the same foreground package.
         */
        if (
            usagePackageName != accessibilityPackageName
        ) {
            cancelPendingExit()
            return
        }

        /*
         * The selected application may still be visible while Android
         * temporarily activates the launcher during a navigation gesture.
         */
        if (
            accessibilityPackageName ==
            currentApplication.packageName
        ) {
            cancelPendingExit()
            return
        }

        scheduleConfirmedExit(
            packageName = usagePackageName
        )
    }

    private fun showApplicationOverlay(
        application: ScreenApplication
    ) {
        if (!::overlayManager.isInitialized) {
            return
        }

        val overlayAlreadyMatches =
            displayedApplication == application

        if (overlayAlreadyMatches) {
            return
        }

        overlayManager.showOverlay(
            application
        )

        displayedApplication =
            application
    }

    private fun scheduleConfirmedExit(
        packageName: String
    ) {
        val currentTime =
            System.currentTimeMillis()

        if (
            pendingExitPackageName != packageName
        ) {
            pendingExitPackageName =
                packageName

            pendingExitStartedAt =
                currentTime

            return
        }

        val pendingDuration =
            currentTime - pendingExitStartedAt

        if (
            pendingDuration <
            EXIT_CONFIRMATION_DELAY_MILLISECONDS
        ) {
            return
        }

        /*
         * Check both sources again before removing the overlay.
         */
        val latestUsagePackageName =
            foregroundApplicationDetector
                .getForegroundPackageName()
                ?: return

        val latestAccessibilityPackageName =
            lastAccessibilityPackageName
                ?: return

        if (
            latestUsagePackageName != packageName ||
            latestAccessibilityPackageName != packageName
        ) {
            cancelPendingExit()
            return
        }

        removeCurrentOverlay()
    }

    private fun cancelPendingExit() {
        pendingExitPackageName = null
        pendingExitStartedAt = 0L
    }

    private fun removeCurrentOverlay() {
        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }

        displayedApplication = null

        cancelPendingExit()
    }

    private fun requestImmediateForegroundCheck() {
        mainHandler.removeCallbacks(
            foregroundCheckRunnable
        )

        mainHandler.post(
            foregroundCheckRunnable
        )
    }

    private fun stopMonitoring() {
        mainHandler.removeCallbacksAndMessages(null)

        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }

        displayedApplication = null
        lastAccessibilityPackageName = null

        cancelPendingExit()
    }

    companion object {

        private const val FOREGROUND_CHECK_INTERVAL_MILLISECONDS =
            400L

        private const val EXIT_CONFIRMATION_DELAY_MILLISECONDS =
            800L
    }
}