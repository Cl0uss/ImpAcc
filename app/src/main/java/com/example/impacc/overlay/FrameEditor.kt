package com.example.impacc.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.impacc.data.ScreenApplication
import kotlin.math.abs
import kotlin.math.roundToInt

/** All coordinates use the same full-screen window space as the saved overlays. */
class FrameEditor(
    context: Context,
    application: ScreenApplication,
    onSave: (ScreenApplication) -> Unit,
    onCancel: () -> Unit
) : FrameLayout(context) {
    private val borders = Borders(context, application)

    init {
        addView(borders, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.argb(215, 25, 25, 25))
        }
        panel.addView(TextView(context).apply {
            text = "${application.name}\nТяните границы рамок"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        })
        val label = TextView(context).apply { setTextColor(Color.WHITE) }
        fun updateLabel() { label.text = "Прозрачность: ${borders.configuration.transparency}%" }
        updateLabel()
        panel.addView(label)
        panel.addView(SeekBar(context).apply {
            max = 100
            progress = application.transparency
            contentDescription = "Прозрачность рамок"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    borders.configuration = borders.configuration.copy(transparency = value)
                    borders.invalidate()
                    updateLabel()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        panel.addView(Button(context).apply {
            text = "ОК"
            setOnClickListener { onSave(borders.configuration) }
        })
        panel.addView(Button(context).apply {
            text = "Отмена"
            setOnClickListener { onCancel() }
        })
        addView(panel, LayoutParams((220 * resources.displayMetrics.density).roundToInt(), LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    private class Borders(context: Context, application: ScreenApplication) : View(context) {
        var configuration = application
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val density = resources.displayMetrics.density
        private var dragged = -1
        private var lastX = 0f
        private var lastY = 0f

        init { contentDescription = "Перетащите верхнюю, нижнюю, левую или правую границу рамки" }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            configuration = configuration.copy(
                upperPart = configuration.upperPart.coerceIn(0, h),
                lowerPart = configuration.lowerPart.coerceIn(0, h),
                leftPart = configuration.leftPart.coerceIn(0, w),
                rightPart = configuration.rightPart.coerceIn(0, w)
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val c = configuration
            val areas = listOf(
                RectF(0f, 0f, w, c.upperPart.toFloat()),
                RectF(0f, h - c.lowerPart, w, h),
                RectF(0f, 0f, c.leftPart.toFloat(), h),
                RectF(w - c.rightPart, 0f, w, h)
            )
            // Keep the application visible even when the saved mask is opaque.
            paint.color = Color.argb(((100 - c.transparency) * 1.3f).roundToInt(), 30, 110, 200)
            paint.style = Paint.Style.FILL
            areas.forEach { canvas.drawRect(it, paint) }
            paint.color = Color.CYAN
            paint.strokeWidth = 2 * density
            canvas.drawLine(0f, c.upperPart.toFloat(), w, c.upperPart.toFloat(), paint)
            canvas.drawLine(0f, h - c.lowerPart, w, h - c.lowerPart, paint)
            canvas.drawLine(c.leftPart.toFloat(), 0f, c.leftPart.toFloat(), h, paint)
            canvas.drawLine(w - c.rightPart, 0f, w - c.rightPart, h, paint)
            // Inset handles remain reachable when a border is reduced to zero.
            val inset = 26 * density
            fun handle(x: Float, y: Float, label: String) {
                val cx = x.coerceIn(inset, (w - inset).coerceAtLeast(inset))
                val cy = y.coerceIn(inset, (h - inset).coerceAtLeast(inset))
                paint.color = Color.rgb(25, 95, 150)
                canvas.drawCircle(cx, cy, 23 * density, paint)
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 18 * density
                canvas.drawText(label, cx, cy + 6 * density, paint)
            }
            handle(w / 2, c.upperPart.toFloat(), "↕")
            handle(w / 2, h - c.lowerPart, "↕")
            handle(c.leftPart.toFloat(), h / 2, "↔")
            handle(w - c.rightPart, h / 2, "↔")
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val c = configuration
                    val distances = listOf(abs(event.y - c.upperPart), abs(event.y - (height - c.lowerPart)),
                        abs(event.x - c.leftPart), abs(event.x - (width - c.rightPart)))
                    dragged = distances.indices.minByOrNull { distances[it] } ?: -1
                    if (dragged >= 0 && distances[dragged] > 52 * density) dragged = -1
                    lastX = event.x
                    lastY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.x - lastX).roundToInt()
                    val dy = (event.y - lastY).roundToInt()
                    val c = configuration
                    configuration = when (dragged) {
                        0 -> c.copy(upperPart = (c.upperPart + dy).coerceIn(0, height))
                        1 -> c.copy(lowerPart = (c.lowerPart - dy).coerceIn(0, height))
                        2 -> c.copy(leftPart = (c.leftPart + dx).coerceIn(0, width))
                        3 -> c.copy(rightPart = (c.rightPart - dx).coerceIn(0, width))
                        else -> c
                    }
                    lastX = event.x
                    lastY = event.y
                    invalidate()
                }
                MotionEvent.ACTION_UP -> { dragged = -1; performClick() }
                MotionEvent.ACTION_CANCEL -> dragged = -1
            }
            return true
        }

        override fun performClick(): Boolean { super.performClick(); return true }
    }
}
