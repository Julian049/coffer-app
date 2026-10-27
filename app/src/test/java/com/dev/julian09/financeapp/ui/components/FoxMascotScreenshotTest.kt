package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The eight states of the animated fox, assembled from their layers (mascot-animated README §6: same as `static/`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class FoxMascotScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyState() {
        compose.setContent {
            CofferTheme(darkTheme = false) {
                Column(Modifier.background(CofferTheme.colors.background)) {
                    FOX_LAYERS.keys.sorted().chunked(2).forEach { pair ->
                        Row { pair.forEach { FoxMascot(it, size = 190.dp) } }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/fox_states.png")
    }
}
