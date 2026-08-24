package com.example.impacc.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.overlay.ScreenOverlayManager

class ScreenControlAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: ScreenOverlayManager
    private lateinit var foregroundApplicationDetector: ForegroundApplicationDetector
    private lateinit var powerManager: PowerManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private var displayedApplication: ScreenApplication? = null

    private var differentApplicationChecks = 0

    private val foregroundCheckRunnable = object : Runnable {

        override fun run() {
            checkForegroundApplication()

            if (displayedApplication != null) {
                mainHandler.postDelayed(
                    this,
                    FOREGROUND_CHECK_INTERVAL_MILLISECONDS
                )
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        overlayManager = ScreenOverlayManager(this)

        foregroundApplicationDetector =
            ForegroundApplicationDetector(this)

        powerManager =
            getSystemService(PowerManager::class.java)
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null) {
            return
        }

        val packageName =
            event.packageName
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: return

        /*
         * SystemUI появляется, например, когда пользователь
         * открывает шторку уведомлений.
         *
         * Мы не считаем это настоящим выходом из TikTok.
         */
        if (packageName == SYSTEM_UI_PACKAGE) {
            return
        }

        val selectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = packageName
            )

        if (selectedApplication != null) {
            showApplicationOverlay(selectedApplication)
        }
    }

    private fun checkForegroundApplication() {
        if (
            !::overlayManager.isInitialized ||
            !::foregroundApplicationDetector.isInitialized
        ) {
            return
        }

        val currentApplication =
            displayedApplication
                ?: return

        /*
         * Если экран телефона выключен,
         * overlay больше не нужен.
         */
        if (
            ::powerManager.isInitialized &&
            !powerManager.isInteractive
        ) {
            removeCurrentOverlay()
            return
        }

        val foregroundPackageName =
            foregroundApplicationDetector
                .getForegroundPackageName()
                ?: return

        /*
         * SystemUI игнорируем.
         *
         * Например:
         * TikTok -> открыть notification shade.
         */
        if (foregroundPackageName == SYSTEM_UI_PACKAGE) {
            differentApplicationChecks = 0
            return
        }

        /*
         * Всё ещё находимся в том же приложении.
         */
        if (
            foregroundPackageName ==
            currentApplication.packageName
        ) {
            differentApplicationChecks = 0
            return
        }

        /*
         * Возможно, пользователь перешёл сразу
         * в другое приложение, которое тоже находится
         * в списке Screen Control.
         */
        val newSelectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = foregroundPackageName
            )

        if (newSelectedApplication != null) {
            differentApplicationChecks = 0
            showApplicationOverlay(
                newSelectedApplication
            )
            return
        }

        /*
         * Текущее приложение больше не foreground.
         *
         * Требуем несколько последовательных подтверждений,
         * чтобы случайное системное событие не снимало overlay.
         */
        differentApplicationChecks++

        if (
            differentApplicationChecks >=
            REQUIRED_DIFFERENT_APPLICATION_CHECKS
        ) {
            removeCurrentOverlay()
        }
    }

    private fun showApplicationOverlay(
        application: ScreenApplication
    ) {
        if (!::overlayManager.isInitialized) {
            return
        }

        if (displayedApplication == application) {
            return
        }

        overlayManager.showOverlay(
            application
        )

        displayedApplication = application
        differentApplicationChecks = 0

        startForegroundWatchdog()
    }

    private fun startForegroundWatchdog() {
        mainHandler.removeCallbacks(
            foregroundCheckRunnable
        )

        mainHandler.post(
            foregroundCheckRunnable
        )
    }

    private fun stopForegroundWatchdog() {
        mainHandler.removeCallbacks(
            foregroundCheckRunnable
        )
    }

    private fun removeCurrentOverlay() {
        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }

        displayedApplication = null
        differentApplicationChecks = 0

        stopForegroundWatchdog()
    }

    override fun onInterrupt() {
        stopMonitoring()
    }

    override fun onDestroy() {
        stopMonitoring()

        super.onDestroy()
    }

    private fun stopMonitoring() {
        stopForegroundWatchdog()

        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }

        displayedApplication = null
        differentApplicationChecks = 0
    }

    companion object {

        /*
         * 100 ms = 10 проверок в секунду.
         */
        private const val FOREGROUND_CHECK_INTERVAL_MILLISECONDS =
            100L

        /*
         * Две проверки подряд.
         *
         * Поэтому overlay снимается примерно
         * через 100-200 мс после выхода.
         */
        private const val REQUIRED_DIFFERENT_APPLICATION_CHECKS =
            2

        private const val SYSTEM_UI_PACKAGE =
            "com.android.systemui"
    }
}