package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerGestureHelperTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPlayerGestureOverlayRender() {
        composeTestRule.setContent {
            PlayerGestureOverlay(
                text = "Hız: 2.0x",
                icon = Icons.Rounded.VolumeUp
            )
        }

        composeTestRule.onNodeWithText("Hız: 2.0x").assertIsDisplayed()
    }

    @Test
    fun testVolumeProgressBarRender() {
        composeTestRule.setContent {
            VolumeProgressBar(volume = 0.45f)
        }

        // 0.45 should render as "45%"
        composeTestRule.onNodeWithText("45%").assertIsDisplayed()
    }

    @Test
    fun testBrightnessProgressBarRender() {
        composeTestRule.setContent {
            BrightnessProgressBar(brightness = 0.75f)
        }

        // 0.75 should render as "75%"
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }
}
