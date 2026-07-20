package com.kitsugi.animelist.ui.utils

import android.view.KeyEvent
import com.kitsugi.animelist.ui.tv.input.LongPressKeyTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TvLongPressKeyTrackerTest {

    @Test
    fun `handle returns false when key is not long press key`() {
        val tracker = LongPressKeyTracker(timeoutMillis = 500L)
        val event = mock<KeyEvent> {
            on { keyCode } doReturn KeyEvent.KEYCODE_BACK
        }
        val result = tracker.handle(
            event = event,
            isLongPressKey = { it == KeyEvent.KEYCODE_DPAD_CENTER },
            onLongPress = {}
        )
        assertFalse(result)
    }

    @Test
    fun `handle ACTION_DOWN returns false on first click without repeat or timeout`() {
        val tracker = LongPressKeyTracker(timeoutMillis = 500L)
        val event = mock<KeyEvent> {
            on { keyCode } doReturn KeyEvent.KEYCODE_DPAD_CENTER
            on { action } doReturn KeyEvent.ACTION_DOWN
            on { repeatCount } doReturn 0
            on { isLongPress } doReturn false
            on { eventTime } doReturn 1000L
            on { downTime } doReturn 1000L
        }
        var called = false
        val result = tracker.handle(
            event = event,
            isLongPressKey = { it == KeyEvent.KEYCODE_DPAD_CENTER },
            onLongPress = { called = true }
        )
        assertFalse(result)
        assertFalse(called)
    }

    @Test
    fun `handle ACTION_DOWN returns true and triggers callback when event is long press`() {
        val tracker = LongPressKeyTracker(timeoutMillis = 500L)
        val event = mock<KeyEvent> {
            on { keyCode } doReturn KeyEvent.KEYCODE_DPAD_CENTER
            on { action } doReturn KeyEvent.ACTION_DOWN
            on { repeatCount } doReturn 1
            on { isLongPress } doReturn true
            on { eventTime } doReturn 1000L
            on { downTime } doReturn 1000L
        }
        var called = false
        val result = tracker.handle(
            event = event,
            isLongPressKey = { it == KeyEvent.KEYCODE_DPAD_CENTER },
            onLongPress = { called = true }
        )
        assertTrue(result)
        assertTrue(called)
    }
}
