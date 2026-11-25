package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.data.FinanceState
import com.example.ui.AdvisorUiState
import com.example.ui.components.BankingHeroCard
import com.example.ui.components.formatMxn
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyles

@Composable
fun AsesorScreen(
    financeState: FinanceState,
    advisorState: AdvisorUiState,
    contextJsonProvider: () -> String,
    onAnalyzeSituation: () -> Unit,
    onAskQuestion: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    var inputText by remember { mutableStateOf("") }
    var showJsonPreview by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(advisorState.messages.size, advisorState.streamingResponse.length) {
        val totalItems = advisorState.messages.size + if (advisorState.isLoading) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems + 1)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Advisor Hero Card + Live Synced Context Summary
                item(key = "advisor_hero") {
                    BankingHeroCard(
                        startColor = financeColors.heroStart,
                        endColor = Color(0xFF115E59)
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
                                    imageVector = Icons.Outlined.Psychology,
                                    contentDescription = null,
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Asesor Financiero IA",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                            if (advisorState.messages.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearChat,
                                    modifier = Modifier.testTag("clear_advisor_chat_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = "Limpiar conversación",
                                        tint = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Analiza en tiempo real tus quincenas, aguinaldo y deudas en MXN priorizando no adelantar 0% MSI, liquidar Azteca >13 sem sin comisión y planear EKT 2.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live numeric context chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiveContextPill(
                                label = "Quincena libre",
                                value = formatMxn(financeState.sobranteQuincenal),
                                modifier = Modifier.weight(1f)
                            )
                            LiveContextPill(
                                label = "Aguinaldo libre",
                                value = formatMxn(financeState.sobranteAguinaldo),
                                modifier = Modifier.weight(1f)
                            )
                            LiveContextPill(
                                label = "Azteca >13 sem",
                                value = "${financeState.bancoAzteca.comprasSinComisionActivas.size} compras",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onAnalyzeSituation,
                            enabled = !advisorState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = financeColors.heroStart
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("advisor_analyze_btn")
                        ) {
                            if (advisorState.isLoading && advisorState.streamingResponse.isEmpty()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = financeColors.heroStart
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Analizar mi situación y recomendar qué hacer",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = { showJsonPreview = !showJsonPreview },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.85f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (showJsonPreview) "Ocultar datos JSON sincronizados" else "Ver datos JSON sincronizados",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        AnimatedVisibility(visible = showJsonPreview) {
                            val jsonText = remember(financeState) { contextJsonProvider() }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Text(
                                    text = jsonText.take(1200) + if (jsonText.length > 1200) "\n..." else "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Quick suggestion chips
                item(key = "quick_prompts") {
                    val suggestions = listOf(
                        "¿Qué compras Azteca liquido con el aguinaldo?",
                        "¿Cuándo conviene liquidar EKT 2 de golpe?",
                        "Plan para mis próximas 4 quincenas",
                        "¿Por qué no adelantar TV DiDi MSI?"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { prompt ->
                            AssistChip(
                                onClick = { onAskQuestion(prompt) },
                                enabled = !advisorState.isLoading,
                                label = { Text(prompt, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }

                // 3. Empty state helper
                if (advisorState.messages.isEmpty() && !advisorState.isLoading && advisorState.errorMessage == null) {
                    item(key = "advisor_empty_hint") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, financeColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Tu diagnóstico se calcula con tus cifras actuales",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Toca \"Analizar mi situación y recomendar qué hacer\" o escribe una pregunta abajo. Cada consulta envía automáticamente tu sueldo, sobrante quincenal, aguinaldo y tus compras activas de Banco Azteca.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Chat history messages
                items(advisorState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(message = message)
                }

                // 5. Streaming response bubble
                if (advisorState.isLoading) {
                    item(key = "streaming_bubble") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = if (advisorState.streamingResponse.isEmpty()) {
                                            "Analizando tus finanzas..."
                                        } else {
                                            "Asesor IA escribiendo..."
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (advisorState.streamingResponse.isNotEmpty()) {
                                    Text(
                                        text = cleanMarkdownDisplay(advisorState.streamingResponse),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Real error state (transparent propagation)
                if (advisorState.errorMessage != null) {
                    item(key = "advisor_error") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = financeColors.negativeContainer,
                            border = BorderStroke(1.dp, financeColors.negative.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("advisor_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = financeColors.onNegativeContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Error al consultar al Asesor IA",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = financeColors.onNegativeContainer
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = advisorState.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = financeColors.onNegativeContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(6.dp)) }
            }

            // Bottom Mini-Chat Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(0.5.dp, financeColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Haz una pregunta a tu asesor...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !advisorState.isLoading) {
                                    onAskQuestion(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("advisor_chat_input")
                    )
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !advisorState.isLoading) {
                                onAskQuestion(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !advisorState.isLoading,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("advisor_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Enviar pregunta"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveContextPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.13f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MoneyTextStyles.CaptionAmount.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val financeColors = LocalFinanceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = if (message.isUser) null else BorderStroke(1.dp, financeColors.cardBorder),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (!message.isUser) {
                    Text(
                        text = "Asesor Financiero IA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = if (message.isUser) message.text else cleanMarkdownDisplay(message.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

private fun cleanMarkdownDisplay(raw: String): String {
    return raw
        .replace("### ", "")
        .replace("## ", "")
        .replace("# ", "")
        .replace("**", "")
        .replace("__", "")
}
