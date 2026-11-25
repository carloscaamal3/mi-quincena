package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

private const val TAG = "FinanceRepository"
private const val STATE_FILE_NAME = "mi_quincena_state.json"

class FinanceRepository(context: Context) {
    private val stateFile = File(context.applicationContext.filesDir, STATE_FILE_NAME)

    fun loadState(): FinanceState {
        return try {
            if (!stateFile.exists()) {
                val defaults = DefaultFinanceData.createDefaultState()
                stateFile.writeText(serializeState(defaults))
                defaults
            } else {
                val raw = stateFile.readText()
                deserializeState(raw).getOrElse {
                    Log.w(TAG, "Corrupted local state JSON, restoring defaults", it)
                    DefaultFinanceData.createDefaultState()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local state", e)
            DefaultFinanceData.createDefaultState()
        }
    }

    suspend fun saveState(state: FinanceState) = withContext(Dispatchers.IO) {
        try {
            val json = serializeState(state)
            val tmpFile = File(stateFile.parentFile, "$STATE_FILE_NAME.tmp")
            tmpFile.writeText(json)
            if (!tmpFile.renameTo(stateFile)) {
                stateFile.writeText(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local state", e)
        }
    }

    fun serializeState(state: FinanceState): String {
        val root = JSONObject().apply {
            put("version", 1)
            put("sueldoQuincenal", state.sueldoQuincenal)
            put("valesDespensa", state.valesDespensa)
            put("valesNota", state.valesNota)
            put("gastosFijos", serializeExpenses(state.gastosFijos))
            put("aguinaldo", state.aguinaldo)
            put("gastosAguinaldo", serializeExpenses(state.gastosAguinaldo))
            put("prestamoEkt1", serializeFixedDebt(state.prestamoEkt1))
            put("prestamoEkt2", serializeFixedDebt(state.prestamoEkt2))
            put("tvDidi", serializeFixedDebt(state.tvDidi))
            put("bancoAzteca", serializeAzteca(state.bancoAzteca))
        }
        return root.toString(2)
    }

    fun deserializeState(jsonString: String): Result<FinanceState> = runCatching {
        val root = JSONObject(jsonString)
        val defaults = DefaultFinanceData.createDefaultState()

        val sueldo = root.getDouble("sueldoQuincenal")
        val vales = root.optDouble("valesDespensa", defaults.valesDespensa)
        val valesNota = root.optString("valesNota", defaults.valesNota)
        val gastosFijos = deserializeExpenses(root.getJSONArray("gastosFijos"))
        val aguinaldo = root.getDouble("aguinaldo")
        val gastosAguinaldo = deserializeExpenses(root.getJSONArray("gastosAguinaldo"))
        val ekt1 = deserializeFixedDebt(root.getJSONObject("prestamoEkt1"), defaults.prestamoEkt1)
        val ekt2 = deserializeFixedDebt(root.getJSONObject("prestamoEkt2"), defaults.prestamoEkt2)
        val tvDidi = deserializeFixedDebt(root.getJSONObject("tvDidi"), defaults.tvDidi)
        val azteca = deserializeAzteca(root.getJSONObject("bancoAzteca"))

        FinanceState(
            sueldoQuincenal = sueldo.coerceAtLeast(0.0),
            valesDespensa = vales.coerceAtLeast(0.0),
            valesNota = valesNota,
            gastosFijos = gastosFijos,
            aguinaldo = aguinaldo.coerceAtLeast(0.0),
            gastosAguinaldo = gastosAguinaldo,
            prestamoEkt1 = ekt1,
            prestamoEkt2 = ekt2,
            tvDidi = tvDidi,
            bancoAzteca = azteca
        )
    }

    fun buildAiContextJson(state: FinanceState): String {
        val quincenaObj = JSONObject().apply {
            put("sueldoNetoEfectivoMXN", round2(state.sueldoQuincenal))
            put("valesDespensaMXN_noSumadoEfectivo", round2(state.valesDespensa))
            put("totalGastosFijosQuincenalesMXN", round2(state.totalGastosFijos))
            put("totalDeudasQuincenalesActivasMXN", round2(state.totalDeudasQuincenales))
            put("totalGastosMasDeudasMXN", round2(state.totalEgresosQuincenales))
            put("sobranteLibreQuincenalMXN", round2(state.sobranteQuincenal))
        }

        val aguinaldoObj = JSONObject().apply {
            put("montoAguinaldoMXN", round2(state.aguinaldo))
            put("totalCompromisosNavidadMXN", round2(state.totalGastosAguinaldo))
            put("sobranteAguinaldoMXN", round2(state.sobranteAguinaldo))
        }

        val comprasActivasArray = JSONArray().apply {
            state.bancoAzteca.comprasActivas.forEach { c ->
                put(JSONObject().apply {
                    put("numero", c.id)
                    put("montoMXN", round2(c.monto))
                    put("semanasPagadas", c.semanasPagadas)
                    put("totalSemanas", c.totalSemanas)
                    put("sinComisionMayor13Sem", c.sinComision)
                })
            }
        }

        val deudasObj = JSONObject().apply {
            put("prestamoEkt1", JSONObject().apply {
                put("nota", state.prestamoEkt1.subtitulo)
                put("saldoMXN", round2(state.prestamoEkt1.saldo))
                put("semanasPagadas", state.prestamoEkt1.pagosRealizados)
                put("totalSemanas", state.prestamoEkt1.totalPagos)
                put("pagoSemanalMXN", round2(state.prestamoEkt1.pagoPorUnidad))
                put("pagoQuincenalMXN", round2(state.prestamoEkt1.pagoQuincenalActivo))
                put("liquidada", state.prestamoEkt1.liquidada)
            })
            put("prestamoEkt2", JSONObject().apply {
                put("saldoLiquidarMXN", round2(state.prestamoEkt2.saldo))
                put("semanasPagadas", state.prestamoEkt2.pagosRealizados)
                put("totalSemanas", state.prestamoEkt2.totalPagos)
                put("pagoSemanalMXN", round2(state.prestamoEkt2.pagoPorUnidad))
                put("pagoQuincenalMXN", round2(state.prestamoEkt2.pagoQuincenalActivo))
                put("liquidada", state.prestamoEkt2.liquidada)
            })
            put("tvDidiCardMSI", JSONObject().apply {
                put("tasaInteres", "0% MSI")
                put("saldoMXN", round2(state.tvDidi.saldo))
                put("mesesPagados", state.tvDidi.pagosRealizados)
                put("totalMeses", state.tvDidi.totalPagos)
                put("pagoMensualMXN", round2(state.tvDidi.pagoPorUnidad))
                put("pagoQuincenalMXN", round2(state.tvDidi.pagoQuincenalActivo))
                put("liquidada", state.tvDidi.liquidada)
            })
            put("tarjetaBancoAzteca", JSONObject().apply {
                put("pagoSemanalActualMXN", round2(state.bancoAzteca.pagoSemanal))
                put("pagoQuincenalActualMXN", round2(state.bancoAzteca.pagoQuincenalActivo))
                put("saldoTotalComprasActivasMXN", round2(state.bancoAzteca.saldoActivoTotal))
                put("liquidada", state.bancoAzteca.estaLiquidada)
                put("cantidadComprasActivas", state.bancoAzteca.comprasActivas.size)
                put("cantidadSinComisionMayor13Sem", state.bancoAzteca.comprasSinComisionActivas.size)
                put("comprasActivas", comprasActivasArray)
            })
        }

        return JSONObject().apply {
            put("moneda", "MXN")
            put("quincena", quincenaObj)
            put("aguinaldo", aguinaldoObj)
            put("deudas", deudasObj)
        }.toString(2)
    }

    private fun serializeExpenses(list: List<ExpenseItem>): JSONArray {
        return JSONArray().apply {
            list.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("nombre", item.nombre)
                    put("monto", item.monto)
                    put("nota", item.nota)
                    put("pagado", item.pagado)
                })
            }
        }
    }

    private fun deserializeExpenses(array: JSONArray): List<ExpenseItem> {
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ExpenseItem(
                id = obj.getString("id"),
                nombre = obj.getString("nombre"),
                monto = obj.getDouble("monto").coerceAtLeast(0.0),
                nota = obj.optString("nota", ""),
                pagado = obj.optBoolean("pagado", false)
            )
        }
    }

