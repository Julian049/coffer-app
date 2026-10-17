package com.dev.julian09.financeapp.ui.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState

/** Empty container until Sprint 5. */
@Composable
fun HistoryScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Historial", actions = { Avatar(onClick = onOpenSettings) })
        EmptyState(title = "Aún no hay movimientos", message = "Tus gastos, ingresos y transferencias aparecerán aquí.")
    }
}
