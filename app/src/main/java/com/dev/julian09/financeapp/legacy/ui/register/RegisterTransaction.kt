package com.dev.julian09.financeapp.legacy.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.legacy.viewmodel.TransactionViewModel
import com.dev.julian09.financeapp.ui.theme.AppBackground
import com.dev.julian09.financeapp.ui.theme.DividerGray
import com.dev.julian09.financeapp.ui.theme.ExpenseRed
import com.dev.julian09.financeapp.ui.theme.ExpenseRedContainer
import com.dev.julian09.financeapp.ui.theme.IncomeGreen
import com.dev.julian09.financeapp.ui.theme.IncomeGreenContainer
import com.dev.julian09.financeapp.ui.theme.SlateBluePrimary
import com.dev.julian09.financeapp.ui.theme.SurfaceWhite
import com.dev.julian09.financeapp.ui.theme.TextOnDark
import com.dev.julian09.financeapp.ui.theme.TextPrimary
import com.dev.julian09.financeapp.ui.theme.TextSecondary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterTransaction(
    onBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var amountValue by remember { mutableStateOf("") }
    var titleValue by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(2) }
    val calendar = Calendar.getInstance()
    val dateValue =
        rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis,
        )
    val timeValue =
        rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
        )
    var descriptionValue by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isTransactionAdded) {
        if (uiState.isTransactionAdded) {
            onBack()
            viewModel.resetNavigation()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors =
                        topAppBarColors(
                            containerColor = SlateBluePrimary,
                            titleContentColor = TextOnDark,
                        ),
                    title = {
                        Text("Add transaction")
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextOnDark,
                            )
                        }
                    },
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = DividerGray,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(AppBackground),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(all = 20.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DividerGray)
                            .padding(all = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DefaultButtonType(
                        "Ingreso",
                        selectedType,
                        { selectedType = 1 },
                        1,
                        IncomeGreenContainer,
                        IncomeGreen,
                        Modifier.weight(1f),
                    )
                    DefaultButtonType(
                        "Gasto",
                        selectedType,
                        { selectedType = 2 },
                        2,
                        ExpenseRedContainer,
                        ExpenseRed,
                        Modifier.weight(1f),
                    )
                }
                DefaultText("Valor")
                DefaultOutlinedTextField(
                    amountValue,
                    { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            amountValue = newValue
                        }
                    },
                    "0.00",
                    KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                DefaultText("Titulo")
                DefaultOutlinedTextField(
                    titleValue,
                    { titleValue = it },
                    "Titulo",
                )
                DefaultText("Fecha")
//            DatePicker(state = dateValue)
//            TimePicker(state = timeValue)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${dateValue.selectedDateMillis}",
                        color = TextPrimary,
                    )
                    Text(
                        text = "${timeValue.hour} : ${timeValue.minute}",
                        color = TextPrimary,
                    )
                }
                DefaultText("Descripcion")
                DefaultOutlinedTextField(
                    descriptionValue,
                    { descriptionValue = it },
                    "Agregar descripcion (opcional)",
                )
            }
            Button(
                onClick = {
                    if (!uiState.isSaving) {
                        viewModel.addTransaction(
                            titleValue,
                            if (selectedType == 2) ("-$amountValue") else amountValue,
                            descriptionValue,
                            "${dateValue.selectedDateMillis} ${timeValue.hour}:${timeValue.minute}",
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = SlateBluePrimary,
                    ),
            ) {
                Text("Guardar", color = TextOnDark)
            }
        }
    }
}

@Composable
fun DefaultText(text: String) {
    Text(
        text = text,
        modifier =
            Modifier
                .fillMaxWidth(),
        color = TextPrimary,
    )
}

@Composable
fun DefaultOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextSecondary) },
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardType,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DividerGray,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
            ),
    )
}

@Composable
fun DefaultButtonType(
    text: String,
    selectedType: Int,
    onActionClick: () -> Unit,
    numberToActivate: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
) {
    Button(
        onClick = onActionClick,
        modifier = modifier,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (selectedType == numberToActivate) containerColor else DividerGray,
                contentColor = if (selectedType == numberToActivate) contentColor else TextOnDark,
            ),
    ) {
        Text(
            text = text,
            modifier =
                Modifier
                    .padding(all = 8.dp),
            textAlign = TextAlign.Center,
            color = if (selectedType == numberToActivate) contentColor else TextSecondary,
            fontWeight = if (selectedType == numberToActivate) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Preview
@Composable
fun RegisterTransactionPreview() {
    RegisterTransaction({})
}
