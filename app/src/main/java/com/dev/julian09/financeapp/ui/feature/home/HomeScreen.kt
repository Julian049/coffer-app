package com.dev.julian09.financeapp.ui.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dev.julian09.financeapp.domain.model.SyncStatus
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.components.SyncBadge

/** Empty container until Sprint 4. */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    syncStatus: SyncStatus? = null,
) {
    Column(modifier.fillMaxSize()) {
        AppHeader(
            title = "Inicio",
            actions = {
                syncStatus?.let { SyncBadge(it) }
                Avatar(onClick = onOpenSettings, initial = avatarInitial)
            },
        )
        EmptyState(title = "Tu inicio está en camino", message = "Pronto verás aquí tu saldo, tus cuentas y los pagos próximos.")
    }
}
