package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseItem
import com.example.data.FinanceState
import com.example.ui.components.BankingHeroCard
import com.example.ui.components.BankingSectionCard
import com.example.ui.components.ExpenseEditDialog
import com.example.ui.components.ExpenseItemRow
import com.example.ui.components.MetricMiniCard
import com.example.ui.components.formatMxn
import com.example.ui.components.parseAmountInput
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyles
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun QuincenaScreen(
    state: FinanceState,
    onUpdateSueldo: (Double) -> Unit,
    onUpdateVales: (Double, String) -> Unit,
    onToggleGastoPagado: (String) -> Unit,
    onResetPagados: () -> Unit,
    onSaveGasto: (id: String?, nombre: String, monto: Double, nota: String) -> Unit,
    onDeleteGasto: (String) -> Unit,
    onNavigateToDeudas: () -> Unit
) {
    val financeColors = LocalFinanceColors.current

    var sueldoInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", state.sueldoQuincenal)) }
    LaunchedEffect(state.sueldoQuincenal) {
        val currentParsed = parseAmountInput(sueldoInput)
        if (currentParsed == null || kotlin.math.abs(currentParsed - state.sueldoQuincenal) > 0.005) {
            sueldoInput = String.format(Locale.US, "%.2f", state.sueldoQuincenal)
        }
    }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var showValesDialog by remember { mutableStateOf(false) }

    val usedRatio = state.porcentajeUsadoQuincena.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = spring(stiffness = 220f),
        label = "quincenaProgress"
    )
    val usedPercent = (state.porcentajeUsadoQuincena * 100f).roundToInt()
    val isDeficit = state.sobranteQuincenal < 0.0

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Balance Card: Sobrante Libre de la Quincena
            item(key = "hero_quincena") {
                BankingHeroCard(
                    startColor = if (isDeficit) financeColors.negative else financeColors.heroStart,
                    endColor = if (isDeficit) Color(0xFF9F1239) else financeColors.heroEnd,
                    modifier = Modifier.testTag("quincena_hero_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sobrante libre de la quincena",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (isDeficit) "Déficit" else "${(100 - usedPercent).coerceAtLeast(0)}% libre",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formatMxn(state.sobranteQuincenal),
                        style = MoneyTextStyles.HeroAmount,
                        color = Color.White,
                        modifier = Modifier.testTag("quincena_sobrante_amount")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = when {
                            isDeficit -> Color(0xFFFDA4AF)
                            usedPercent >= 85 -> Color(0xFFFCD34D)
                            else -> Color(0xFF6EE7B7)
                        },
                        trackColor = Color.White.copy(alpha = 0.22f),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Comprometido: $usedPercent% del efectivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Text(
                            text = "MXN",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            // 2. Real-time Summary Cards (Ingresos vs Gastos + Deudas)
            item(key = "kpi_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        label = "Total Ingresos",
                        amount = state.sueldoQuincenal,
                        accentColor = financeColors.positive,
                        subtitle = "Efectivo neto",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_ingresos")
                    )
                    MetricMiniCard(
                        label = "Gastos + Deudas",
                        amount = state.totalEgresosQuincenales,
                        accentColor = financeColors.warning,
                        subtitle = "Fijos ${formatMxn(state.totalGastosFijos)}",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_total_egresos")
                    )
                }
            }

            // 3. Salary Input + Quick Presets
            item(key = "salary_input") {
                BankingSectionCard {
                    Text(
                        text = "Sueldo neto en efectivo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sueldoInput,
                        onValueChange = { raw ->
                            sueldoInput = raw
                            parseAmountInput(raw)?.let { onUpdateSueldo(it) }
                        },
                        label = { Text("Sueldo de esta quincena (MXN)") },
                        prefix = { Text("$", style = MoneyTextStyles.RowAmount) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MoneyTextStyles.CardAmount,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sueldo_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isBase = kotlin.math.abs(state.sueldoQuincenal - FinanceState.QUINCENA_BASE) < 0.01
                        val isBono = kotlin.math.abs(state.sueldoQuincenal - FinanceState.QUINCENA_CON_BONO) < 0.01

                        if (isBase) {
                            Button(
                                onClick = { onUpdateSueldo(FinanceState.QUINCENA_BASE) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_quincena_base")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Quincena Base", style = MaterialTheme.typography.labelLarge)
                                    Text("$3,680.25", style = MoneyTextStyles.CaptionAmount)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onUpdateSueldo(FinanceState.QUINCENA_BASE) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_quincena_base")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Quincena Base", style = MaterialTheme.typography.labelLarge)
                                    Text("$3,680.25", style = MoneyTextStyles.CaptionAmount)
                                }
                            }
                        }

                        if (isBono) {
                            Button(
                                onClick = { onUpdateSueldo(FinanceState.QUINCENA_CON_BONO) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_quincena_bono")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Con Bono", style = MaterialTheme.typography.labelLarge)
                                    Text("$4,017.42", style = MoneyTextStyles.CaptionAmount)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onUpdateSueldo(FinanceState.QUINCENA_CON_BONO) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_quincena_bono")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Con Bono", style = MaterialTheme.typography.labelLarge)
                                    Text("$4,017.42", style = MoneyTextStyles.CaptionAmount)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Vales de Despensa (Separate Indicator)
            item(key = "vales_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vales_despensa_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = financeColors.positiveContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, financeColors.positive.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(financeColors.positive.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restaurant,
                                contentDescription = "Vales de despensa",
                                tint = financeColors.onPositiveContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Vales de despensa",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = financeColors.onPositiveContainer
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = financeColors.positive.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "Aparte",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = financeColors.onPositiveContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.valesNota,
                                style = MaterialTheme.typography.bodySmall,
                                color = financeColors.onPositiveContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = formatMxn(state.valesDespensa),
                            style = MoneyTextStyles.CardAmount,
                            color = financeColors.onPositiveContainer
                        )
                        IconButton(
                            onClick = { showValesDialog = true },
                            modifier = Modifier.testTag("edit_vales_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar vales de despensa",
                                tint = financeColors.onPositiveContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 5. Auto-Synced Active Debts Card
            item(key = "synced_debts_card") {
                BankingSectionCard(modifier = Modifier.testTag("synced_debts_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Pago quincenal de deudas",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Sincronizado con el módulo Deudas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = formatMxn(state.totalDeudasQuincenales),
                            style = MoneyTextStyles.CardAmount,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = financeColors.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    SyncedDebtMiniRow("Tarjeta Banco Azteca", state.bancoAzteca.pagoQuincenalActivo, state.bancoAzteca.estaLiquidada)
                    SyncedDebtMiniRow("Préstamo EKT 2", state.prestamoEkt2.pagoQuincenalActivo, state.prestamoEkt2.liquidada)
                    SyncedDebtMiniRow("TV DiDi Card (0% MSI)", state.tvDidi.pagoQuincenalActivo, state.tvDidi.liquidada)
                    SyncedDebtMiniRow("Préstamo EKT 1 (con mamá)", state.prestamoEkt1.pagoQuincenalActivo, state.prestamoEkt1.liquidada)

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToDeudas,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("go_to_deudas_btn")
                    ) {
                        Text("Administrar deudas y compras →")
                    }
                }
            }

            // 6. Fixed Biweekly Expenses Header
            item(key = "fixed_expenses_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gastos fijos quincenales",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Pagado ${formatMxn(state.totalGastosFijosPagados)} de ${formatMxn(state.totalGastosFijos)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onResetPagados,
                            modifier = Modifier.testTag("reset_gastos_pagados_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "Desmarcar gastos pagados"
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                editingExpense = null
                                showExpenseDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_gasto_fijo_btn")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                }
            }

            // 7. Fixed Biweekly Expenses List inside clean card-like surface
            item(key = "fixed_expenses_list") {
                BankingSectionCard {
                    if (state.gastosFijos.isEmpty()) {
                        Text(
                            text = "No tienes gastos fijos registrados. Toca \"Agregar\" para añadir uno.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        state.gastosFijos.forEachIndexed { index, item ->
                            ExpenseItemRow(
                                item = item,
                                onTogglePaid = { onToggleGastoPagado(item.id) },
                                onEdit = {
                                    editingExpense = item
                                    showExpenseDialog = true
                                },
                                onDelete = { onDeleteGasto(item.id) },
                                testTagPrefix = "gasto_fijo"
                            )
                            if (index < state.gastosFijos.lastIndex) {
                                HorizontalDivider(
                                    color = financeColors.cardBorder.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showExpenseDialog) {
        ExpenseEditDialog(
            title = if (editingExpense == null) "Nuevo gasto fijo" else "Editar gasto fijo",
            initialItem = editingExpense,
            onDismiss = { showExpenseDialog = false },
            onSave = { nombre, monto, nota ->
                onSaveGasto(editingExpense?.id, nombre, monto, nota)
                showExpenseDialog = false
            }
        )
    }

    if (showValesDialog) {
        EditValesDialog(
            initialAmount = state.valesDespensa,
            initialNota = state.valesNota,
            onDismiss = { showValesDialog = false },
            onSave = { amount, nota ->
                onUpdateVales(amount, nota)
                showValesDialog = false
            }
        )
    }
}

@Composable
private fun SyncedDebtMiniRow(
    label: String,
    amount: Double,
    liquidated: Boolean
) {
    val financeColors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (liquidated) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        if (liquidated) {
            Surface(
                shape = RoundedCornerShape(50),
                color = financeColors.positiveContainer
            ) {
                Text(
                    text = "Liquidada · $0.00",
                    style = MaterialTheme.typography.labelSmall,
                    color = financeColors.onPositiveContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = formatMxn(amount),
                style = MoneyTextStyles.RowAmount,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EditValesDialog(
    initialAmount: Double,
    initialNota: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialAmount)) }
    var nota by remember { mutableStateOf(initialNota) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar vales de despensa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto en vales (MXN)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = parseAmountInput(amountText) ?: initialAmount
                    onSave(parsed.coerceAtLeast(0.0), nota.trim().ifEmpty { "Exclusivo desayuno y cena" })
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
