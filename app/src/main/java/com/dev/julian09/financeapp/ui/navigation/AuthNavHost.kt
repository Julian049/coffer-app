package com.dev.julian09.financeapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dev.julian09.financeapp.ui.feature.auth.LoginScreen
import com.dev.julian09.financeapp.ui.feature.auth.RegisterScreen
import com.dev.julian09.financeapp.ui.theme.CofferTheme

/** Signed out or expired: sign in or create an account. [expiredUser] pre-fills who was signed in. */
@Composable
fun AuthNavHost(expiredUser: String?) {
    val navController = rememberNavController()
    Scaffold(containerColor = CofferTheme.colors.background) { padding ->
        NavHost(navController, startDestination = LoginRoute, modifier = Modifier.padding(padding)) {
            composable<LoginRoute> { LoginScreen(onRegister = { navController.navigate(RegisterRoute) }, expiredUser = expiredUser) }
            composable<RegisterRoute> { RegisterScreen(onBack = navController::popBackStack) }
        }
    }
}
