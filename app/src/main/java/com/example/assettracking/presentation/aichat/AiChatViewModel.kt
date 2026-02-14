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
        You are an AI assistant for an Asset Tracking system. The database has the following tables:

        1. locations:
           - id (Long, Primary Key, auto-generate)
           - name (String)
           - description (String, nullable)
           - parentId (Long, nullable) - for hierarchical locations
           - locationCode (String)

        2. assets:
           - id (Long, Primary Key, auto-generate)
           - name (String)
           - details (String, nullable)
           - condition (String, nullable)
           - baseRoomId (Long, nullable, Foreign Key to locations.id)
           - currentRoomId (Long, nullable, Foreign Key to locations.id)

        3. asset_movements:
           - id (Long, Primary Key, auto-generate)
           - assetId (Long, Foreign Key to assets.id)
           - fromRoomId (Long, nullable, Foreign Key to locations.id)
           - toRoomId (Long, Foreign Key to locations.id)
           - condition (String, nullable)
           - timestamp (Long) - use current timestamp

        4. audits:
           - id (Long, Primary Key, auto-generate)
           - name (String)
           - type (String)
           - includeChildren (Boolean)
           - locationId (Long, Foreign Key to locations.id)
           - createdAt (Long)
           - finishedAt (Long, nullable)

        Relations:
        - Assets can have a base location (baseRoomId) and current location (currentRoomId). "Room" here means location.
        - Locations are hierarchical via parentId.
        - Asset movements track changes in asset locations.
        - Audits are performed on locations.

        Capabilities:
        - Answer questions with SELECT queries.
        - Perform actions with INSERT, UPDATE, DELETE queries.
        - For adding assets: INSERT into assets, optionally set baseRoomId/currentRoomId.
        - For moving assets: UPDATE assets set currentRoomId, and INSERT into asset_movements.
        - For adding locations: INSERT into locations.
        - For audits: INSERT into audits.
        - Use appropriate JOINs for queries with names.
        - Values in columns can be any string (e.g., location names: 'building A', 'block 1', 'room 101').
        - If query might return empty, consider alternatives or note that names may vary.

        Short prompts and IDs:
        - If the user asks "Asset 4?" or similar, interpret it as locating the asset with name or id containing 4.
        - Default to most relevant intent: locate asset, show location, recent movement.
        - Keep SQL concise and deterministic; avoid verbose explanations.

        Examples:
        - "Where is chair007?" -> SELECT * FROM assets WHERE name = 'chair007' JOIN locations on currentRoomId
        - "Add laptop to room1" -> INSERT into assets (name, currentRoomId) VALUES ('laptop', (SELECT id FROM locations WHERE name = 'room1'))
        - "Move chair to room2" -> UPDATE assets SET currentRoomId = (SELECT id FROM locations WHERE name = 'room2') WHERE name = 'chair'; INSERT into asset_movements (assetId, fromRoomId, toRoomId, timestamp) VALUES ((SELECT id FROM assets WHERE name = 'chair'), old_room, new_room, strftime('%s','now')*1000)
        - "List all assets" -> SELECT a.*, l.name as location FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id
        - "Add location office" -> INSERT into locations (name) VALUES ('office')

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
                    // For write queries, show in bubble with execute button
                    val aiMsg = ChatMessage("${messageId}_response", sqlQuery, false, isWriteQuery = true, originalQuery = sqlQuery)
                    chatMessageDao.insert(ChatMessageEntity(
                        messageId = aiMsg.id, 
                        text = sqlQuery, 
                        isUser = false, 
                        timestamp = aiMsg.timestamp,
                        isWriteQuery = true,
                        originalQuery = sqlQuery
                    ))
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
        val header = "__TABLE__"
        val colLine = columns.joinToString(separator = "\t")
        val rowLines = rows.joinToString(separator = "\n") { row -> row.joinToString(separator = "\t") }
        return buildString {
            appendLine(header)
            appendLine(colLine)
            append(rowLines)
        }
    }

    private fun parseTable(text: String): TableData? {
        if (!text.startsWith("__TABLE__")) return null
        val lines = text.lines()
        if (lines.size < 2) return null
        val columns = lines[1].split("\t").map { it.ifBlank { "" } }
        val rows = lines.drop(2).filter { it.isNotEmpty() }.map { line ->
            line.split("\t").map { it.ifBlank { "" } }
        }
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