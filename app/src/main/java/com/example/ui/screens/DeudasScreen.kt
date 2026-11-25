package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AztecaPurchase
import com.example.data.FinanceState
import com.example.data.FixedDebt
import com.example.ui.components.BankingSectionCard
import com.example.ui.components.MetricMiniCard
import com.example.ui.components.formatMxn
import com.example.ui.components.parseAmountInput
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyles
import java.util.Locale

private enum class AztecaFilter(val label: String) {
    TODAS("Activas"),
    SIN_COMISION("Sin comisión (>13)"),
    ESPERAR("Esperar (≤13)"),
    LIQUIDADAS("Liquidadas")
}

@Composable
fun DeudasScreen(
    state: FinanceState,
    onIncrementFixedDebt: (String) -> Unit,
    onToggleFixedDebtLiquidada: (String) -> Unit,
    onEditFixedDebt: (String, Double, Int, Int, Double, Double) -> Unit,
    onUpdateAztecaPagoSemanal: (Double) -> Unit,
    onToggleAztecaCardLiquidada: () -> Unit,
    onAdvanceAllAztecaWeeks: () -> Unit,
    onToggleAztecaPurchaseLiquidada: (Int) -> Unit,
    onSaveAztecaPurchase: (Int?, Double, Int, Int) -> Unit
) {
    val financeColors = LocalFinanceColors.current

    var selectedFilter by remember { mutableStateOf(AztecaFilter.TODAS) }
    var editingFixedDebt by remember { mutableStateOf<FixedDebt?>(null) }
    var showEditAztecaPagoDialog by remember { mutableStateOf(false) }
    var showAztecaPurchaseDialog by remember { mutableStateOf(false) }
    var editingAztecaPurchase by remember { mutableStateOf<AztecaPurchase?>(null) }

    val filteredPurchases = remember(state.bancoAzteca.compras, selectedFilter) {
        when (selectedFilter) {
            AztecaFilter.TODAS -> state.bancoAzteca.comprasActivas
            AztecaFilter.SIN_COMISION -> state.bancoAzteca.comprasSinComisionActivas
            AztecaFilter.ESPERAR -> state.bancoAzteca.comprasEsperarActivas
            AztecaFilter.LIQUIDADAS -> state.bancoAzteca.compras.filter { it.liquidada }
        }
    }

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
            // 1. Summary KPI Row
            item(key = "deudas_summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        label = "Saldo total activo",
                        amount = state.saldoTotalDeudasActivas,
                        accentColor = financeColors.negative,
                        subtitle = "Préstamos + Azteca",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("deudas_saldo_total")
                    )
                    MetricMiniCard(
                        label = "Pago quincenal",
                        amount = state.totalDeudasQuincenales,
                        accentColor = MaterialTheme.colorScheme.primary,
                        subtitle = "Restado en Quincena",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("deudas_pago_quincenal")
                    )
                }
            }

            // 2. Fixed Debts Cards (EKT 1, EKT 2, TV DiDi)
            item(key = "fixed_debts_title") {
                Text(
                    text = "Préstamos y MSI",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item(key = "debt_ekt1") {
                FixedDebtCard(
                    debt = state.prestamoEkt1,
                    incrementLabel = "+1 semana pagada",
                    onIncrement = { onIncrementFixedDebt(state.prestamoEkt1.id) },
                    onToggleLiquidada = { onToggleFixedDebtLiquidada(state.prestamoEkt1.id) },
                    onEdit = { editingFixedDebt = state.prestamoEkt1 }
                )
            }

            item(key = "debt_ekt2") {
                FixedDebtCard(
                    debt = state.prestamoEkt2,
                    incrementLabel = "+1 semana pagada",
                    onIncrement = { onIncrementFixedDebt(state.prestamoEkt2.id) },
                    onToggleLiquidada = { onToggleFixedDebtLiquidada(state.prestamoEkt2.id) },
                    onEdit = { editingFixedDebt = state.prestamoEkt2 }
                )
            }

            item(key = "debt_tv_didi") {
                FixedDebtCard(
                    debt = state.tvDidi,
                    incrementLabel = "+1 mes pagado",
                    onIncrement = { onIncrementFixedDebt(state.tvDidi.id) },
                    onToggleLiquidada = { onToggleFixedDebtLiquidada(state.tvDidi.id) },
                    onEdit = { editingFixedDebt = state.tvDidi }
                )
            }

            // 3. Tarjeta de Crédito Banco Azteca Header Card
            item(key = "azteca_header_card") {
                val azteca = state.bancoAzteca
                BankingSectionCard(modifier = Modifier.testTag("azteca_card_summary")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tarjeta Banco Azteca",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (azteca.estaLiquidada) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = financeColors.positiveContainer
                                    ) {
                                        Text(
                                            text = "Liquidada",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = financeColors.onPositiveContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${azteca.comprasActivas.size} compras activas · ${azteca.comprasSinComisionActivas.size} sin comisión (>13 sem)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showEditAztecaPagoDialog = true },
                            modifier = Modifier.testTag("edit_azteca_pago_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar pago semanal Banco Azteca"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Saldo compras activas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMxn(azteca.saldoActivoTotal),
                                style = MoneyTextStyles.CardAmount,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("azteca_saldo_activo")
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Pago semanal / quincenal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatMxn(azteca.pagoSemanal)} / ${formatMxn(azteca.pagoQuincenalActivo)}",
                                style = MoneyTextStyles.RowAmount,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAdvanceAllAztecaWeeks,
                            enabled = azteca.comprasActivas.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_plus_one_week_all_azteca")
                        ) {
                            Icon(Icons.Outlined.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+1 semana a todas")
                        }
                        FilledTonalButton(
                            onClick = {
                                editingAztecaPurchase = null
                                showAztecaPurchaseDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_add_azteca_purchase")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Compra")
                        }
                    }

                    if (azteca.comprasActivas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = onToggleAztecaCardLiquidada,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                if (azteca.liquidadaManual) "Reactivar tarjeta en presupuesto" else "Pausar / marcar tarjeta liquidada"
                            )
                        }
                    }
                }
            }

            // 4. Filter Chips for the 25 Purchases
            item(key = "azteca_filters") {
                val azteca = state.bancoAzteca
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AztecaFilter.entries.forEach { filter ->
                        val count = when (filter) {
                            AztecaFilter.TODAS -> azteca.comprasActivas.size
                            AztecaFilter.SIN_COMISION -> azteca.comprasSinComisionActivas.size
                            AztecaFilter.ESPERAR -> azteca.comprasEsperarActivas.size
                            AztecaFilter.LIQUIDADAS -> azteca.compras.count { it.liquidada }
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.label} ($count)") },
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            // 5. Individual Banco Azteca Purchases Table / Rows
            if (filteredPurchases.isEmpty()) {
                item(key = "empty_azteca_filter") {
                    BankingSectionCard {
                        Text(
                            text = "No hay compras en esta categoría.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            } else {
                items(filteredPurchases, key = { "azteca_p_${it.id}" }) { purchase ->
                    AztecaPurchaseRowCard(
                        purchase = purchase,
                        onEdit = {
                            editingAztecaPurchase = purchase
                            showAztecaPurchaseDialog = true
                        },
                        onToggleLiquidar = { onToggleAztecaPurchaseLiquidada(purchase.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    editingFixedDebt?.let { debt ->
        EditFixedDebtDialog(
            debt = debt,
            onDismiss = { editingFixedDebt = null },
            onSave = { saldo, pagados, total, pagoUnidad, pagoQuincenal ->
                onEditFixedDebt(debt.id, saldo, pagados, total, pagoUnidad, pagoQuincenal)
                editingFixedDebt = null
            }
        )
    }

    if (showEditAztecaPagoDialog) {
        EditAztecaWeeklyPaymentDialog(
            initialPagoSemanal = state.bancoAzteca.pagoSemanal,
            onDismiss = { showEditAztecaPagoDialog = false },
            onSave = {
                onUpdateAztecaPagoSemanal(it)
                showEditAztecaPagoDialog = false
            }
        )
    }

    if (showAztecaPurchaseDialog) {
        EditAztecaPurchaseDialog(
            initialPurchase = editingAztecaPurchase,
            onDismiss = { showAztecaPurchaseDialog = false },
            onSave = { monto, semanas, total ->
                onSaveAztecaPurchase(editingAztecaPurchase?.id, monto, semanas, total)
                showAztecaPurchaseDialog = false
            }
        )
    }
}

@Composable
private fun FixedDebtCard(
    debt: FixedDebt,
    incrementLabel: String,
    onIncrement: () -> Unit,
    onToggleLiquidada: () -> Unit,
    onEdit: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val unitLabel = if (debt.unidad == "meses") "/mes" else "/sem"

    BankingSectionCard(modifier = Modifier.testTag("debt_card_${debt.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = debt.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (debt.esCeroInteres) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = financeColors.infoContainer
                        ) {
                            Text(
                                text = "0% MSI",
                                style = MaterialTheme.typography.labelSmall,
                                color = financeColors.onInfoContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (debt.liquidada) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = financeColors.positiveContainer
                        ) {
                            Text(
                                text = "Liquidada",
                                style = MaterialTheme.typography.labelSmall,
                                color = financeColors.onPositiveContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = debt.subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_debt_${debt.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar ${debt.nombre}",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Saldo pendiente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMxn(if (debt.liquidada) 0.0 else debt.saldo),
                    style = MoneyTextStyles.CardAmount,
                    color = if (debt.liquidada) financeColors.positive else MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatMxn(debt.pagoPorUnidad)}$unitLabel",
                    style = MoneyTextStyles.RowAmount,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatMxn(debt.pagoQuincenalActivo)} / quincena",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { if (debt.liquidada) 1f else debt.progresoFraccion },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color = when {
                debt.liquidada -> financeColors.positive
                debt.esCeroInteres -> financeColors.info
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${debt.pagosRealizados} de ${debt.totalPagos} ${debt.unidad} pagadas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Restan ${debt.pagosRestantes}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onIncrement,
                enabled = !debt.liquidada && debt.pagosRealizados < debt.totalPagos,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("increment_debt_${debt.id}")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(incrementLabel, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onToggleLiquidada,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("liquidate_debt_${debt.id}")
            ) {
                Icon(
                    imageVector = if (debt.liquidada) Icons.Outlined.Undo else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (debt.liquidada) "Reactivar" else "Liquidar")
            }
        }
    }
}

@Composable
private fun AztecaPurchaseRowCard(
    purchase: AztecaPurchase,
    onEdit: () -> Unit,
    onToggleLiquidar: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val badgeBg = when {
        purchase.liquidada -> MaterialTheme.colorScheme.surfaceVariant
        purchase.sinComision -> financeColors.positiveContainer
        else -> financeColors.warningContainer
    }
    val badgeFg = when {
        purchase.liquidada -> MaterialTheme.colorScheme.onSurfaceVariant
        purchase.sinComision -> financeColors.onPositiveContainer
        else -> financeColors.onWarningContainer
    }
    val badgeLabel = when {
        purchase.liquidada -> "Liquidada"
        purchase.sinComision -> "Sin comisión (>13 sem)"
        else -> "Esperar (≤13 sem)"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("azteca_purchase_${purchase.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (!purchase.liquidada && purchase.sinComision) {
                financeColors.positive.copy(alpha = 0.45f)
            } else {
                financeColors.cardBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "#${purchase.id}",
                            style = MoneyTextStyles.CaptionAmount,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = formatMxn(purchase.monto),
                        style = MoneyTextStyles.CardAmount,
                        color = if (purchase.liquidada) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${purchase.semanasPagadas} de ${purchase.totalSemanas} semanas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { purchase.progresoFraccion },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (purchase.sinComision) financeColors.positive else financeColors.warning,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_azteca_${purchase.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar compra #${purchase.id}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (purchase.liquidada) {
                        OutlinedButton(
                            onClick = onToggleLiquidar,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("toggle_azteca_${purchase.id}")
                        ) {
                            Text("Reactivar", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Button(
                            onClick = onToggleLiquidar,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (purchase.sinComision) {
                                    financeColors.positive
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (purchase.sinComision) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("toggle_azteca_${purchase.id}")
                        ) {
                            Icon(Icons.Outlined.CreditScore, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Liquidar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFixedDebtDialog(
    debt: FixedDebt,
    onDismiss: () -> Unit,
    onSave: (Double, Int, Int, Double, Double) -> Unit
) {
    var saldoText by remember { mutableStateOf(String.format(Locale.US, "%.2f", debt.saldo)) }
    var pagadosText by remember { mutableStateOf(debt.pagosRealizados.toString()) }
    var totalText by remember { mutableStateOf(debt.totalPagos.toString()) }
    var pagoUnidadText by remember { mutableStateOf(String.format(Locale.US, "%.2f", debt.pagoPorUnidad)) }
    var pagoQuincenalText by remember { mutableStateOf(String.format(Locale.US, "%.2f", debt.pagoQuincenal)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${debt.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = saldoText,
                    onValueChange = { saldoText = it },
                    label = { Text("Saldo actual (MXN)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pagadosText,
                        onValueChange = { pagadosText = it },
                        label = { Text("Pagadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("Total ${debt.unidad}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pagoUnidadText,
                        onValueChange = { raw ->
                            pagoUnidadText = raw
                            val parsed = parseAmountInput(raw)
                            if (parsed != null) {
                                val autoQuincena = if (debt.unidad == "meses") parsed / 2.0 else parsed * 2.0
                                pagoQuincenalText = String.format(Locale.US, "%.2f", autoQuincena)
                            }
                        },
                        label = { Text(if (debt.unidad == "meses") "Mensual" else "Semanal") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pagoQuincenalText,
                        onValueChange = { pagoQuincenalText = it },
                        label = { Text("Quincenal") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saldo = parseAmountInput(saldoText) ?: debt.saldo
                    val pagados = pagadosText.toIntOrNull() ?: debt.pagosRealizados
                    val total = totalText.toIntOrNull() ?: debt.totalPagos
                    val unidad = parseAmountInput(pagoUnidadText) ?: debt.pagoPorUnidad
                    val quincenal = parseAmountInput(pagoQuincenalText) ?: debt.pagoQuincenal
                    onSave(saldo, pagados, total, unidad, quincenal)
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

@Composable
private fun EditAztecaWeeklyPaymentDialog(
    initialPagoSemanal: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var pagoText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialPagoSemanal)) }
    val previewQuincenal = (parseAmountInput(pagoText) ?: initialPagoSemanal) * 2.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pago semanal Banco Azteca") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pagoText,
                    onValueChange = { pagoText = it },
                    label = { Text("Pago semanal actual (MXN)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Equivale a ${formatMxn(previewQuincenal)} por quincena en tu dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = parseAmountInput(pagoText) ?: initialPagoSemanal
                    onSave(parsed.coerceAtLeast(0.0))
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

@Composable
private fun EditAztecaPurchaseDialog(
    initialPurchase: AztecaPurchase?,
    onDismiss: () -> Unit,
    onSave: (Double, Int, Int) -> Unit
) {
    var montoText by remember {
        mutableStateOf(
            if (initialPurchase != null) String.format(Locale.US, "%.2f", initialPurchase.monto) else ""
        )
    }
    var semanasText by remember { mutableStateOf((initialPurchase?.semanasPagadas ?: 0).toString()) }
    var totalText by remember { mutableStateOf((initialPurchase?.totalSemanas ?: 100).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialPurchase == null) "Nueva compra Banco Azteca" else "Editar compra #${initialPurchase.id}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Saldo / monto (MXN)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = semanasText,
                        onValueChange = { semanasText = it },
                        label = { Text("Semanas pagadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("Total sem") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = parseAmountInput(montoText) ?: return@Button
                    val sem = semanasText.toIntOrNull() ?: 0
                    val total = totalText.toIntOrNull() ?: 100
                    onSave(monto, sem, total)
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
