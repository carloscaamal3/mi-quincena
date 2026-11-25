package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseItem
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyles
import java.text.NumberFormat
import java.util.Locale

private val mxnFormatter: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
}

fun formatMxn(amount: Double): String {
    val formatted = mxnFormatter.format(amount)
    return if (formatted.startsWith("MX$")) {
        formatted.replace("MX$", "$")
    } else {
        formatted
    }
}

fun parseAmountInput(text: String): Double? {
    val normalized = text.replace(",", "").replace("$", "").trim()
    return normalized.toDoubleOrNull()
}

@Composable
fun BankingHeroCard(
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        color = startColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(startColor, endColor)
                    )
                )
        ) {
            // Architectural geometric background rings
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.maxDimension * 0.42f,
                    center = Offset(size.width * 0.92f, size.height * 0.12f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = size.maxDimension * 0.28f,
                    center = Offset(size.width * 0.08f, size.height * 0.95f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun BankingSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, financeColors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun MetricMiniCard(
    label: String,
    amount: Double,
    accentColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financeColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatMxn(amount),
                style = MoneyTextStyles.CardAmount,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ExpenseItemRow(
    item: ExpenseItem,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    testTagPrefix: String
) {
    val financeColors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onTogglePaid)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("${testTagPrefix}_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.pagado) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (item.pagado) "Pagado" else "Marcar como pagado",
            tint = if (item.pagado) financeColors.positive else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nombre,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.pagado) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.pagado) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.nota.isNotBlank()) {
                Text(
                    text = item.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMxn(item.monto),
                style = MoneyTextStyles.RowAmount,
                color = if (item.pagado) financeColors.positive else MaterialTheme.colorScheme.onSurface
            )
            if (item.pagado) {
                Text(
                    text = "Pagado",
                    style = MaterialTheme.typography.labelSmall,
                    color = financeColors.positive
                )
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.testTag("${testTagPrefix}_edit_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Editar ${item.nombre}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("${testTagPrefix}_delete_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Eliminar ${item.nombre}",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ExpenseEditDialog(
    title: String,
    initialItem: ExpenseItem?,
    onDismiss: () -> Unit,
    onSave: (nombre: String, monto: Double, nota: String) -> Unit
) {
    var nombre by remember(initialItem) { mutableStateOf(initialItem?.nombre ?: "") }
    var montoText by remember(initialItem) {
        mutableStateOf(
            if (initialItem != null) String.format(Locale.US, "%.2f", initialItem.monto) else ""
        )
    }
    var nota by remember(initialItem) { mutableStateOf(initialItem?.nota ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        errorText = null
                    },
                    label = { Text("Concepto") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_dialog_name")
                )
                OutlinedTextField(
                    value = montoText,
                    onValueChange = {
                        montoText = it
                        errorText = null
                    },
                    label = { Text("Monto (MXN)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_dialog_amount")
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota opcional (ej. $400/sem)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_dialog_note")
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = parseAmountInput(montoText)
                    when {
                        nombre.trim().isEmpty() -> errorText = "Escribe un nombre para el concepto."
                        parsed == null || parsed < 0.0 -> errorText = "Ingresa un monto válido en MXN."
                        else -> onSave(nombre.trim(), parsed, nota.trim())
                    }
                },
                modifier = Modifier.testTag("expense_dialog_save")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFile: () -> Unit,
    onCopyJson: () -> Unit,
    onImportFromText: (String) -> Boolean
) {
    var showPasteField by remember { mutableStateOf(false) }
    var pastedJson by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Respaldo JSON", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Todos tus cambios se guardan automáticamente en este dispositivo. Exporta o importa un archivo JSON si cambias de equipo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onExportToFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_export_file_btn")
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar archivo JSON")
                }
                OutlinedButton(
                    onClick = onImportFromFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_import_file_btn")
                ) {
                    Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Importar archivo JSON")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onCopyJson,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_copy_json_btn")
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar JSON")
                    }
                    TextButton(
                        onClick = { showPasteField = !showPasteField },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (showPasteField) "Ocultar texto" else "Pegar JSON")
                    }
                }
                if (showPasteField) {
                    OutlinedTextField(
                        value = pastedJson,
                        onValueChange = {
                            pastedJson = it
                            pasteError = null
                        },
                        label = { Text("Pega aquí tu respaldo JSON") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pasteError != null) {
                        Text(
                            text = pasteError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = {
                            if (onImportFromText(pastedJson)) {
                                onDismiss()
                            } else {
                                pasteError = "El texto pegado no es un respaldo JSON válido."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restaurar desde texto")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun ConfirmResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Reiniciar todos los datos?") },
        text = {
            Text(
                "Se restaurarán los valores iniciales de tu quincena, gastos fijos, aguinaldo, préstamos EKT, TV DiDi y las 25 compras de Banco Azteca.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_reset_btn")
            ) {
                Text("Sí, reiniciar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
