package com.example.data

import kotlin.math.max

data class ExpenseItem(
    val id: String,
    val nombre: String,
    val monto: Double,
    val nota: String = "",
    val pagado: Boolean = false
)

data class FixedDebt(
    val id: String,
    val nombre: String,
    val subtitulo: String,
    val saldo: Double,
    val pagosRealizados: Int,
    val totalPagos: Int,
    val unidad: String, // "semanas" o "meses"
    val pagoPorUnidad: Double,
    val pagoQuincenal: Double,
    val esCeroInteres: Boolean = false,
    val liquidada: Boolean = false
) {
    val pagosRestantes: Int
        get() = max(0, totalPagos - pagosRealizados)

    val progresoFraccion: Float
        get() = if (totalPagos > 0) (pagosRealizados.toFloat() / totalPagos.toFloat()).coerceIn(0f, 1f) else 1f

    val pagoQuincenalActivo: Double
        get() = if (liquidada) 0.0 else pagoQuincenal
}

data class AztecaPurchase(
    val id: Int,
    val monto: Double,
    val semanasPagadas: Int,
    val totalSemanas: Int = 100,
    val liquidada: Boolean = false
) {
    val sinComision: Boolean
        get() = semanasPagadas > 13

    val progresoFraccion: Float
        get() = if (totalSemanas > 0) (semanasPagadas.toFloat() / totalSemanas.toFloat()).coerceIn(0f, 1f) else 1f
}

data class AztecaCardDebt(
    val pagoSemanal: Double = 166.0,
    val liquidadaManual: Boolean = false,
    val compras: List<AztecaPurchase>
) {
    val comprasActivas: List<AztecaPurchase>
        get() = compras.filter { !it.liquidada }

    val comprasSinComisionActivas: List<AztecaPurchase>
        get() = comprasActivas.filter { it.sinComision }

    val comprasEsperarActivas: List<AztecaPurchase>
        get() = comprasActivas.filter { !it.sinComision }

    val saldoActivoTotal: Double
        get() = comprasActivas.sumOf { it.monto }

    val estaLiquidada: Boolean
        get() = liquidadaManual || comprasActivas.isEmpty()

    val pagoQuincenal: Double
        get() = pagoSemanal * 2.0

    val pagoQuincenalActivo: Double
        get() = if (estaLiquidada) 0.0 else pagoQuincenal
}

