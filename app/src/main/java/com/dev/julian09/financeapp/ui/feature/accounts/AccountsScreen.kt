package com.dev.julian09.financeapp.ui.feature.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState

/** Empty container until Sprint 4. */
@Composable
fun AccountsScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
) {
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Cuentas", actions = { Avatar(onClick = onOpenSettings, initial = avatarInitial) })
        EmptyState(title = "Aún no tienes cuentas", message = "Aquí vas a crear y administrar tus cuentas, tarjetas y préstamos.")
    }
}
