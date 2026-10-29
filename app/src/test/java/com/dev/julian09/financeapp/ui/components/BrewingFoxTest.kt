package com.dev.julian09.financeapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class BrewingFoxTest {
    @Test
    fun `the coffee rises with the download from the bottom to the rim`() {
        assertEquals(884f, coffeeLevel(0f), 0.01f)
        assertEquals(803f, coffeeLevel(0.5f), 0.01f)
        assertEquals(722f, coffeeLevel(1f), 0.01f)
        assertEquals(722f, coffeeLevel(1.4f), 0.01f)
    }

    @Test
    fun `the crema narrows with the glass`() {
        assertEquals(1f - 20f / 144f * (722f - 712f) / 150f, cremaScaleX(722f), 0.001f)
    }

    @Test
    fun `keyframes interpolate and hold the last value`() {
        assertEquals(2f, keys(0.5f, 0f to 0f, 1f to 4f), 0.001f)
        assertEquals(4f, keys(2f, 0f to 0f, 1f to 4f), 0.001f)
    }
}
