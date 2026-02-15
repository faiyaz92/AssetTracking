package com.example.assettracking.presentation.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1E40AF), // Deep Blue
                                Color(0xFF06B6D4)  // Teal
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Asset Tracing AI",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        ModeMenu(current = state.mode, onSelect = { viewModel.setMode(it) })
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.Default.Clear, "Clear Chat", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { message ->
                    MessageBubble(message, viewModel::executeQuery, viewModel::undoQuery)
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

            }

            if (state.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (state.error!!.contains("continue")) {
                                TextButton(onClick = { viewModel.dismissError() }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { viewModel.continueChat() }) {
                                    Text("Continue")
                                }
                            } else {
                                TextButton(onClick = { viewModel.dismissError() }) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Ask about assets...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}
@Composable
private fun ModeMenu(current: AiMode, onSelect: (AiMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = if (current == AiMode.Gemini) "Gemini" else "Offline",
                color = Color.White
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Gemini") },
                onClick = {
                    expanded = false
                    onSelect(AiMode.Gemini)
                }
            )
            DropdownMenuItem(
                text = { Text("Offline") },
                onClick = {
                    expanded = false
                    onSelect(AiMode.Offline)
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onExecute: (String) -> Unit, onUndo: (String) -> Unit) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        val bubbleModifier = if (message.table != null) Modifier.fillMaxWidth() else Modifier.widthIn(max = 320.dp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = bubbleModifier
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontFamily = if (!isUser) FontFamily.Monospace else null
                )

                message.table?.let { table ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TableCard(table = table, textColor = textColor)
                }

                if (message.isWriteQuery && !message.queryExecuted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onExecute(message.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Execute")
                    }
                } else if (message.isWriteQuery && message.queryExecuted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onUndo(message.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Undo")
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCard(table: TableData, textColor: Color) {
    val scroll = rememberScrollState()
    val headerBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val zebraA = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    val zebraB = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f)

    Column(
        modifier = Modifier
            .horizontalScroll(scroll)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            table.columns.forEach { col ->
                Box(modifier = Modifier.widthIn(min = 120.dp, max = 220.dp)) {
                    Text(
                        col,
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        if (table.rows.isEmpty()) {
            Text("No rows", color = textColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
        } else {
            table.rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) zebraA else zebraB, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEachIndexed { colIndex, cell ->
                        val columnName = table.columns.getOrNull(colIndex)?.lowercase().orEmpty()
                        Box(modifier = Modifier.widthIn(min = 120.dp, max = 220.dp)) {
                            when {
                                isStatusColumn(columnName) -> StatusBadge(cell)
                                isConditionColumn(columnName) -> ConditionBadge(cell)
                                else -> Text(
                                    cell,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
    val (bg, fg) = statusColors(text)
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ConditionBadge(text: String) {
    val tone = when (text.lowercase()) {
        "good", "ok", "fine", "working" -> Color(0xFF10B981)
        "fair", "worn" -> Color(0xFFF59E0B)
        "bad", "broken", "missing" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = tone.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = tone,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun isStatusColumn(columnName: String): Boolean {
    return columnName.contains("status") || columnName.contains("state")
}

private fun isConditionColumn(columnName: String): Boolean {
    return columnName.contains("condition")
}

@Composable
private fun statusColors(status: String): Pair<Color, Color> {
    return when (status.lowercase()) {
        "at home" -> Color(0xFF10B981).copy(alpha = 0.18f) to Color(0xFF0F766E)
        "at other location", "other location" -> Color(0xFFF59E0B).copy(alpha = 0.18f) to Color(0xFF92400E)
        "missing" -> Color(0xFFEF4444).copy(alpha = 0.18f) to Color(0xFFB91C1C)
        "not assigned", "unassigned" -> Color(0xFF3B82F6).copy(alpha = 0.18f) to Color(0xFF1D4ED8)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) to MaterialTheme.colorScheme.primary
    }
}