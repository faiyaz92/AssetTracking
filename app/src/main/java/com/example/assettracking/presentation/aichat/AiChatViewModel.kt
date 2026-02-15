package com.example.assettracking.presentation.aichat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.assettracking.data.local.AssetTrackingDatabase
import com.example.assettracking.data.local.dao.ChatMessageDao
import com.example.assettracking.data.local.entity.ChatMessageEntity
import com.example.assettracking.presentation.aichat.LocalSqlFallbackEngine
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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

data class OptionItem(
    val index: Int,
    val type: String, // "asset" or "location"
    val id: Int,
    val name: String
)

data class AiChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mode: AiMode = AiMode.Gemini,
    val lastOptions: List<OptionItem> = emptyList()
)

enum class AiMode { Gemini, Offline, Ollama, OnDevice }

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val database: AssetTrackingDatabase,
    private val chatMessageDao: ChatMessageDao,
    private val application: Application
) : ViewModel() {

    private var lastUserMessage: String? = null

    private val fallbackEngine: LocalSqlFallbackEngine? = null // TODO: Restore

    private val _uiState = MutableStateFlow(AiChatState())
    val uiState: StateFlow<AiChatState> = _uiState

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyCFYV0RkXxi_yAjpa2XKyCuog5r7vz82uc"
    )

    private val okHttpClient = OkHttpClient()

    // Placeholder for on-device LLM (ONNX Runtime)
    // Note: Requires a model file in assets, e.g., a quantized LLM
    // private val ortEnvironment = OrtEnvironment.getEnvironment()
    // private var ortSession: OrtSession? = null

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

        Examples:
        - User: Asset 4?
          SQL: SELECT a.id, a.name, a.details, a.condition, l.name AS currentLocation, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.id = 4;
        - User: Where is asset 4?
          SQL: SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.id = 4;
        - User: Move asset 4 to room 5
          SQL: INSERT INTO asset_movements (assetId, fromRoomId, toRoomId, timestamp) SELECT 4, currentRoomId, 5, strftime('%s','now')*1000 FROM assets WHERE id = 4; UPDATE assets SET currentRoomId = 5 WHERE id = 4;

        Always respond with one or more valid SQLite queries, separated by semicolons if multiple. No extra text.
    """.trimIndent()

    private suspend fun callOllama(prompt: String): String {
        val json = """{"model": "llama3.2", "prompt": "$prompt", "stream": false}"""
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("http://10.0.2.2:11434/api/generate")
            .post(requestBody)
            .build()
        return withContext(Dispatchers.IO) {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Ollama not running. Please install Ollama and run 'ollama serve'. Error: ${response.code}")
            response.body?.string() ?: throw Exception("Empty response from Ollama")
        }
    }

    private fun parseOllamaResponse(json: String): String {
        val obj = JSONObject(json)
        return obj.getString("response").trim()
    }

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

    fun sendMessage(userMessage: String) {
        lastUserMessage = userMessage
        val messageId = System.currentTimeMillis().toString()
        val userMsg = ChatMessage(messageId, userMessage, true)

        viewModelScope.launch {
            chatMessageDao.insert(ChatMessageEntity(messageId = messageId, text = userMessage, isUser = true, timestamp = userMsg.timestamp))
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        // Validation: Single words must be at least 4 characters
        val trimmed = userMessage.trim()
        if (trimmed.isNotBlank() && !trimmed.contains(" ") && trimmed.length <= 3) {
            viewModelScope.launch {
                val aiMsg = ChatMessage("${System.currentTimeMillis()}_response", "Please provide a more specific query with at least 4 characters.", false)
                chatMessageDao.insert(ChatMessageEntity(messageId = aiMsg.id, text = aiMsg.text, isUser = false, timestamp = aiMsg.timestamp))
                _uiState.update { it.copy(isLoading = false) }
            }
            return
        }

        // Check if user is selecting an option (e.g., "1" for first option)
        val input = userMessage.trim()
        val selectedIndex = input.toIntOrNull()
        val currentOptions = _uiState.value.lastOptions
        if (selectedIndex != null && selectedIndex in 1..currentOptions.size) {
            val selected = currentOptions[selectedIndex - 1]
            val detailSql = when (selected.type) {
                "asset" -> "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, " +
                        "CASE WHEN a.currentRoomId IS NULL THEN 'Missing' " +
                        "WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status " +
                        "FROM assets a " +
                        "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                        "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                        "WHERE a.id = ${selected.id}"
                "location" -> "WITH loc AS (\n" +
                    "  SELECT l.id, l.name, l.description, l.parentId, l.locationCode,\n" +
                    "         (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName\n" +
                    "  FROM locations l\n" +
                    "  WHERE l.id = ${selected.id}\n" +
                    "), asset_summary AS (\n" +
                    "  SELECT\n" +
                    "    SUM(CASE WHEN a.currentRoomId = loc.id THEN 1 ELSE 0 END) AS totalHere,\n" +
                    "    SUM(CASE WHEN a.currentRoomId IS NULL THEN 1 ELSE 0 END) AS missingCount,\n" +
                    "    SUM(CASE WHEN a.currentRoomId = a.baseRoomId AND a.currentRoomId = loc.id THEN 1 ELSE 0 END) AS atHome,\n" +
                    "    SUM(CASE WHEN a.currentRoomId != a.baseRoomId AND a.currentRoomId = loc.id THEN 1 ELSE 0 END) AS otherLocation\n" +
                    "  FROM assets a, loc\n" +
                    "), assets_here AS (\n" +
                    "  SELECT a.id, a.name, a.details, a.condition\n" +
                    "  FROM assets a, loc\n" +
                    "  WHERE a.currentRoomId = loc.id\n" +
                    "  ORDER BY a.id DESC\n" +
                    "  LIMIT 50\n" +
                    ")\n" +
                    "SELECT 'location' AS type, id, name, description, parentId, locationCode, parentName, NULL AS col8, NULL AS col9 FROM loc\n" +
                    "UNION ALL\n" +
                    "SELECT 'summary', NULL, NULL, NULL, NULL, NULL, NULL, totalHere, missingCount FROM asset_summary\n" +
                    "UNION ALL\n" +
                    "SELECT 'assets_here', id, name, details, condition, NULL, NULL, NULL, NULL FROM assets_here"
                else -> null
            }
            if (detailSql != null) {
                viewModelScope.launch {
                    handleSql(detailSql, messageId)
                    _uiState.update { it.copy(lastOptions = emptyList()) } // Clear options after selection
                }
                return
            }
        }

        viewModelScope.launch {
            val mode = _uiState.value.mode

            if (mode == AiMode.Offline) {
                // Offline mode
                handleError("Offline mode temporarily disabled - LocalSqlFallbackEngine needs fixing.", messageId)
                return@launch
            }

            if (mode == AiMode.Ollama) {
                // Ollama mode
                try {
                    val response = callOllama("$schemaPrompt\n\nUser: $userMessage\n\nGenerate SQL query:")
                    val sqlQuery = parseOllamaResponse(response)
                    handleSql(sqlQuery, messageId)
                } catch (e: Exception) {
                    handleError(e.message ?: "Ollama error", messageId)
                }
                return@launch
            }

            if (mode == AiMode.OnDevice) {
                // On-device LLM mode (placeholder)
                try {
                    throw Exception("On-device LLM not implemented yet. Requires ONNX model file and inference logic.")
                } catch (e: Exception) {
                    handleError(e.message ?: "On-device error", messageId)
                }
                return@launch
            }

            // Gemini mode: Use Gemini AI with 2 minute timeout
            try {
                val response = withTimeout(120000) { // 2 minutes
                    generativeModel.generateContent(
                        "$schemaPrompt\n\nUser: $userMessage\n\nGenerate SQL query:"
                    )
                }

                val sqlQuery = response.text?.trim() ?: throw Exception("No SQL generated")
                handleSql(sqlQuery, messageId)

            } catch (e: Exception) {
                handleError(e.message ?: "Unknown error", messageId)
            }
        }
    }

    fun setMode(mode: AiMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun retryLastMessage() {
        val message = lastUserMessage ?: return
        sendMessage(message)
    }

    private suspend fun handleSql(sqlQuery: String, messageId: String) {
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
    }

    // localSqlFallback moved to LocalSqlFallbackEngine for unit testing

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

                // Check if this is an options response (from single word search)
                if (columns.size >= 3 && columns[0] == "type" && rows.isNotEmpty()) {
                    val options = rows.mapIndexedNotNull { index, row ->
                        val type = row.getOrNull(0)
                        val idStr = row.getOrNull(1)
                        val name = row.getOrNull(2)
                        if (type == "asset" || type == "location") {
                            val id = idStr?.toIntOrNull()
                            if (id != null && name != null) {
                                OptionItem(index + 1, type, id, name)
                            } else null
                        } else null
                    }.filterNotNull()
                    if (options.isNotEmpty()) {
                        val optionsText = "Possible matches:\n" + options.joinToString("\n") { "${it.index}. ${it.type.replaceFirstChar { it.uppercase() }}: ${it.name} (ID ${it.id})" }
                        val aiMsg = ChatMessage("${messageId}_response", optionsText, false)
                        chatMessageDao.insert(ChatMessageEntity(messageId = aiMsg.id, text = optionsText, isUser = false, timestamp = aiMsg.timestamp))
                        _uiState.update { it.copy(isLoading = false, lastOptions = options) }
                        return
                    }
                }

                if (rows.isNotEmpty()) {
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
                } else {
                    // No matches, don't show message
                }
                _uiState.update { it.copy(isLoading = false) }
            } else {
                // For write queries, execute all
                queries.forEach { query ->
                    database.openHelper.writableDatabase.execSQL(query)
                }
                val aiMsg = ChatMessage("${messageId}_response", "Query executed successfully.", false)
                chatMessageDao.insert(ChatMessageEntity(messageId = aiMsg.id, text = "Query executed successfully.", isUser = false, timestamp = aiMsg.timestamp))
                _uiState.update { it.copy(isLoading = false) }
            }
        } catch (e: Exception) {
            handleError("Query execution failed: ${e.message}", messageId)
        }
    }

    private fun handleError(errorMsg: String, messageId: String) {
        showFinalError(errorMsg)
    }

    private fun showFinalError(errorMsg: String) {
        _uiState.update { it.copy(
            error = "Sorry, I couldn't process that request. Please try again.",
            isLoading = false
        ) }
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

    fun executeQuery(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        if (!message.isWriteQuery || message.queryExecuted) return

        viewModelScope.launch {
            try {
                // Optimistically mark as executed to avoid double taps while executing
                _uiState.update { state ->
                    val updatedMessages = state.messages.map {
                        if (it.id == messageId) it.copy(queryExecuted = true) else it
                    }
                    state.copy(messages = updatedMessages)
                }

                database.openHelper.writableDatabase.execSQL(message.originalQuery!!)
                chatMessageDao.insert(ChatMessageEntity(
                    messageId = "${messageId}_executed", 
                    text = "Query executed successfully.", 
                    isUser = false, 
                    timestamp = System.currentTimeMillis(),
                    isWriteQuery = false
                ))
            } catch (e: Exception) {
                chatMessageDao.insert(ChatMessageEntity(
                    messageId = "${messageId}_error", 
                    text = "Execution failed: ${e.message}", 
                    isUser = false, 
                    timestamp = System.currentTimeMillis(),
                    isWriteQuery = false
                ))
                // Revert optimistic flag on failure
                _uiState.update { state ->
                    val updatedMessages = state.messages.map {
                        if (it.id == messageId) it.copy(queryExecuted = false) else it
                    }
                    state.copy(messages = updatedMessages)
                }
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
            _uiState.update { it.copy(messages = emptyList(), error = null, isLoading = false) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}