    private fun serializeFixedDebt(debt: FixedDebt): JSONObject {
        return JSONObject().apply {
            put("id", debt.id)
            put("nombre", debt.nombre)
            put("subtitulo", debt.subtitulo)
            put("saldo", debt.saldo)
            put("pagosRealizados", debt.pagosRealizados)
            put("totalPagos", debt.totalPagos)
            put("unidad", debt.unidad)
            put("pagoPorUnidad", debt.pagoPorUnidad)
            put("pagoQuincenal", debt.pagoQuincenal)
            put("esCeroInteres", debt.esCeroInteres)
            put("liquidada", debt.liquidada)
        }
    }

    private fun deserializeFixedDebt(obj: JSONObject, fallback: FixedDebt): FixedDebt {
        return FixedDebt(
            id = obj.optString("id", fallback.id),
            nombre = obj.optString("nombre", fallback.nombre),
            subtitulo = obj.optString("subtitulo", fallback.subtitulo),
            saldo = obj.getDouble("saldo").coerceAtLeast(0.0),
            pagosRealizados = obj.getInt("pagosRealizados").coerceAtLeast(0),
            totalPagos = obj.getInt("totalPagos").coerceAtLeast(1),
            unidad = obj.optString("unidad", fallback.unidad),
            pagoPorUnidad = obj.getDouble("pagoPorUnidad").coerceAtLeast(0.0),
            pagoQuincenal = obj.getDouble("pagoQuincenal").coerceAtLeast(0.0),
            esCeroInteres = obj.optBoolean("esCeroInteres", fallback.esCeroInteres),
            liquidada = obj.optBoolean("liquidada", false)
        )
    }

    private fun serializeAzteca(azteca: AztecaCardDebt): JSONObject {
        return JSONObject().apply {
            put("pagoSemanal", azteca.pagoSemanal)
            put("liquidadaManual", azteca.liquidadaManual)
            put("compras", JSONArray().apply {
                azteca.compras.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("monto", c.monto)
                        put("semanasPagadas", c.semanasPagadas)
                        put("totalSemanas", c.totalSemanas)
                        put("liquidada", c.liquidada)
                    })
                }
            })
        }
    }

    private fun deserializeAzteca(obj: JSONObject): AztecaCardDebt {
        val comprasArray = obj.getJSONArray("compras")
        val compras = (0 until comprasArray.length()).map { i ->
            val c = comprasArray.getJSONObject(i)
            AztecaPurchase(
                id = c.getInt("id"),
                monto = c.getDouble("monto").coerceAtLeast(0.0),
                semanasPagadas = c.getInt("semanasPagadas").coerceAtLeast(0),
                totalSemanas = c.optInt("totalSemanas", 100).coerceAtLeast(1),
                liquidada = c.optBoolean("liquidada", false)
            )
        }
        return AztecaCardDebt(
            pagoSemanal = obj.getDouble("pagoSemanal").coerceAtLeast(0.0),
            liquidadaManual = obj.optBoolean("liquidadaManual", false),
            compras = compras
        )
    }

    private fun round2(value: Double): Double {
        return String.format(Locale.US, "%.2f", value).toDouble()
    }
}
