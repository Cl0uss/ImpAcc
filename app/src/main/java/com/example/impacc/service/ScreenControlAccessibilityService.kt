package com.example.impacc.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.overlay.ScreenOverlayManager

class ScreenControlAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: ScreenOverlayManager

    private var displayedApplication: ScreenApplication? = null

    private var screenReceiverRegistered = false

    private val screenReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                when (intent?.action) {

                    Intent.ACTION_SCREEN_OFF -> {
                        removeCurrentOverlay()
                    }

                    Intent.ACTION_SCREEN_ON -> {
                        updateOverlayFromWindows()
                    }
                }
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        overlayManager =
            ScreenOverlayManager(this)

        val info = serviceInfo

        info.flags =
            info.flags or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

        serviceInfo = info

        registerScreenReceiver()

        updateOverlayFromWindows()
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null) {
            return
        }

        when (event.eventType) {

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                updateOverlayFromWindows()
            }
        }
    }

    private fun updateOverlayFromWindows() {
        if (!::overlayManager.isInitialized) {
            return
        }

        /*
         * Проверяем SystemUI.
         *
         * Это шторка уведомлений,
         * Quick Settings и другие системные панели.
         *
         * Если SystemUI активно или имеет фокус,
         * временно убираем overlay.
         */
        val systemUiIsOpen =
            windows.any { window ->

                getPackageName(window) ==
                        SYSTEM_UI_PACKAGE &&
                        (
                                window.isFocused ||
                                        window.isActive
                                )
            }

        if (systemUiIsOpen) {
            removeCurrentOverlay()
            return
        }

        /*
         * Берём только обычные application windows.
         */
        val applicationWindows =
            windows.filter { window ->

                window.type ==
                        AccessibilityWindowInfo.TYPE_APPLICATION
            }

        val currentApplication =
            displayedApplication

        /*
         * Если текущее приложение всё ещё реально
         * active или focused, overlay оставляем.
         *
         * Это важно для Samsung gesture navigation:
         *
         * TikTok
         * -> начал тянуть Home
         * -> отменил жест
         * -> вернулся обратно.
         *
         * Overlay при этом не исчезнет.
         */
        if (
            currentApplication != null &&
            applicationWindows.any { window ->

                getPackageName(window) ==
                        currentApplication.packageName &&
                        (
                                window.isFocused ||
                                        window.isActive
                                )
            }
        ) {
            return
        }

        /*
         * Сначала пытаемся найти focused application window.
         */
        val focusedPackageName =
            applicationWindows
                .firstOrNull { window ->
                    window.isFocused
                }
                ?.let { window ->
                    getPackageName(window)
                }

        /*
         * Если focused почему-то нет,
         * используем active window.
         */
        val activePackageName =
            applicationWindows
                .firstOrNull { window ->
                    window.isActive
                }
                ?.let { window ->
                    getPackageName(window)
                }

        val foregroundPackageName =
            focusedPackageName
                ?: activePackageName
                ?: return

        /*
         * Проверяем, находится ли текущее приложение
         * в списке Screen Control.
         */
        val selectedApplication =
            ScreenApplicationStorage.getApplication(
                context = this,
                packageName = foregroundPackageName
            )

        if (selectedApplication != null) {

            showApplicationOverlay(
                selectedApplication
            )

        } else {

            removeCurrentOverlay()
        }
    }

    private fun getPackageName(
        window: AccessibilityWindowInfo
    ): String? {

        return window.root
            ?.packageName
            ?.toString()
            ?.takeIf { packageName ->
                packageName.isNotBlank()
            }
    }

    private fun showApplicationOverlay(
        application: ScreenApplication
    ) {
        if (!::overlayManager.isInitialized) {
            return
        }

        /*
         * Overlay уже настроен для этого приложения.
         */
        if (displayedApplication == application) {
            return
        }

        overlayManager.showOverlay(
            application
        )

        displayedApplication =
            application
    }

    private fun removeCurrentOverlay() {
        if (::overlayManager.isInitialized) {
            overlayManager.removeOverlay()
        }

        displayedApplication = null
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) {
            return
        }

        val filter =
            IntentFilter().apply {

                addAction(
                    Intent.ACTION_SCREEN_OFF
                )

                addAction(
                    Intent.ACTION_SCREEN_ON
                )
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                screenReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            registerReceiver(
                screenReceiver,
                filter
            )
        }

        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) {
            return
        }

        unregisterReceiver(
            screenReceiver
        )

        screenReceiverRegistered = false
    }

    override fun onInterrupt() {
        removeCurrentOverlay()
    }

    override fun onDestroy() {

        unregisterScreenReceiver()

        removeCurrentOverlay()

        super.onDestroy()
    }

    companion object {

        private const val SYSTEM_UI_PACKAGE =
            "com.android.systemui"
    }
}   