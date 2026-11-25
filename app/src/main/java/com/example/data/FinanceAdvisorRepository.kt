package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceAdvisorRepository {

    private val systemPrompt = """
        Eres un asesor financiero personal práctico y directo en México (todos los montos están en pesos mexicanos, MXN).
        Analizas la situación financiera real del usuario a partir del JSON que se te entrega en cada consulta.

        REGLAS Y PRIORIDADES OBLIGATORIAS:
        1. NUNCA recomendar adelantar la TV DiDi Card porque es a 0% de interés (MSI). Ese dinero rinde mucho más liquidando deudas que sí cobran intereses o bajando el pago semanal.
        2. PRIORIZAR recomendar liquidar en sucursal de Banco Azteca las compras individuales activas que YA superaron las 13 semanas pagadas (>13 sem, sin comisión/penalización), indicando exactamente qué números de compra (#) y montos exactos liquidar con el sobrante disponible (aguinaldo y/o quincena) para evitar penalizaciones y reducir el pago semanal. Nunca recomendar liquidar compras con ≤13 semanas salvo advertencia explícita de comisión.
        3. EVALUAR explícitamente cuándo conviene juntar el sobrante libre quincenal (y/o sobrante de aguinaldo) para liquidar de golpe el Préstamo EKT 2 y ahorrar intereses, indicando en cuántas quincenas podría juntarlo según su sobrante actual.
        4. RESPONDER SIEMPRE EN ESPAÑOL, con montos exactos en formato ${'$'}X,XXX.XX MXN y pasos concretos, accionables y ordenados (1, 2, 3...).
        5. Sé claro, empático, conciso y estructurado con títulos cortos y viñetas fáciles de leer en pantalla móvil.
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(systemPrompt)
            }
        )
    }

    fun streamAdvice(
        financialSnapshotJson: String,
        userQuestion: String?,
        recentHistory: List<ChatMessage>
    ): Flow<String> {
        val historyContext = buildString {
            val validHistory = recentHistory.filter { !it.isError }.takeLast(6)
            if (validHistory.isNotEmpty()) {
                appendLine("HISTORIAL RECIENTE DE LA CONVERSACIÓN:")
                validHistory.forEach { msg ->
                    val role = if (msg.isUser) "Usuario" else "Asesor"
                    appendLine("$role: ${msg.text}")
                }
                appendLine()
            }
        }

        val promptText = buildString {
            appendLine("ESTADO FINANCIERO ACTUALIZADO EN TIEMPO REAL (JSON):")
            appendLine(financialSnapshotJson)
            appendLine()
            if (historyContext.isNotBlank()) {
                append(historyContext)
            }
            if (userQuestion.isNullOrBlank()) {
                appendLine("SOLICITUD DEL USUARIO:")
                appendLine(
                    "Analiza mi situación financiera actual completa y dame un plan concreto con montos exactos: " +
                        "1) qué hacer hoy con mi sobrante de aguinaldo (qué compras exactas de Banco Azteca >13 sem liquidar), " +
                        "2) cómo usar mi sobrante quincenal, " +
                        "3) cuándo me conviene liquidar de golpe el Préstamo EKT 2, y " +
                        "4) recordatorio breve sobre TV DiDi MSI y Préstamo EKT 1."
                )
            } else {
                appendLine("PREGUNTA DEL USUARIO:")
                appendLine(userQuestion)
            }
        }

        return model.generateContentStream(promptText).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
