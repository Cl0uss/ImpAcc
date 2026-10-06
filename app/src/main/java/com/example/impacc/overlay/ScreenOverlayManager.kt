package com.example.impacc.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.impacc.data.ScreenApplication
import kotlin.math.roundToInt

class ScreenOverlayManager(
    private val service: AccessibilityService
) {

    private val windowManager =
        service.getSystemService(WindowManager::class.java)

    private val overlayViews = mutableListOf<View>()

    private var currentPackageName: String? = null
    private var currentConfiguration: ScreenApplication? = null

    fun showOverlay(application: ScreenApplication) {
        val normalizedApplication = application.copy(
            upperPart = application.upperPart.coerceAtLeast(0),
            lowerPart = application.lowerPart.coerceAtLeast(0),
            leftPart = application.leftPart.coerceAtLeast(0),
            rightPart = application.rightPart.coerceAtLeast(0),
            transparency = application.transparency.coerceIn(0, 100)
        )

        if (
            currentPackageName == normalizedApplication.packageName &&
            currentConfiguration == normalizedApplication &&
            overlayViews.isNotEmpty()
        ) {
            return
        }

        removeOverlay()

        if (
            normalizedApplication.transparency >= 100 ||
            (
                    normalizedApplication.upperPart == 0 &&
                            normalizedApplication.lowerPart == 0 &&
                            normalizedApplication.leftPart == 0 &&
                            normalizedApplication.rightPart == 0
                    )
        ) {
            return
        }

        currentPackageName = normalizedApplication.packageName
        currentConfiguration = normalizedApplication

        val displayMetrics = windowManager.currentWindowMetrics.bounds
        val screenWidth = displayMetrics.width()
        val screenHeight = displayMetrics.height()

        val upperHeight = normalizedApplication.upperPart
            .coerceAtMost(screenHeight)

        val lowerHeight = normalizedApplication.lowerPart
            .coerceAtMost(screenHeight)

        val leftWidth = normalizedApplication.leftPart
            .coerceAtMost(screenWidth)

        val rightWidth = normalizedApplication.rightPart
            .coerceAtMost(screenWidth)

        val overlayColor = createOverlayColor(
            transparency = normalizedApplication.transparency
        )

        if (upperHeight > 0) {
            addOverlayView(
                width = WindowManager.LayoutParams.MATCH_PARENT,
                height = upperHeight,
                gravity = Gravity.TOP,
                color = overlayColor
            )
        }

        if (lowerHeight > 0) {
            addOverlayView(
                width = WindowManager.LayoutParams.MATCH_PARENT,
                height = lowerHeight,
                gravity = Gravity.BOTTOM,
                color = overlayColor
            )
        }

        if (leftWidth > 0) {
            addOverlayView(
                width = leftWidth,
                height = WindowManager.LayoutParams.MATCH_PARENT,
                gravity = Gravity.LEFT,
                color = overlayColor
            )
        }

        if (rightWidth > 0) {
            addOverlayView(
                width = rightWidth,
                height = WindowManager.LayoutParams.MATCH_PARENT,
                gravity = Gravity.RIGHT,
                color = overlayColor
            )
        }
    }

    fun removeOverlay() {
        overlayViews.toList().forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // The view may already have been removed by Android.
            }
        }

        overlayViews.clear()
        currentPackageName = null
        currentConfiguration = null
    }

    private fun addOverlayView(
        width: Int,
        height: Int,
        gravity: Int,
        color: Int
    ) {
        val overlayView = View(service).apply {
            setBackgroundColor(color)
            importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val layoutParams = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }

        try {
            windowManager.addView(
                overlayView,
                layoutParams
            )

            overlayViews.add(overlayView)
        } catch (_: Exception) {
            // Ignore overlay creation failures and keep the service alive.
        }
    }

    private fun createOverlayColor(
        transparency: Int
    ): Int {
        val opacityFraction =
            1f - transparency.coerceIn(0, 100) / 100f

        val alpha = (opacityFraction * 255f)
            .roundToInt()
            .coerceIn(0, 255)

        return Color.argb(
            alpha,
            0,
            0,
            0
        )
    }
}