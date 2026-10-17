package com.dev.julian09.financeapp.ui.feature.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState

/** Empty container until Sprint 9. */
@Composable
fun ReportsScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Reportes", actions = { Avatar(onClick = onOpenSettings) })
        EmptyState(title = "Sin reportes todavía", message = "Cuando registres movimientos verás en qué se va tu dinero.")
    }
}
