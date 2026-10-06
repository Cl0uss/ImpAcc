package com.example.impacc

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.impacc.data.ScreenApplication
import com.example.impacc.overlay.FrameEditor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameEditorTest {
    private val original = ScreenApplication("Test", "test.frames", 100, 120, 80, 90, 50)

    @Test fun dragAllEdgesAndSave() = onMain {
        var saved: ScreenApplication? = null
        val editor = editor(onSave = { saved = it })
        val borders = editor.getChildAt(0)
        drag(borders, 500f, 100f, 500f, 180f)
        drag(borders, 500f, 1480f, 500f, 1400f)
        drag(borders, 80f, 800f, 150f, 800f)
        drag(borders, 910f, 800f, 850f, 800f)
        button(editor, "ОК").performClick()
        assertEquals(original.copy(upperPart = 180, lowerPart = 200, leftPart = 150, rightPart = 150), saved)
    }

    @Test fun cancelDoesNotSave() = onMain {
        var saved = false
        var cancelled = false
        val editor = editor(onSave = { saved = true }, onCancel = { cancelled = true })
        drag(editor.getChildAt(0), 500f, 100f, 500f, 200f)
        button(editor, "Отмена").performClick()
        assertTrue(cancelled)
        assertFalse(saved)
    }

    @Test fun existingSizesArePreservedAndDraggingIsClamped() = onMain {
        var saved: ScreenApplication? = null
        val editor = editor(onSave = { saved = it })
        button(editor, "ОК").performClick()
        assertEquals(original, saved)
        drag(editor.getChildAt(0), 500f, 100f, 500f, -100f)
        drag(editor.getChildAt(0), 80f, 800f, 1400f, 800f)
        button(editor, "ОК").performClick()
        assertEquals(0, saved!!.upperPart)
        assertEquals(1000, saved!!.leftPart)
    }

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private fun editor(onSave: (ScreenApplication) -> Unit, onCancel: () -> Unit = {}): FrameEditor {
        return FrameEditor(InstrumentationRegistry.getInstrumentation().targetContext, original, onSave, onCancel).apply {
            measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY))
            layout(0, 0, 1000, 1600)
        }
    }

    private fun button(view: ViewGroup, text: String): Button {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is Button && child.text == text) return child
            if (child is ViewGroup) {
                try { return button(child, text) } catch (_: NoSuchElementException) { }
            }
        }
        throw NoSuchElementException(text)
    }

    private fun drag(view: View, x: Float, y: Float, endX: Float, endY: Float) {
        listOf(Triple(MotionEvent.ACTION_DOWN, x, y), Triple(MotionEvent.ACTION_MOVE, endX, endY),
            Triple(MotionEvent.ACTION_UP, endX, endY)).forEachIndexed { index, (action, px, py) ->
            val event = MotionEvent.obtain(0, index * 16L, action, px, py, 0)
            view.dispatchTouchEvent(event)
            event.recycle()
        }
    }
}
