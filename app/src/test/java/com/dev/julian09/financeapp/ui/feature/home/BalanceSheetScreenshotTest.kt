package com.dev.julian09.financeapp.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.CustomNetBalanceByCardTest
import com.dev.julian09.financeapp.domain.model.NetBalanceMode
import com.dev.julian09.financeapp.domain.model.NetChoices
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 2.5 (#253): Detalle del saldo · Personalizado, with a card by its debt and its installment purchases unfolded. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class BalanceSheetScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val data = CustomNetBalanceByCardTest()

    @Test
    fun customLight() = capture("balance_custom_light", false, NetChoices(excludedPurchases = setOf(data.trip)))

    @Test
    fun customPeriodDark() = capture("balance_custom_period_dark", true, NetChoices(periodCards = setOf(data.visaId)))

    private fun capture(
        name: String,
        dark: Boolean,
        choices: NetChoices,
    ) {
        compose.setContent {
            CofferTheme(darkTheme = dark) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(CofferTheme.colors.background)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BalanceViews(
                        data.dashboard(choices),
                        BalanceView.NET,
                        NetBalanceMode.CUSTOM,
                        onView = {},
                        onMode = {},
                        onIncludeInNet = { _, _ -> },
                        onPeriodOnly = { _, _ -> },
                        onPurchaseIncluded = { _, _ -> },
                    )
                }
            }
        }
        compose.onNodeWithText("Compras a cuotas", substring = true).performClick()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
