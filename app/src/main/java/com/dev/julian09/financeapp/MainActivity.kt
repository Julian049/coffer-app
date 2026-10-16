package com.dev.julian09.financeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dev.julian09.financeapp.legacy.ui.main.MainScreen
import com.dev.julian09.financeapp.legacy.ui.navigation.Screen
import com.dev.julian09.financeapp.legacy.ui.register.RegisterTransaction
import com.dev.julian09.financeapp.ui.theme.FinanceAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceAppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Main.route,
                ) {
                    composable(Screen.Main.route) {
                        MainScreen(
                            onAddNewTransaction = {
                                navController.navigate(Screen.Register.route)
                            },
                        )
                    }

                    composable(Screen.Register.route) {
                        RegisterTransaction(
                            onBack = {
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }
    }
}
