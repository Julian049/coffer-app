package com.dev.julian09.financeapp.ui.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.Avatar
import com.dev.julian09.financeapp.ui.components.EmptyState
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate

@Composable
fun HistoryScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    HistoryContent(days, onOpenSettings, modifier, avatarInitial)
}

@Composable
fun HistoryContent(
    days: List<HistoryDay>?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitial: Char? = null,
    today: LocalDate = LocalDate.now(),
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Historial", actions = { Avatar(onClick = onOpenSettings, initial = avatarInitial) })
        when {
            days == null -> Unit
            days.isEmpty() ->
                EmptyState(
                    title = "Aún no hay movimientos",
                    message = "Tus gastos, ingresos y transferencias aparecerán aquí.",
                )
            else ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(days, key = { it.date.toString() }) { day -> DayGroup(day, today) }
                }
        }
    }
}
