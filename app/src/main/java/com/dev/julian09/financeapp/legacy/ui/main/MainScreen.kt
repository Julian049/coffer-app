package com.dev.julian09.financeapp.legacy.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.julian09.financeapp.legacy.domain.model.Transaction
import com.dev.julian09.financeapp.legacy.viewmodel.TransactionViewModel
import com.dev.julian09.financeapp.ui.theme.AppBackground
import com.dev.julian09.financeapp.ui.theme.PendingSync
import com.dev.julian09.financeapp.ui.theme.SlateBluePrimary
import com.dev.julian09.financeapp.ui.theme.SyncedCloud
import com.dev.julian09.financeapp.ui.theme.TextOnDark

@Composable
fun MainScreen(
    onAddNewTransaction: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(uiState, onAddNewTransaction, viewModel::syncTransactions)
}

@Composable
fun MainScreenContent(
    uiState: TransactionViewModel.UiState,
    onAddNewTransaction: () -> Unit,
    onSyncTransactions: () -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewTransaction,
            ) {
                Icon(Icons.Filled.Add, "Floating action button.")
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(AppBackground),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Saldo actual",
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(all = 8.dp),
                )
                Button(
                    onClick = onSyncTransactions,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = SlateBluePrimary,
                        ),
                ) {
                    Text("Sincronizar", color = TextOnDark)
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$${uiState.amount}",
                    fontSize = 30.sp,
                )
                var color: Color?
                if (uiState.isApiHealthy == false) {
                    color = PendingSync
                } else {
                    color = SyncedCloud
                }
                Box(
                    modifier =
                        Modifier
                            .padding(all = 14.dp)
                            .size(20.dp)
                            .background(color = color, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {}
            }
            Spacer(modifier = Modifier.padding(bottom = 15.dp))
            Text(text = "Transacciones", modifier = Modifier.padding(all = 8.dp))

            if (uiState.isLoading) {
                Text("Cargando transacciones...", Modifier.padding(8.dp))
            } else if (uiState.transactions.isEmpty()) {
                Text(
                    "Error: ${uiState.errorMessage}",
                    color = Color.Red,
                )
            } else {
                LazyColumn {
                    items(uiState.transactions) { transaction ->
                        TransactionItem(
                            title = transaction.title,
                            date = transaction.date,
                            status = transaction.synced,
                            value = transaction.value,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Estado: Cargado con datos")
fun MainScreenPreview() {
    // Te inventas un estado "falso" (Mock)
    val mockUiState =
        TransactionViewModel.UiState(
            isLoading = false,
            transactions =
                listOf(
                    Transaction("1", "Mercado", -150.00, true, "Supermercado XYZ", "05 May 2026", true),
                    Transaction("2", "Salario", 2500.00, false, "Pago mensual", "01 May 2026", true),
                ),
            amount = 2350.0,
            errorMessage = null,
            isApiHealthy = true,
        )
    MainScreenContent(
        uiState = mockUiState,
        onAddNewTransaction = {},
    )
}

@Composable
@Preview(showBackground = true, name = "Estado: Cargando")
fun MainScreenLoadingPreview() {
    val mockUiState =
        TransactionViewModel.UiState(
            isLoading = true,
            transactions = emptyList(),
            errorMessage = null,
            isApiHealthy = true,
        )
    MainScreenContent(
        uiState = mockUiState,
        onAddNewTransaction = {},
    )
}
