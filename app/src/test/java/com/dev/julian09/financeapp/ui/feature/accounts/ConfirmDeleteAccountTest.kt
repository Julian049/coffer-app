package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.AccountFlows
import com.dev.julian09.financeapp.domain.model.AccountImpact
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** AT-HU13-2 / REQ-CTA-05: the impact shows and the button waits for the exact name. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConfirmDeleteAccountTest {
    @get:Rule
    val compose = createComposeRule()

    private val account =
        AccountBalance(Account(UUID.randomUUID(), "Bancolombia Débito", AccountType.SAVINGS, Money.ofPesos(850_000)), AccountFlows(), 38)

    @Test
    fun theButtonEnablesOnlyWithTheName() {
        var sheet by mutableStateOf(AccountSheet.Confirm(account, AccountImpact(38, Money.ofPesos(850_000), null, null)))
        var deleted = 0
        compose.setContent {
            CofferTheme { ConfirmSheet(sheet, onDismiss = {}, onType = { sheet = sheet.copy(typed = it) }, onConfirm = { deleted++ }) }
        }

        compose.onNodeWithText("38").assertExists()
        compose.onNodeWithText("−$850.000").assertExists()
        // The sheet title says the same words: the button is the clickable one
        val button = compose.onNode(hasClickAction() and hasText("Eliminar cuenta y transacciones"))
        val field = compose.onNode(hasSetTextAction())

        button.assertIsNotEnabled()
        field.performTextInput("bancolombia")
        button.assertIsNotEnabled()
        field.performTextInput(" débito")
        button.assertIsEnabled()
        button.performClick()
        assertEquals(1, deleted)
    }
}
