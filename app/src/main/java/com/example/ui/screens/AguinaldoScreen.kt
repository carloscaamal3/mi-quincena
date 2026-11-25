package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.data.AztecaPurchase
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
fun AguinaldoScreen(
    state: FinanceState,
    onUpdateAguinaldo: (Double) -> Unit,
    onToggleGastoPagado: (String) -> Unit,
    onSaveGasto: (id: String?, nombre: String, monto: Double, nota: String) -> Unit,
    onDeleteGasto: (String) -> Unit,
    onSuggestOptimalPurchases: (Double) -> Set<Int>,
    onApplyLiquidaciones: (Set<Int>) -> Unit
) {
    val financeColors = LocalFinanceColors.current

    var aguinaldoInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", state.aguinaldo)) }
    LaunchedEffect(state.aguinaldo) {
        val currentParsed = parseAmountInput(aguinaldoInput)
        if (currentParsed == null || kotlin.math.abs(currentParsed - state.aguinaldo) > 0.005) {
            aguinaldoInput = String.format(Locale.US, "%.2f", state.aguinaldo)
        }
    }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var showSimulatorDialog by remember { mutableStateOf(false) }

    val usedRatio = if (state.aguinaldo > 0.0) {
        (state.totalGastosAguinaldo / state.aguinaldo).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = spring(stiffness = 220f),
        label = "aguinaldoProgress"
    )
    val isDeficit = state.sobranteAguinaldo < 0.0
    val usedPercent = if (state.aguinaldo > 0.0) {
        ((state.totalGastosAguinaldo / state.aguinaldo) * 100.0).roundToInt()
    } else 0

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
            // 1. Hero Aguinaldo Card
            item(key = "hero_aguinaldo") {
                BankingHeroCard(
                    startColor = if (isDeficit) financeColors.negative else financeColors.aguinaldoStart,
                    endColor = if (isDeficit) Color(0xFF9F1239) else financeColors.aguinaldoEnd,
                    modifier = Modifier.testTag("aguinaldo_hero_card")
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
                                imageVector = Icons.Outlined.CardGiftcard,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sobrante libre del aguinaldo",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (isDeficit) "Excedido" else "$usedPercent% asignado",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formatMxn(state.sobranteAguinaldo),
                        style = MoneyTextStyles.HeroAmount,
                        color = Color.White,
                        modifier = Modifier.testTag("aguinaldo_sobrante_amount")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (isDeficit) Color(0xFFFDA4AF) else Color(0xFFFDE68A),
                        trackColor = Color.White.copy(alpha = 0.24f),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showSimulatorDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = financeColors.aguinaldoStart
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_simular_azteca")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Simular liquidación Banco Azteca",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 2. KPI Mini Cards
            item(key = "aguinaldo_kpis") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        label = "Aguinaldo total",
                        amount = state.aguinaldo,
                        accentColor = financeColors.warning,
                        subtitle = "Ingreso de temporada",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        label = "Compromisos",
                        amount = state.totalGastosAguinaldo,
                        accentColor = financeColors.positive,
                        subtitle = "Pagado ${formatMxn(state.totalGastosAguinaldoPagados)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Editable Aguinaldo Input
            item(key = "aguinaldo_input_card") {
                BankingSectionCard {
                    Text(
                        text = "Monto de aguinaldo",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = aguinaldoInput,
                        onValueChange = { raw ->
                            aguinaldoInput = raw
                            parseAmountInput(raw)?.let { onUpdateAguinaldo(it) }
                        },
                        label = { Text("Aguinaldo neto (MXN)") },
                        prefix = { Text("$", style = MoneyTextStyles.RowAmount) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MoneyTextStyles.CardAmount,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("aguinaldo_input")
                    )
                }
            }

            // 4. Seasonal Expenses Header
            item(key = "seasonal_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Compromisos y Navidad",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${state.gastosAguinaldo.count { it.pagado }} de ${state.gastosAguinaldo.size} marcados como pagados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            editingExpense = null
                            showExpenseDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_gasto_aguinaldo_btn")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
            }

            // 5. Seasonal Expenses List
            item(key = "seasonal_list") {
                BankingSectionCard {
                    if (state.gastosAguinaldo.isEmpty()) {
                        Text(
                            text = "Sin compromisos registrados. Toca \"Agregar\" para incluir gastos de temporada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        state.gastosAguinaldo.forEachIndexed { index, item ->
                            ExpenseItemRow(
                                item = item,
                                onTogglePaid = { onToggleGastoPagado(item.id) },
                                onEdit = {
                                    editingExpense = item
                                    showExpenseDialog = true
                                },
                                onDelete = { onDeleteGasto(item.id) },
                                testTagPrefix = "gasto_aguinaldo"
                            )
                            if (index < state.gastosAguinaldo.lastIndex) {
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
            title = if (editingExpense == null) "Nuevo compromiso" else "Editar compromiso",
            initialItem = editingExpense,
            onDismiss = { showExpenseDialog = false },
            onSave = { nombre, monto, nota ->
                onSaveGasto(editingExpense?.id, nombre, monto, nota)
                showExpenseDialog = false
            }
        )
    }

    if (showSimulatorDialog) {
        AztecaSimulatorDialog(
            sobranteAguinaldo = state.sobranteAguinaldo,
            comprasActivas = state.bancoAzteca.comprasActivas,
            suggestedIds = remember(state.sobranteAguinaldo, state.bancoAzteca.compras) {
                onSuggestOptimalPurchases(state.sobranteAguinaldo)
            },
            onDismiss = { showSimulatorDialog = false },
            onApplyLiquidaciones = { ids ->
                onApplyLiquidaciones(ids)
                showSimulatorDialog = false
            }
        )
    }
}

@Composable
private fun AztecaSimulatorDialog(
    sobranteAguinaldo: Double,
    comprasActivas: List<AztecaPurchase>,
    suggestedIds: Set<Int>,
    onDismiss: () -> Unit,
    onApplyLiquidaciones: (Set<Int>) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    var selectedIds by remember(suggestedIds) { mutableStateOf(suggestedIds) }

    val sortedPurchases = remember(comprasActivas) {
        comprasActivas.sortedWith(
            compareByDescending<AztecaPurchase> { it.sinComision }
                .thenByDescending { it.semanasPagadas }
                .thenByDescending { it.monto }
        )
    }

    val selectedTotal = comprasActivas.filter { it.id in selectedIds }.sumOf { it.monto }
    val remainingAfter = sobranteAguinaldo - selectedTotal
    val overBudget = remainingAfter < -0.005

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Simulador Banco Azteca", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Prioriza compras Sin comisión (>13 sem)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (overBudget) financeColors.negativeContainer else financeColors.positiveContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sobrante aguinaldo:", style = MaterialTheme.typography.bodySmall)
                            Text(formatMxn(sobranteAguinaldo), style = MoneyTextStyles.CaptionAmount)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Seleccionado (${selectedIds.size}):", style = MaterialTheme.typography.bodySmall)
                            Text(formatMxn(selectedTotal), style = MoneyTextStyles.CaptionAmount)
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (overBudget) "Te faltarían:" else "Te quedarían:",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (overBudget) financeColors.onNegativeContainer else financeColors.onPositiveContainer
                            )
                            Text(
                                text = formatMxn(if (overBudget) -remainingAfter else remainingAfter),
                                style = MoneyTextStyles.RowAmount,
                                color = if (overBudget) financeColors.onNegativeContainer else financeColors.onPositiveContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedIds = suggestedIds },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Óptima (>13 sem)", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = { selectedIds = emptySet() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Limpiar", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (sortedPurchases.isEmpty()) {
                    Text(
                        text = "¡No tienes compras activas en Banco Azteca!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sortedPurchases, key = { it.id }) { purchase ->
                            val isSelected = purchase.id in selectedIds
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                border = if (isSelected) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedIds = if (isSelected) {
                                            selectedIds - purchase.id
                                        } else {
                                            selectedIds + purchase.id
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Compra #${purchase.id} · ${purchase.semanasPagadas} sem",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = if (purchase.sinComision) "Sin comisión (>13 sem)" else "Esperar (≤13 sem)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (purchase.sinComision) financeColors.positive else financeColors.warning
                                        )
                                    }
                                    Text(
                                        text = formatMxn(purchase.monto),
                                        style = MoneyTextStyles.RowAmount
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyLiquidaciones(selectedIds) },
                enabled = selectedIds.isNotEmpty() && !overBudget,
                modifier = Modifier.testTag("apply_simulated_liquidaciones_btn")
            ) {
                Text("Liquidar (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
