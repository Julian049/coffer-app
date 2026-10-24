package com.dev.julian09.financeapp.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** S15-QA04: where a user without a session starts. */
class AuthNavHostTest {
    @Test
    fun withoutASessionTheWelcomeComesFirst() = assertEquals(WelcomeRoute, authStart(expiredUser = null))

    @Test
    fun anExpiredSessionGoesStraightToLogin() = assertEquals(LoginRoute, authStart(expiredUser = "julian"))
}
