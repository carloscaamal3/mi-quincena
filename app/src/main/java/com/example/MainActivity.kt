package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.configureAppCheck
import com.example.ui.FinanceViewModel
import com.example.ui.components.BackupDialog
import com.example.ui.components.ConfirmResetDialog
import com.example.ui.screens.AguinaldoScreen
import com.example.ui.screens.AsesorScreen
import com.example.ui.screens.DeudasScreen
import com.example.ui.screens.QuincenaScreen
import com.example.ui.theme.MiQuincenaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FinanceTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    QUINCENA(
        route = "quincena",
        label = "Quincena",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    ),
    AGUINALDO(
        route = "aguinaldo",
        label = "Aguinaldo",
        selectedIcon = Icons.Filled.CardGiftcard,
        unselectedIcon = Icons.Outlined.CardGiftcard
    ),
    DEUDAS(
        route = "deudas",
        label = "Deudas",
        selectedIcon = Icons.Filled.CreditCard,
        unselectedIcon = Icons.Outlined.CreditCard
    ),
    ASESOR(
        route = "asesor",
        label = "Asesor IA",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MiQuincenaTheme {
                MiQuincenaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiQuincenaApp(
    viewModel: FinanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val financeState by viewModel.state.collectAsStateWithLifecycle()
    val advisorState by viewModel.advisorState.collectAsStateWithLifecycle()
    val snackbarText by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(FinanceTab.QUINCENA) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarText) {
        snackbarText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    // Back button on secondary tabs returns to Quincena dashboard
    BackHandler(enabled = currentTab != FinanceTab.QUINCENA) {
        currentTab = FinanceTab.QUINCENA
    }

    // Zero-permission SAF JSON Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.exportBackupJson().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                viewModel.showMessage("Respaldo JSON exportado correctamente")
                showBackupDialog = false
            }.onFailure {
                viewModel.showMessage("No se pudo guardar el archivo de respaldo")
            }
        }
    }

    // Zero-permission SAF JSON Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: ""
            }.onSuccess { content ->
                if (viewModel.importBackupJson(content)) {
                    showBackupDialog = false
                }
            }.onFailure {
                viewModel.showMessage("No se pudo leer el archivo seleccionado")
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                FinanceTab.QUINCENA -> "Mi Quincena"
                                FinanceTab.AGUINALDO -> "Aguinaldo y Navidad"
                                FinanceTab.DEUDAS -> "Control de Deudas"
                                FinanceTab.ASESOR -> "Asesor Financiero IA"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(
                            onClick = { showBackupDialog = true },
                            modifier = Modifier.testTag("top_bar_backup_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = "Exportar o importar respaldo JSON"
                            )
                        }
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.testTag("top_bar_reset_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "Reiniciar datos"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        FinanceTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        FinanceTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        FinanceTab.QUINCENA -> QuincenaScreen(
                            state = financeState,
                            onUpdateSueldo = viewModel::updateSueldoQuincenal,
                            onUpdateVales = viewModel::updateValesDespensa,
                            onToggleGastoPagado = viewModel::toggleGastoFijoPagado,
                            onResetPagados = viewModel::resetGastosFijosPagados,
                            onSaveGasto = viewModel::saveGastoFijo,
                            onDeleteGasto = viewModel::deleteGastoFijo,
                            onNavigateToDeudas = { currentTab = FinanceTab.DEUDAS }
                        )

                        FinanceTab.AGUINALDO -> AguinaldoScreen(
                            state = financeState,
                            onUpdateAguinaldo = viewModel::updateAguinaldo,
                            onToggleGastoPagado = viewModel::toggleGastoAguinaldoPagado,
                            onSaveGasto = viewModel::saveGastoAguinaldo,
                            onDeleteGasto = viewModel::deleteGastoAguinaldo,
                            onSuggestOptimalPurchases = viewModel::suggestAztecaPurchasesForBudget,
                            onApplyLiquidaciones = viewModel::liquidarComprasSimuladas
                        )

                        FinanceTab.DEUDAS -> DeudasScreen(
                            state = financeState,
                            onIncrementFixedDebt = viewModel::incrementFixedDebtPayment,
                            onToggleFixedDebtLiquidada = viewModel::toggleFixedDebtLiquidada,
                            onEditFixedDebt = viewModel::updateFixedDebt,
                            onUpdateAztecaPagoSemanal = viewModel::updateAztecaPagoSemanal,
                            onToggleAztecaCardLiquidada = viewModel::toggleAztecaCardLiquidada,
                            onAdvanceAllAztecaWeeks = viewModel::advanceAllActiveAztecaWeeks,
                            onToggleAztecaPurchaseLiquidada = viewModel::toggleAztecaPurchaseLiquidada,
                            onSaveAztecaPurchase = viewModel::saveAztecaPurchase
                        )

                        FinanceTab.ASESOR -> AsesorScreen(
                            financeState = financeState,
                            advisorState = advisorState,
                            contextJsonProvider = viewModel::getCurrentAiContextJson,
                            onAnalyzeSituation = viewModel::requestFullFinancialAnalysis,
                            onAskQuestion = viewModel::askAdvisorQuestion,
                            onClearChat = viewModel::clearAdvisorChat
                        )
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onExportToFile = {
                val dateTag = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                exportLauncher.launch("mi_quincena_respaldo_$dateTag.json")
            },
            onImportFromFile = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onCopyJson = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(
                    ClipData.newPlainText("Respaldo Mi Quincena", viewModel.exportBackupJson())
                )
                viewModel.showMessage("Respaldo JSON copiado al portapapeles")
            },
            onImportFromText = { text -> viewModel.importBackupJson(text) }
        )
    }

    if (showResetDialog) {
        ConfirmResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.resetAllData()
                showResetDialog = false
            }
        )
    }
}