data class FinanceState(
    val sueldoQuincenal: Double,
    val valesDespensa: Double,
    val valesNota: String,
    val gastosFijos: List<ExpenseItem>,
    val aguinaldo: Double,
    val gastosAguinaldo: List<ExpenseItem>,
    val prestamoEkt1: FixedDebt,
    val prestamoEkt2: FixedDebt,
    val tvDidi: FixedDebt,
    val bancoAzteca: AztecaCardDebt
) {
    val totalGastosFijos: Double
        get() = gastosFijos.sumOf { it.monto }

    val totalGastosFijosPagados: Double
        get() = gastosFijos.filter { it.pagado }.sumOf { it.monto }

    val totalDeudasQuincenales: Double
        get() = prestamoEkt1.pagoQuincenalActivo +
            prestamoEkt2.pagoQuincenalActivo +
            tvDidi.pagoQuincenalActivo +
            bancoAzteca.pagoQuincenalActivo

    val totalEgresosQuincenales: Double
        get() = totalGastosFijos + totalDeudasQuincenales

    val sobranteQuincenal: Double
        get() = sueldoQuincenal - totalEgresosQuincenales

    val porcentajeUsadoQuincena: Float
        get() = if (sueldoQuincenal > 0.0) {
            (totalEgresosQuincenales / sueldoQuincenal).toFloat().coerceIn(0f, 1.5f)
        } else {
            if (totalEgresosQuincenales > 0.0) 1f else 0f
        }

    val totalGastosAguinaldo: Double
        get() = gastosAguinaldo.sumOf { it.monto }

    val totalGastosAguinaldoPagados: Double
        get() = gastosAguinaldo.filter { it.pagado }.sumOf { it.monto }

    val sobranteAguinaldo: Double
        get() = aguinaldo - totalGastosAguinaldo

    val saldoTotalDeudasActivas: Double
        get() = (if (prestamoEkt1.liquidada) 0.0 else prestamoEkt1.saldo) +
            (if (prestamoEkt2.liquidada) 0.0 else prestamoEkt2.saldo) +
            (if (tvDidi.liquidada) 0.0 else tvDidi.saldo) +
            (if (bancoAzteca.estaLiquidada) 0.0 else bancoAzteca.saldoActivoTotal)

    companion object {
        const val QUINCENA_BASE = 3680.25
        const val QUINCENA_CON_BONO = 4017.42
    }
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

object DefaultFinanceData {
    fun createDefaultState(): FinanceState = FinanceState(
        sueldoQuincenal = FinanceState.QUINCENA_BASE,
        valesDespensa = 612.73,
        valesNota = "Exclusivo desayuno y cena",
        gastosFijos = listOf(
            ExpenseItem("gf_1", "Comida", 800.00, "$400/sem", false),
            ExpenseItem("gf_2", "Ahorro con hermana", 100.00, "", false),
            ExpenseItem("gf_3", "Camión", 240.00, "$24/día x 10", false),
            ExpenseItem("gf_4", "Taxi", 150.00, "$15/día x 10", false),
            ExpenseItem("gf_5", "Gym", 280.00, "", false),
            ExpenseItem("gf_6", "Spotify (fondo)", 60.00, "", false),
            ExpenseItem("gf_7", "Internet", 125.00, "", false),
            ExpenseItem("gf_8", "Recarga teléfono", 86.00, "", false),
            ExpenseItem("gf_9", "Luz", 50.00, "", false),
            ExpenseItem("gf_10", "Basura", 25.00, "", false)
        ),
        aguinaldo = 11829.90,
        gastosAguinaldo = listOf(
            ExpenseItem("ag_1", "Medicamentos", 1800.00, "", false),
            ExpenseItem("ag_2", "Devolver a mi mamá", 5750.00, "", false),
            ExpenseItem("ag_3", "Regalos Mercado Libre", 800.00, "", false),
            ExpenseItem("ag_4", "Convivio amigas (incluye intercambio)", 1000.00, "", false),
            ExpenseItem("ag_5", "Aportación cena/comida", 600.00, "", false),
            ExpenseItem("ag_6", "Ropa", 500.00, "", false)
        ),
        prestamoEkt1 = FixedDebt(
            id = "ekt_1",
            nombre = "Préstamo EKT 1",
            subtitulo = "Compartido con mamá",
            saldo = 3709.00,
            pagosRealizados = 38,
            totalPagos = 80,
            unidad = "semanas",
            pagoPorUnidad = 50.00,
            pagoQuincenal = 100.00,
            esCeroInteres = false,
            liquidada = false
        ),
        prestamoEkt2 = FixedDebt(
            id = "ekt_2",
            nombre = "Préstamo EKT 2",
            subtitulo = "Saldo para liquidar",
            saldo = 4959.00,
            pagosRealizados = 48,
            totalPagos = 128,
            unidad = "semanas",
            pagoPorUnidad = 100.00,
            pagoQuincenal = 200.00,
            esCeroInteres = false,
            liquidada = false
        ),
        tvDidi = FixedDebt(
            id = "tv_didi",
            nombre = "TV DiDi Card",
            subtitulo = "MSI 0% interés",
            saldo = 1972.00,
            pagosRealizados = 4,
            totalPagos = 12,
            unidad = "meses",
            pagoPorUnidad = 246.50,
            pagoQuincenal = 123.25,
            esCeroInteres = true,
            liquidada = false
        ),
        bancoAzteca = AztecaCardDebt(
            pagoSemanal = 166.00,
            liquidadaManual = false,
            compras = listOf(
                AztecaPurchase(1, 546.0, 22),
                AztecaPurchase(2, 451.0, 24),
                AztecaPurchase(3, 502.0, 31),
                AztecaPurchase(4, 380.0, 22),
                AztecaPurchase(5, 145.0, 11),
                AztecaPurchase(6, 199.0, 3),
                AztecaPurchase(7, 146.0, 10),
                AztecaPurchase(8, 123.0, 11),
                AztecaPurchase(9, 486.0, 28),
                AztecaPurchase(10, 221.0, 8),
                AztecaPurchase(11, 539.0, 14),
                AztecaPurchase(12, 240.0, 3),
                AztecaPurchase(13, 97.0, 1),
                AztecaPurchase(14, 159.0, 16),
                AztecaPurchase(15, 315.0, 25),
                AztecaPurchase(16, 98.0, 4),
                AztecaPurchase(17, 122.0, 3),
                AztecaPurchase(18, 178.0, 10),
                AztecaPurchase(19, 467.0, 28),
                AztecaPurchase(20, 399.0, 12),
                AztecaPurchase(21, 184.0, 3),
                AztecaPurchase(22, 245.0, 11),
                AztecaPurchase(23, 236.0, 8),
                AztecaPurchase(24, 223.0, 6),
                AztecaPurchase(25, 169.0, 10)
            )
        )
    )
}
