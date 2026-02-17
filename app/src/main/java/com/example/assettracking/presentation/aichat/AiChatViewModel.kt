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
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import java.nio.FloatBuffer

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
    val mode: AiMode = AiMode.OnDevice,
    val lastOptions: List<OptionItem> = emptyList(),
    val onDeviceModel: OnDeviceModel = OnDeviceModel.Gemma
)

enum class AiMode { Gemini, OnDevice }

enum class OnDeviceModel { Gemma, TinyFB, SqlTemplate }

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
        apiKey = "AIzaSyDtuBSiveaMde7GyEcOox3Yo3TG1ySAOYU"
    )

    private val okHttpClient = OkHttpClient()

    // ONNX Runtime for on-device LLM inference
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    private var isModelLoaded = false

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

    private fun loadOnnxModel() {
        try {
            // Try to load model from assets
            val assetManager = application.assets
            val modelBytes = assetManager.open("model.onnx").readBytes()
            ortSession = ortEnvironment.createSession(modelBytes)
            isModelLoaded = true
        } catch (e: Exception) {
            // Model not found or failed to load
            isModelLoaded = false
        }
    }

    private fun runOnnxInference(inputText: String): String {
        if (!isModelLoaded || ortSession == null) {
            return "SELECT * FROM assets LIMIT 5" // Fallback query
        }

        try {
            // Extract user message from the full prompt
            val userMessage = inputText.substringAfter("User: ").substringBefore("\n\nGenerate SQL query:").trim()

            // Mock ONNX inference - analyze user message and generate SQL based on schema knowledge
            val sqlQuery = generateSqlFromMessage(userMessage)
            return sqlQuery

        } catch (e: Exception) {
            return "SELECT * FROM assets LIMIT 5" // Fallback on error
        }
    }

    private fun generateSqlFromMessage(userMessage: String): String {
        val message = userMessage.lowercase().trim()

        return when {
            // Asset queries
            message.matches(Regex("asset\\s+\\d+.*")) -> {
                val assetId = message.substringAfter("asset ").substringBefore(" ").toIntOrNull() ?: 0
                "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE a.id = $assetId"
            }

            message.contains("all assets") || message.contains("show assets") || message.contains("list assets") ->
                "SELECT a.id, a.name, a.details, a.condition, l.name AS location FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id"

            // Location queries
            message.matches(Regex("where is asset\\s+\\d+.*")) || message.contains("location of asset") -> {
                val assetId = Regex("\\d+").find(message)?.value?.toIntOrNull() ?: 0
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.id = $assetId"
            }

            message.contains("all locations") || message.contains("show locations") || message.contains("list locations") ->
                "SELECT l.id, l.name, l.locationCode, COUNT(a.id) as asset_count FROM locations l LEFT JOIN assets a ON l.id = a.currentRoomId GROUP BY l.id"

            // Movement/audit queries
            message.matches(Regex("move asset\\s+\\d+.*to.*\\d+.*")) -> {
                val assetId = Regex("move asset\\s+(\\d+)").find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val roomId = Regex("to.*(\\d+)").find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                "INSERT INTO asset_movements (assetId, fromRoomId, toRoomId, timestamp) SELECT $assetId, currentRoomId, $roomId, strftime('%s','now')*1000 FROM assets WHERE id = $assetId; UPDATE assets SET currentRoomId = $roomId WHERE id = $assetId"
            }

            message.contains("recent movements") || message.contains("asset movements") ->
                "SELECT am.id, a.name AS asset, lf.name AS from_location, lt.name AS to_location, am.timestamp FROM asset_movements am JOIN assets a ON am.assetId = a.id LEFT JOIN locations lf ON am.fromRoomId = lf.id LEFT JOIN locations lt ON am.toRoomId = lt.id ORDER BY am.timestamp DESC LIMIT 10"

            message.contains("audits") || message.contains("audit") ->
                "SELECT a.id, a.name, a.type, l.name AS location, a.createdAt, a.finishedAt FROM audits a LEFT JOIN locations l ON a.locationId = l.id ORDER BY a.createdAt DESC"

            // Search queries
            message.length >= 4 && !message.contains(" ") -> {
                // Single word search - could be asset name or location
                "SELECT 'Asset' AS type, a.id, a.name, l.name AS location FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name LIKE '%$userMessage%' UNION SELECT 'Location' AS type, l.id, l.name, NULL FROM locations l WHERE l.name LIKE '%$userMessage%'"
            }

            // Default fallback
            else -> "SELECT * FROM assets WHERE name LIKE '%' || '$userMessage' || '%' LIMIT 5"
        }
    }

    fun sendMessage(userMessage: String) {

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

        val messageId = System.currentTimeMillis().toString()

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
            // Insert user message first
            val userMsg = ChatMessage(
                id = messageId,
                text = userMessage,
                isUser = true
            )
            chatMessageDao.insert(ChatMessageEntity(
                messageId = userMsg.id,
                text = userMsg.text,
                isUser = true,
                timestamp = userMsg.timestamp
            ))

            val mode = _uiState.value.mode

            if (mode == AiMode.OnDevice) {
                // On-device mode
                try {
                    when (_uiState.value.onDeviceModel) {
                        OnDeviceModel.Gemma, OnDeviceModel.TinyFB -> {
                            val modelAsset = when (_uiState.value.onDeviceModel) {
                                OnDeviceModel.Gemma -> "gem_model.bin"
                                OnDeviceModel.TinyFB -> "tinyllama_fb.tflite"
                                else -> "gem_model.bin"
                            }
                            val assistant = LocalMediaPipeSqlAssistant(application, modelAsset)

                            // Check if query is asset-related
                            if (!assistant.isAssetRelatedQuery(userMessage)) {
                                // Handle general conversation
                                val generalPrompt = """
                                You are a helpful Asset Tracking Assistant. The user said: "$userMessage"

                                This doesn't seem to be an asset-related query. Respond politely and guide them towards asset tracking features.
                                Keep the response short and friendly.
                                """.trimIndent()
                                val response = assistant.generateResponse(generalPrompt)
                                val aiMsg = ChatMessage(
                                    id = "${messageId}_response",
                                    text = response,
                                    isUser = false
                                )
                                chatMessageDao.insert(ChatMessageEntity(
                                    messageId = aiMsg.id,
                                    text = aiMsg.text,
                                    isUser = false,
                                    timestamp = aiMsg.timestamp
                                ))
                                assistant.close()
                                _uiState.update { it.copy(isLoading = false) }
                                return@launch
                            }

                            // Proceed with asset-related query
                            var sqlQuery = assistant.generateSqlWithRetry(userMessage)
                            var data: TableData? = null
                            var error: String? = null
                            var retryCount = 0
                            val maxRetries = 2

                            while (retryCount <= maxRetries) {
                                try {
                                    data = getQueryData(sqlQuery)
                                    error = null
                                    break
                                } catch (e: Exception) {
                                    error = e.message ?: "Unknown SQL error"
                                    if (retryCount < maxRetries) {
                                        sqlQuery = assistant.generateSqlWithRetry(userMessage, error)
                                        retryCount++
                                    } else {
                                        break
                                    }
                                }
                            }

                            assistant.close()

                            if (error != null) {
                                // Still error after retries
                                val aiMsg = ChatMessage(
                                    id = "${messageId}_response",
                                    text = "SQL Error after retries: $error\nGenerated SQL: $sqlQuery",
                                    isUser = false
                                )
                                chatMessageDao.insert(ChatMessageEntity(
                                    messageId = aiMsg.id,
                                    text = aiMsg.text,
                                    isUser = false,
                                    timestamp = aiMsg.timestamp
                                ))
                            } else if (data != null) {
                                handleIntelligentResponse(data, userMessage, messageId, modelAsset)
                            }
                        }
                        OnDeviceModel.SqlTemplate -> {
                            val engine = LocalSqlFallbackEngine()
                            if (!engine.isAssetRelatedQuery(userMessage)) {
                                // Handle general conversation with simple response
                                val response = "Hello! I'm your Asset Tracking Assistant. You can ask me about assets, locations, audits, or movements. For example: 'Show all assets' or 'Where is asset 1?'"
                                val aiMsg = ChatMessage(
                                    id = "${messageId}_response",
                                    text = response,
                                    isUser = false
                                )
                                chatMessageDao.insert(ChatMessageEntity(
                                    messageId = aiMsg.id,
                                    text = aiMsg.text,
                                    isUser = false,
                                    timestamp = aiMsg.timestamp
                                ))
                                _uiState.update { it.copy(isLoading = false) }
                                return@launch
                            }
                            val sqlQuery = engine.generate(userMessage) ?: "SELECT * FROM assets LIMIT 5"
                            handleSqlWithResponse(sqlQuery, userMessage, messageId, generateResponse = false)
                        }
                    }
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

    fun setOnDeviceModel(model: OnDeviceModel) {
        _uiState.update { it.copy(onDeviceModel = model) }
    }

    fun retryLastMessage() {
        val message = lastUserMessage ?: return
        sendMessage(message)
    }

    private suspend fun handleSqlWithResponse(sqlQuery: String, userQuestion: String, messageId: String, generateResponse: Boolean = true) {
        if (!generateResponse) {
            handleSql(sqlQuery, messageId)
            return
        }

        val isWrite = sqlQuery.startsWith("INSERT", ignoreCase = true) ||
                sqlQuery.startsWith("UPDATE", ignoreCase = true) ||
                sqlQuery.startsWith("DELETE", ignoreCase = true)

        if (isWrite) {
            // For write queries, execute and show result
            executeQuery(sqlQuery, messageId)
        } else {
            // For read queries, execute and generate response
            val data = getQueryData(sqlQuery)
            if (data.rows.isEmpty()) {
                // No data, show table schemas
                val schemas = getTableSchemas()
                val response = generateResponseWithModel("User asked: $userQuestion\n\nNo data found. Here are the table schemas:\n$schemas\n\nProvide a helpful response.", userQuestion)
                val aiMsg = ChatMessage(
                    id = "${messageId}_response",
                    text = response,
                    isUser = false
                )
                chatMessageDao.insert(ChatMessageEntity(
                    messageId = aiMsg.id,
                    text = aiMsg.text,
                    isUser = false,
                    timestamp = aiMsg.timestamp
                ))
            } else {
                // Data found, generate intelligent response
                val dataText = formatDataAsText(data)
                val response = generateResponseWithModel("User asked: $userQuestion\n\nData retrieved:\n$dataText\n\nProvide a natural language answer based on this data, confirming if it answers the question.", userQuestion)
                val aiMsg = ChatMessage(
                    id = "${messageId}_response",
                    text = response,
                    isUser = false,
                    table = data
                )
                chatMessageDao.insert(ChatMessageEntity(
                    messageId = aiMsg.id,
                    text = aiMsg.text,
                    isUser = false,
                    timestamp = aiMsg.timestamp
                ))
            }
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun handleIntelligentResponse(data: TableData, userQuestion: String, messageId: String, modelAsset: String) {
        // Get additional context - all locations and assets summary
        val allLocations = getQueryData("SELECT id, name, locationCode FROM locations")
        val allAssets = getQueryData("SELECT id, name, currentRoomId FROM assets")
        val recentMovements = getQueryData("SELECT a.name AS asset, l1.name AS from_loc, l2.name AS to_loc, am.timestamp FROM asset_movements am JOIN assets a ON am.assetId = a.id LEFT JOIN locations l1 ON am.fromRoomId = l1.id LEFT JOIN locations l2 ON am.toRoomId = l2.id ORDER BY am.timestamp DESC LIMIT 5")

        val contextData = """
        All Locations: ${formatDataAsText(allLocations)}
        All Assets: ${formatDataAsText(allAssets)}
        Recent Movements: ${formatDataAsText(recentMovements)}
        """.trimIndent()

        val dataText = formatDataAsText(data)

        val intelligentPrompt = """
        You are an intelligent Asset Tracking Assistant with access to all database data.

        User Question: "$userQuestion"

        Query Results: $dataText

        Full Context:
        $contextData

        Task: Analyze the query results in the context of all data. Provide a comprehensive, helpful response that:
        - Answers the user's question directly
        - Explains the data meaningfully
        - Suggests related insights or actions if relevant
        - Confirms if the data fully answers the question or if more information is needed

        Response should be natural, conversational, and decision-supporting.
        """.trimIndent()

        val assistant = LocalMediaPipeSqlAssistant(application, modelAsset)
        val response = assistant.generateResponse(intelligentPrompt)
        assistant.close()

        val aiMsg = ChatMessage(
            id = "${messageId}_response",
            text = response,
            isUser = false,
            table = data
        )
        chatMessageDao.insert(ChatMessageEntity(
            messageId = aiMsg.id,
            text = aiMsg.text,
            isUser = false,
            timestamp = aiMsg.timestamp
        ))
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun getQueryData(sqlQuery: String): TableData {
        val cursor = database.query(sqlQuery, emptyArray())
        val columns = (0 until cursor.columnCount).map { cursor.getColumnName(it) }
        val rows = mutableListOf<List<String>>()
        if (cursor.moveToFirst()) {
            do {
                val row = (0 until cursor.columnCount).map { cursor.getString(it) ?: "NULL" }
                rows.add(row)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return TableData(columns, rows)
    }

    private fun getTableSchemas(): String {
        // Simplified schemas based on your database
        return """
        assets: id (INTEGER PRIMARY KEY), name (TEXT), details (TEXT), condition (TEXT), baseRoomId (INTEGER), currentRoomId (INTEGER)
        locations: id (INTEGER PRIMARY KEY), name (TEXT), description (TEXT), parentId (INTEGER), locationCode (TEXT)
        asset_movements: id (INTEGER PRIMARY KEY), assetId (INTEGER), fromRoomId (INTEGER), toRoomId (INTEGER), condition (TEXT), timestamp (INTEGER)
        audits: id (INTEGER PRIMARY KEY), name (TEXT), type (TEXT), includeChildren (INTEGER), locationId (INTEGER), createdAt (INTEGER), finishedAt (INTEGER)
        """.trimIndent()
    }

    private fun formatDataAsText(data: TableData): String {
        val sb = StringBuilder()
        sb.append("Columns: ${data.columns.joinToString(", ")}\n")
        sb.append("Rows (${data.rows.size}):\n")
        data.rows.forEach { row ->
            sb.append(row.joinToString(" | ")).append("\n")
        }
        return sb.toString()
    }

    private suspend fun generateResponseWithModel(prompt: String, userQuestion: String): String {
        val modelAsset = when (_uiState.value.onDeviceModel) {
            OnDeviceModel.Gemma -> "gem_model.bin"
            OnDeviceModel.TinyFB -> "tinyllama_fb.tflite"
            OnDeviceModel.SqlTemplate -> "gem_model.bin" // Fallback, but SqlTemplate doesn't use this
        }
        val assistant = LocalMediaPipeSqlAssistant(application, modelAsset)
        val response = assistant.generateResponse(prompt)
        assistant.close()
        return response
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
                    // No results found
                    val aiMsg = ChatMessage("${messageId}_response", "No results found for your query.", false)
                    chatMessageDao.insert(ChatMessageEntity(messageId = aiMsg.id, text = aiMsg.text, isUser = false, timestamp = aiMsg.timestamp))
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
        viewModelScope.launch {
            val aiMsg = ChatMessage(
                id = "${messageId}_error",
                text = "Sorry, I couldn't process that request. Please try again.",
                isUser = false
            )
            chatMessageDao.insert(ChatMessageEntity(
                messageId = aiMsg.id,
                text = aiMsg.text,
                isUser = false,
                timestamp = aiMsg.timestamp
            ))
            _uiState.update { it.copy(isLoading = false) }
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