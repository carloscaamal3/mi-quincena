package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AztecaPurchase
import com.example.data.ChatMessage
import com.example.data.DefaultFinanceData
import com.example.data.ExpenseItem
import com.example.data.FinanceAdvisorRepository
import com.example.data.FinanceRepository
import com.example.data.FinanceState
import com.example.data.FixedDebt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.math.max

private const val TAG = "FinanceViewModel"

data class AdvisorUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val streamingResponse: String = "",
    val errorMessage: String? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(application)
    private val advisorRepository = FinanceAdvisorRepository()

    private val _state = MutableStateFlow(repository.loadState())
    val state: StateFlow<FinanceState> = _state.asStateFlow()

    private val _advisorState = MutableStateFlow(AdvisorUiState())
    val advisorState: StateFlow<AdvisorUiState> = _advisorState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private fun updateAndPersist(transform: (FinanceState) -> FinanceState) {
        val updated = transform(_state.value)
        _state.value = updated
        viewModelScope.launch {
            repository.saveState(updated)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    // --- MODULE 1: QUINCENA ---

    fun updateSueldoQuincenal(amount: Double) {
        updateAndPersist { it.copy(sueldoQuincenal = amount.coerceAtLeast(0.0)) }
    }

    fun updateValesDespensa(amount: Double, nota: String = _state.value.valesNota) {
        updateAndPersist {
            it.copy(
                valesDespensa = amount.coerceAtLeast(0.0),
                valesNota = nota
            )
        }
    }

    fun toggleGastoFijoPagado(id: String) {
        updateAndPersist { current ->
            current.copy(
                gastosFijos = current.gastosFijos.map {
                    if (it.id == id) it.copy(pagado = !it.pagado) else it
                }
            )
        }
    }

    fun resetGastosFijosPagados() {
        updateAndPersist { current ->
            current.copy(gastosFijos = current.gastosFijos.map { it.copy(pagado = false) })
        }
        showMessage("Gastos fijos desmarcados para una nueva quincena")
    }

    fun saveGastoFijo(id: String?, nombre: String, monto: Double, nota: String) {
        val cleanName = nombre.trim()
        if (cleanName.isEmpty()) return
        updateAndPersist { current ->
            val updatedList = if (id == null) {
                current.gastosFijos + ExpenseItem(
                    id = "gf_${UUID.randomUUID().toString().take(8)}",
                    nombre = cleanName,
                    monto = monto.coerceAtLeast(0.0),
                    nota = nota.trim(),
                    pagado = false
                )
            } else {
                current.gastosFijos.map {
                    if (it.id == id) it.copy(
                        nombre = cleanName,
                        monto = monto.coerceAtLeast(0.0),
                        nota = nota.trim()
                    ) else it
                }
            }
            current.copy(gastosFijos = updatedList)
        }
    }

    fun deleteGastoFijo(id: String) {
        updateAndPersist { current ->
            current.copy(gastosFijos = current.gastosFijos.filterNot { it.id == id })
        }
    }

    // --- MODULE 2: AGUINALDO ---

    fun updateAguinaldo(amount: Double) {
        updateAndPersist { it.copy(aguinaldo = amount.coerceAtLeast(0.0)) }
    }

    fun toggleGastoAguinaldoPagado(id: String) {
        updateAndPersist { current ->
            current.copy(
                gastosAguinaldo = current.gastosAguinaldo.map {
                    if (it.id == id) it.copy(pagado = !it.pagado) else it
                }
            )
        }
    }

    fun saveGastoAguinaldo(id: String?, nombre: String, monto: Double, nota: String) {
        val cleanName = nombre.trim()
        if (cleanName.isEmpty()) return
        updateAndPersist { current ->
            val updatedList = if (id == null) {
                current.gastosAguinaldo + ExpenseItem(
                    id = "ag_${UUID.randomUUID().toString().take(8)}",
                    nombre = cleanName,
                    monto = monto.coerceAtLeast(0.0),
                    nota = nota.trim(),
                    pagado = false
                )
            } else {
                current.gastosAguinaldo.map {
                    if (it.id == id) it.copy(
                        nombre = cleanName,
                        monto = monto.coerceAtLeast(0.0),
                        nota = nota.trim()
                    ) else it
                }
            }
            current.copy(gastosAguinaldo = updatedList)
        }
    }

    fun deleteGastoAguinaldo(id: String) {
        updateAndPersist { current ->
            current.copy(gastosAguinaldo = current.gastosAguinaldo.filterNot { it.id == id })
        }
    }

    fun liquidarComprasSimuladas(purchaseIds: Set<Int>) {
        if (purchaseIds.isEmpty()) return
        updateAndPersist { current ->
            val updatedCompras = current.bancoAzteca.compras.map { purchase ->
                if (purchase.id in purchaseIds) purchase.copy(liquidada = true) else purchase
            }
            current.copy(bancoAzteca = current.bancoAzteca.copy(compras = updatedCompras))
        }
        showMessage("${purchaseIds.size} compra(s) de Banco Azteca marcadas como liquidadas")
    }

    /**
     * Suggests an optimal set of active Banco Azteca purchases (>13 weeks, no commission)
     * that fit within the available budget, maximizing amount cleared.
     */
    fun suggestAztecaPurchasesForBudget(budget: Double): Set<Int> {
        if (budget <= 0.0) return emptySet()
        val eligible = _state.value.bancoAzteca.comprasSinComisionActivas
            .sortedWith(compareByDescending<AztecaPurchase> { it.semanasPagadas }.thenByDescending { it.monto })
        if (eligible.isEmpty()) return emptySet()

        // Exact subset search since eligible list is small (<= 25); greedy fallback if > 20
        if (eligible.size <= 18) {
            var bestSum = 0.0
            var bestSet = emptySet<Int>()

            fun dfs(index: Int, currentSum: Double, chosen: List<Int>) {
                if (currentSum > bestSum || (currentSum == bestSum && chosen.size > bestSet.size)) {
                    bestSum = currentSum
                    bestSet = chosen.toSet()
                }
                if (index >= eligible.size) return
                val item = eligible[index]
                if (currentSum + item.monto <= budget + 0.005) {
                    dfs(index + 1, currentSum + item.monto, chosen + item.id)
                }
                dfs(index + 1, currentSum, chosen)
            }
            dfs(0, 0.0, emptyList())
            return bestSet
        } else {
            var remaining = budget
            val chosen = mutableSetOf<Int>()
            for (item in eligible) {
                if (item.monto <= remaining + 0.005) {
                    chosen.add(item.id)
                    remaining -= item.monto
                }
            }
            return chosen
        }
    }

    // --- MODULE 3: DEUDAS ---

    fun incrementFixedDebtPayment(debtId: String) {
        updateAndPersist { current ->
            fun advance(debt: FixedDebt): FixedDebt {
                if (debt.liquidada) return debt
                val nextPaid = (debt.pagosRealizados + 1).coerceAtMost(debt.totalPagos)
                val nextSaldo = max(0.0, debt.saldo - debt.pagoPorUnidad)
                val reachedEnd = nextPaid >= debt.totalPagos || nextSaldo <= 0.01
                return debt.copy(
                    pagosRealizados = nextPaid,
                    saldo = if (reachedEnd) 0.0 else nextSaldo,
                    liquidada = reachedEnd
                )
            }
            when (debtId) {
                current.prestamoEkt1.id -> current.copy(prestamoEkt1 = advance(current.prestamoEkt1))
                current.prestamoEkt2.id -> current.copy(prestamoEkt2 = advance(current.prestamoEkt2))
                current.tvDidi.id -> current.copy(tvDidi = advance(current.tvDidi))
                else -> current
            }
        }
    }

    fun toggleFixedDebtLiquidada(debtId: String) {
        updateAndPersist { current ->
            fun toggle(debt: FixedDebt): FixedDebt = debt.copy(liquidada = !debt.liquidada)
            when (debtId) {
                current.prestamoEkt1.id -> current.copy(prestamoEkt1 = toggle(current.prestamoEkt1))
                current.prestamoEkt2.id -> current.copy(prestamoEkt2 = toggle(current.prestamoEkt2))
                current.tvDidi.id -> current.copy(tvDidi = toggle(current.tvDidi))
                else -> current
            }
        }
    }

    fun updateFixedDebt(
        debtId: String,
        saldo: Double,
        pagosRealizados: Int,
        totalPagos: Int,
        pagoPorUnidad: Double,
        pagoQuincenal: Double
    ) {
        updateAndPersist { current ->
            fun applyEdit(debt: FixedDebt): FixedDebt = debt.copy(
                saldo = saldo.coerceAtLeast(0.0),
                pagosRealizados = pagosRealizados.coerceIn(0, max(1, totalPagos)),
                totalPagos = max(1, totalPagos),
                pagoPorUnidad = pagoPorUnidad.coerceAtLeast(0.0),
                pagoQuincenal = pagoQuincenal.coerceAtLeast(0.0)
            )
            when (debtId) {
                current.prestamoEkt1.id -> current.copy(prestamoEkt1 = applyEdit(current.prestamoEkt1))
                current.prestamoEkt2.id -> current.copy(prestamoEkt2 = applyEdit(current.prestamoEkt2))
                current.tvDidi.id -> current.copy(tvDidi = applyEdit(current.tvDidi))
                else -> current
            }
        }
    }

    fun updateAztecaPagoSemanal(pagoSemanal: Double) {
        updateAndPersist { current ->
            current.copy(
                bancoAzteca = current.bancoAzteca.copy(pagoSemanal = pagoSemanal.coerceAtLeast(0.0))
            )
        }
    }

    fun toggleAztecaCardLiquidada() {
        updateAndPersist { current ->
            current.copy(
                bancoAzteca = current.bancoAzteca.copy(
                    liquidadaManual = !current.bancoAzteca.liquidadaManual
                )
            )
        }
    }

    fun advanceAllActiveAztecaWeeks() {
        updateAndPersist { current ->
            val updated = current.bancoAzteca.compras.map { c ->
                if (c.liquidada) {
                    c
                } else {
                    val nextWeeks = (c.semanasPagadas + 1).coerceAtMost(c.totalSemanas)
                    c.copy(
                        semanasPagadas = nextWeeks,
                        liquidada = nextWeeks >= c.totalSemanas
                    )
                }
            }
            current.copy(bancoAzteca = current.bancoAzteca.copy(compras = updated))
        }
        showMessage("+1 semana agregada a todas las compras activas de Banco Azteca")
    }

    fun toggleAztecaPurchaseLiquidada(purchaseId: Int) {
        updateAndPersist { current ->
            val updated = current.bancoAzteca.compras.map { c ->
                if (c.id == purchaseId) c.copy(liquidada = !c.liquidada) else c
            }
            current.copy(bancoAzteca = current.bancoAzteca.copy(compras = updated))
        }
    }

    fun saveAztecaPurchase(purchaseId: Int?, monto: Double, semanasPagadas: Int, totalSemanas: Int) {
        updateAndPersist { current ->
            val safeTotal = max(1, totalSemanas)
            val safeWeeks = semanasPagadas.coerceIn(0, safeTotal)
            val updated = if (purchaseId == null) {
                val nextId = (current.bancoAzteca.compras.maxOfOrNull { it.id } ?: 0) + 1
                current.bancoAzteca.compras + AztecaPurchase(
                    id = nextId,
                    monto = monto.coerceAtLeast(0.0),
                    semanasPagadas = safeWeeks,
                    totalSemanas = safeTotal,
                    liquidada = false
                )
            } else {
                current.bancoAzteca.compras.map { c ->
                    if (c.id == purchaseId) {
                        c.copy(
                            monto = monto.coerceAtLeast(0.0),
                            semanasPagadas = safeWeeks,
                            totalSemanas = safeTotal
                        )
                    } else c
                }
            }
            current.copy(bancoAzteca = current.bancoAzteca.copy(compras = updated))
        }
    }

    // --- MODULE 4: ASESOR IA (FIREBASE AI LOGIC) ---

    fun requestFullFinancialAnalysis() {
        sendAdvisorQuery(
            displayUserPrompt = "Analiza mi situación actual y recomiéndame qué hacer",
            rawQuestion = null
        )
    }

    fun askAdvisorQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        sendAdvisorQuery(displayUserPrompt = trimmed, rawQuestion = trimmed)
    }

    fun clearAdvisorChat() {
        _advisorState.value = AdvisorUiState()
    }

    fun getCurrentAiContextJson(): String = repository.buildAiContextJson(_state.value)

    private fun sendAdvisorQuery(displayUserPrompt: String, rawQuestion: String?) {
        if (_advisorState.value.isLoading) return

        val snapshotJson = repository.buildAiContextJson(_state.value)
        val priorHistory = _advisorState.value.messages
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = displayUserPrompt
        )

        _advisorState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true,
                streamingResponse = "",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    advisorRepository.streamAdvice(
                        financialSnapshotJson = snapshotJson,
                        userQuestion = rawQuestion,
                        recentHistory = priorHistory
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            _advisorState.update { state ->
                                state.copy(streamingResponse = accumulated.toString())
                            }
                        }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    val aiMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = finalReply
                    )
                    _advisorState.update {
                        it.copy(
                            messages = it.messages + aiMsg,
                            isLoading = false,
                            streamingResponse = "",
                            errorMessage = null
                        )
                    }
                } else {
                    val emptyErr = "El asesor no devolvió texto. Intenta nuevamente."
                    _advisorState.update {
                        it.copy(
                            isLoading = false,
                            streamingResponse = "",
                            errorMessage = emptyErr
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini streaming timed out", e)
                val msg = "La consulta tardó más de 60 segundos. Verifica tu conexión e intenta nuevamente."
                _advisorState.update {
                    it.copy(
                        isLoading = false,
                        streamingResponse = "",
                        errorMessage = msg
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI financial analysis failed", e)
                val errText = e.localizedMessage ?: "No se pudo conectar con el Asesor IA."
                _advisorState.update {
                    it.copy(
                        isLoading = false,
                        streamingResponse = "",
                        errorMessage = errText
                    )
                }
            }
        }
    }

    // --- BACKUP & RESET ---

    fun exportBackupJson(): String = repository.serializeState(_state.value)

    fun importBackupJson(jsonText: String): Boolean {
        val result = repository.deserializeState(jsonText)
        return result.fold(
            onSuccess = { importedState ->
                _state.value = importedState
                viewModelScope.launch {
                    repository.saveState(importedState)
                }
                showMessage("Respaldo JSON importado correctamente")
                true
            },
            onFailure = { error ->
                Log.e(TAG, "Invalid backup JSON", error)
                showMessage("JSON inválido: verifica el archivo de respaldo")
                false
            }
        )
    }

    fun resetAllData() {
        val defaults = DefaultFinanceData.createDefaultState()
        _state.value = defaults
        _advisorState.value = AdvisorUiState()
        viewModelScope.launch {
            repository.saveState(defaults)
        }
        showMessage("Todos los datos fueron reiniciados a sus valores iniciales")
    }
}
