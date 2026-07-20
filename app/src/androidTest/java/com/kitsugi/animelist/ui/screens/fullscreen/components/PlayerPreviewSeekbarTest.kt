package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerPreviewSeekbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRenderSeekbar() {
        var seekCalled = false
        var targetPosition = 0L

        composeTestRule.setContent {
            PlayerPreviewSeekbar(
                positionMs = 10000L,
                durationMs = 60000L,
                onSeek = { pos ->
                    seekCalled = true
                    targetPosition = pos
                }
            )
        }

        // 10000 ms is 0:10, 60000 ms is 1:00
        composeTestRule.onNodeWithText("0:10").assertIsDisplayed()
        composeTestRule.onNodeWithText("1:00").assertIsDisplayed()
    }
}
