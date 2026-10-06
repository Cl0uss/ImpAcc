package com.example.impacc.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import com.example.impacc.overlay.FrameEditor
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.overlay.ScreenOverlayManager

class ScreenControlAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: ScreenOverlayManager

    private var editingApplication: ScreenApplication? = null
    private var editor: FrameEditor? = null
    private val handler = Handler(Looper.getMainLooper())
    private val launchTimeout = Runnable {
        if (editingApplication != null && editor == null) {
            cancelEditing()
            Toast.makeText(this, "Приложение не открылось. Попробуйте ещё раз.", Toast.LENGTH_LONG).show()
        }
    }

    fun beginEditing(application: ScreenApplication) {
        cancelEditing()
        removeCurrentOverlay()
        editingApplication = application
        handler.postDelayed(launchTimeout, 15000)
    }

    fun cancelEditing() {
        handler.removeCallbacks(launchTimeout)
        editingApplication = null
        editor?.let { view ->
            if (view.isAttachedToWindow) getSystemService(WindowManager::class.java).removeView(view)
        }
        editor = null
    }

    private fun showEditor(application: ScreenApplication) {
        if (editor != null) return
        removeCurrentOverlay()
        val view = FrameEditor(this, application, onSave = { updated ->
            ScreenApplicationStorage.updateApplication(this, updated)
            cancelEditing()
            updateOverlayFromWindows()
            Toast.makeText(this, "Рамки сохранены", Toast.LENGTH_SHORT).show()
        }, onCancel = {
            cancelEditing()
            updateOverlayFromWindows()
        })
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }
        editor = view
        try {
            getSystemService(WindowManager::class.java).addView(view, params)
            handler.removeCallbacks(launchTimeout)
        } catch (_: RuntimeException) {
            cancelEditing()
            Toast.makeText(this, "Не удалось показать рамки", Toast.LENGTH_LONG).show()
        }
    }

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
                        cancelEditing()
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

        connectedInstance = this
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
            if (editor != null) cancelEditing()
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

        editingApplication?.let { application ->
            val foreground = applicationWindows.firstOrNull { it.isFocused }
                ?: applicationWindows.firstOrNull { it.isActive }
            val foregroundPackage = foreground?.let { getPackageName(it) }
            if (foregroundPackage == application.packageName) {
                showEditor(application)
            } else if (editor != null && foregroundPackage != null) {
                cancelEditing()
            }
            if (editingApplication != null) return
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cancelEditing()
        removeCurrentOverlay()
        updateOverlayFromWindows()
    }

    override fun onInterrupt() {
        cancelEditing()
        removeCurrentOverlay()
    }

    override fun onDestroy() {
        connectedInstance = null
        cancelEditing()

        unregisterScreenReceiver()

        removeCurrentOverlay()

        super.onDestroy()
    }

    companion object {
        var connectedInstance: ScreenControlAccessibilityService? = null
            private set


        private const val SYSTEM_UI_PACKAGE =
            "com.android.systemui"
    }
}   