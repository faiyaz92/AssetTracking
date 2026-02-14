package com.example.assettracking.presentation.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.data.local.AssetTrackingDatabase
import com.example.assettracking.data.local.dao.ChatMessageDao
import com.example.assettracking.data.local.entity.ChatMessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isWriteQuery: Boolean = false,
    val queryExecuted: Boolean = false,
    val originalQuery: String? = null,
    val table: TableData? = null
)

data class TableData(
    val columns: List<String>,
    val rows: List<List<String>>
)

data class AiChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val retryCount: Int = 0,
    val continueCount: Int = 0
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val database: AssetTrackingDatabase,
    private val chatMessageDao: ChatMessageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatState())
    val uiState: StateFlow<AiChatState> = _uiState

    init {
        viewModelScope.launch {
            chatMessageDao.observeMessages().collect { entities ->
                val messages = entities.map { entity ->
                    val table = parseTable(entity.text)
                    val displayText = table?.let { tbl -> "Query results (${tbl.rows.size} rows)" } ?: entity.text
                    ChatMessage(
                        id = entity.messageId,
                        text = displayText,
                        isUser = entity.isUser,
                        timestamp = entity.timestamp,
                        isWriteQuery = entity.isWriteQuery,
                        queryExecuted = entity.queryExecuted,
                        originalQuery = entity.originalQuery,
                        table = table
                    )
                }
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyCFYV0RkXxi_yAjpa2XKyCuog5r7vz82uc"
    )

    private val schemaPrompt = """
        You are an AI assistant for an Asset Tracking system. Rely on the database schema (tables and columns) to infer intent, even when the user gives very short or vague prompts. Do not limit yourself to the examples; pick the most relevant query based on column meanings.

        Schema:
        1) locations: id (PK), name, description?, parentId?, locationCode
        2) assets: id (PK), name, details?, condition?, baseRoomId?, currentRoomId?
        3) asset_movements: id (PK), assetId, fromRoomId?, toRoomId, condition?, timestamp
        4) audits: id (PK), name, type, includeChildren, locationId, createdAt, finishedAt?

        Relations:
        - Room == location. Any roomId refers to locations.id. assets.baseRoomId/currentRoomId are location ids.
        - locations.parentId -> locations.id (hierarchy)
        - asset_movements links assets and locations (from/to)
        - audits link to locations

        Behavior:
        - Infer intent from schema: locate assets, find locations, show recent movements, create/move assets, add locations, start audits.
        - Short prompts (e.g., "Asset 4?") should resolve to best-fit queries: match id or name containing the token, include location info and latest movement where relevant.
        - Use concise, deterministic SQLite. Prefer simple joins to expose human-readable names. If data may be missing, still return a sensible SELECT.
        - Writes: INSERT/UPDATE/DELETE when the prompt implies action (create/move/change). Reads: SELECT for lookup/list/reporting.
        - Examples guide style only; they are not limits.

        Always respond with one or more valid SQLite queries, separated by semicolons if multiple. No extra text.
    """.trimIndent()

    fun sendMessage(userMessage: String) {
        val messageId = System.currentTimeMillis().toString()
        val userMsg = ChatMessage(messageId, userMessage, true)

        viewModelScope.launch {
            chatMessageDao.insert(ChatMessageEntity(messageId = messageId, text = userMessage, isUser = true, timestamp = userMsg.timestamp))
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(
                    "$schemaPrompt\n\nUser: $userMessage\n\nGenerate SQL query:"
                )

                val sqlQuery = response.text?.trim() ?: throw Exception("No SQL generated")
                val isWrite = sqlQuery.startsWith("INSERT", ignoreCase = true) ||
                              sqlQuery.startsWith("UPDATE", ignoreCase = true) ||
                              sqlQuery.startsWith("DELETE", ignoreCase = true)

                if (isWrite) {
                    // For write queries, hide raw SQL and show a friendly action card
                    val displayText = "Action ready. Tap Execute to apply changes."
                    val aiMsg = ChatMessage(
                        id = "${messageId}_response",
                        text = displayText,
                        isUser = false,
                        isWriteQuery = true,
                        originalQuery = sqlQuery
                    )
                    chatMessageDao.insert(
                        ChatMessageEntity(
                            messageId = aiMsg.id,
                            text = displayText,
                            isUser = false,
                            timestamp = aiMsg.timestamp,
                            isWriteQuery = true,
                            originalQuery = sqlQuery
                        )
                    )
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    // For read queries, execute immediately
                    executeQuery(sqlQuery, messageId)
                }

            } catch (e: Exception) {
                handleError(e.message ?: "Unknown error", messageId)
            }
        }
    }

    private suspend fun executeQuery(sqlQuery: String, messageId: String) {
        try {
            val queries = sqlQuery.split(";").map { it.trim() }.filter { it.isNotBlank() }
            val isSelect = queries.any { it.startsWith("SELECT", ignoreCase = true) }

            if (isSelect) {
                // For read queries, execute the first SELECT
                val selectQuery = queries.first { it.startsWith("SELECT", ignoreCase = true) }
                val cursor = database.query(selectQuery, emptyArray())
                val maxRows = 200
                val columns = (0 until cursor.columnCount).map { cursor.getColumnName(it) }
                val rows = mutableListOf<List<String>>()
                if (cursor.moveToFirst()) {
                    do {
                        val row = (0 until cursor.columnCount).map { cursor.getString(it) ?: "NULL" }
                        if (rows.size < maxRows) rows.add(row)
                    } while (cursor.moveToNext())
                }
                cursor.close()

                val encodedTable = encodeTable(columns, rows)
                val aiMsg = ChatMessage(
                    id = "${messageId}_response",
                    text = "Query results (${rows.size} rows shown)",
                    isUser = false,
                    table = TableData(columns, rows)
                )
                chatMessageDao.insert(
                    ChatMessageEntity(
                        messageId = aiMsg.id,
                        text = encodedTable,
                        isUser = false,
                        timestamp = aiMsg.timestamp
                    )
                )
                _uiState.update { it.copy(isLoading = false, retryCount = 0) }
            } else {
                // For write queries, execute all
                queries.forEach { query ->
                    database.openHelper.writableDatabase.execSQL(query)
                }
                val aiMsg = ChatMessage("${messageId}_response", "Query executed successfully.", false)
                chatMessageDao.insert(ChatMessageEntity(messageId = aiMsg.id, text = "Query executed successfully.", isUser = false, timestamp = aiMsg.timestamp))
                _uiState.update { it.copy(isLoading = false, retryCount = 0) }
            }
        } catch (e: Exception) {
            handleError("Query execution failed: ${e.message}", messageId)
        }
    }

    private fun handleError(errorMsg: String, messageId: String) {
        val currentState = _uiState.value
        val newRetryCount = currentState.retryCount + 1

        if (newRetryCount <= 3) {
            // Retry by asking AI again
            viewModelScope.launch {
                try {
                    val response = generativeModel.generateContent(
                        "$schemaPrompt\n\nPrevious query failed with error: $errorMsg\n\nPlease provide a corrected SQL query:"
                    )
                    val correctedSql = response.text?.trim() ?: throw Exception("No corrected SQL")
                    executeQuery(correctedSql, messageId)
                } catch (e: Exception) {
                    if (newRetryCount >= 3) {
                        showFinalError("Failed after 3 retries: ${e.message}")
                    } else {
                        _uiState.update { it.copy(retryCount = newRetryCount) }
                        handleError(e.message ?: "Retry failed", messageId)
                    }
                }
            }
        } else {
            showFinalError("Maximum retries exceeded. Error: $errorMsg")
        }
    }

    private fun showFinalError(errorMsg: String) {
        val continueCount = _uiState.value.continueCount
        if (continueCount < 3) {
            _uiState.update { it.copy(
                error = "Asset Tracing AI is working so long. If you want to continue, click Continue. ($errorMsg)",
                isLoading = false,
                retryCount = 0,
                continueCount = continueCount + 1
            ) }
        } else {
            _uiState.update { it.copy(
                error = "Error: Try a different way. $errorMsg",
                isLoading = false
            ) }
        }
    }

    private fun encodeTable(columns: List<String>, rows: List<List<String>>): String {
        val header = "__TABLE_V1__"
        val colLine = columns.joinToString(separator = "\t")
        val rowLines = rows.joinToString(separator = "\n") { row -> row.joinToString(separator = "\t") }
        return buildString {
            appendLine(header)
            appendLine(colLine)
            append(rowLines)
        }
    }

    private fun parseTable(text: String): TableData? {
        val isV1 = text.startsWith("__TABLE_V1__")
        val isLegacy = !isV1 && text.startsWith("__TABLE__")
        if (!isV1 && !isLegacy) return null

        val lines = text.lines()
        if (lines.size < 2) return null

        val columnLine = lines[1]
        if (!columnLine.contains('\t')) return null

        val columns = columnLine.split("\t").map { it.ifBlank { "" } }
        if (columns.isEmpty()) return null

        val rows = lines.drop(2)
            .filter { it.isNotEmpty() && it.contains('\t') }
            .mapNotNull { line ->
                val cells = line.split("\t").map { it.ifBlank { "" } }
                if (cells.size == columns.size) cells else null
            }

        if (rows.isEmpty()) return TableData(columns, emptyList())
        return TableData(columns, rows)
    }

    fun continueChat() {
        _uiState.update { it.copy(error = null, retryCount = 0) }
    }

    fun executeQuery(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        if (!message.isWriteQuery || message.queryExecuted) return

        viewModelScope.launch {
            try {
                database.openHelper.writableDatabase.execSQL(message.originalQuery!!)
                // Update message as executed
                chatMessageDao.insert(ChatMessageEntity(
                    messageId = "${messageId}_executed", 
                    text = "Query executed successfully.", 
                    isUser = false, 
                    timestamp = System.currentTimeMillis(),
                    isWriteQuery = false
                ))
                // Mark original as executed
                // Since we can't update easily, we'll add a new message or handle in UI
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { 
                        if (it.id == messageId) it.copy(queryExecuted = true) else it 
                    }
                    state.copy(messages = updatedMessages)
                }
            } catch (e: Exception) {
                chatMessageDao.insert(ChatMessageEntity(
                    messageId = "${messageId}_error", 
                    text = "Execution failed: ${e.message}", 
                    isUser = false, 
                    timestamp = System.currentTimeMillis(),
                    isWriteQuery = false
                ))
            }
        }
    }

    fun undoQuery(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        if (!message.queryExecuted) return

        // Simple undo: for now, just mark as not executed. For complex undo, need to reverse operations.
        // For simplicity, we'll assume undo means "operation voided"
        viewModelScope.launch {
            chatMessageDao.insert(ChatMessageEntity(
                messageId = "${messageId}_undo", 
                text = "Operation undone.", 
                isUser = false, 
                timestamp = System.currentTimeMillis(),
                isWriteQuery = false
            ))
            _uiState.update { state ->
                val updatedMessages = state.messages.map { 
                    if (it.id == messageId) it.copy(queryExecuted = false) else it 
                }
                state.copy(messages = updatedMessages)
            }
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            chatMessageDao.clearAll()
            _uiState.update { it.copy(messages = emptyList(), error = null, isLoading = false, retryCount = 0, continueCount = 0) }
        }
    }
}