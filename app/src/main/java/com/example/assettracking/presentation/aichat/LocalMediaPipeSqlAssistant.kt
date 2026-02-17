package com.example.assettracking.presentation.aichat

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference

/**
 * Local SQL assistant backed by MediaPipe LLM Inference.
 *
 * Usage:
 * 1) Put your model in app/src/main/assets/gem_model.bin
 * 2) Create this class once (for example in a ViewModel/service)
 * 3) Call [generateSql] with user natural language text
 */
class LocalMediaPipeSqlAssistant(
    private val context: Context,
    private val modelPath: String
) : AutoCloseable {

    private val llmInference: LlmInference by lazy {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(512)
            .setTemperature(0.0f)
            .build()

        LlmInference.createFromOptions(context, options)
    }

    fun generateSqlWithRetry(userQuestion: String, previousError: String? = null): String {
        require(userQuestion.isNotBlank()) { "User question cannot be blank." }

        val prompt = if (previousError != null) {
            buildRetryPrompt(userQuestion, previousError)
        } else {
            buildPrompt(userQuestion)
        }
        val raw = llmInference.generateResponse(prompt)
        return sanitizeSql(raw)
    }

    private fun buildRetryPrompt(userQuestion: String, error: String): String =
        """
        Context:
        You are an expert SQL assistant for an Asset Tracking App.
        Database Schema:
        1. assets: id (PRIMARY KEY), name, details?, condition?, baseRoomId?, currentRoomId?
        2. locations: id (PRIMARY KEY), name, description?, parentId?, locationCode
        3. asset_movements: id (PRIMARY KEY), assetId, fromRoomId?, toRoomId, condition?, timestamp
        4. audits: id (PRIMARY KEY), name, type, includeChildren, locationId, createdAt, finishedAt?

        Relations:
        - assets.baseRoomId/currentRoomId -> locations.id
        - asset_movements.assetId -> assets.id
        - asset_movements.fromRoomId/toRoomId -> locations.id
        - audits.locationId -> locations.id

        Previous SQL attempt failed with error: $error

        Task:
        Fix the SQL query based on the error and generate a correct SQLite query for the user's question.
        Return ONLY the corrected SQL string. No explanations.

        User Question: "$userQuestion"
        Corrected SQL:
        """.trimIndent()

    fun generateResponse(prompt: String): String {
        require(prompt.isNotBlank()) { "Prompt cannot be blank." }

        val raw = llmInference.generateResponse(prompt)
        return raw.trim()
    }

    private fun buildPrompt(userQuestion: String): String =
        """
        Context:
        You are an expert SQL assistant for an Asset Tracking App.
        Database Schema:
        1. assets: id (PRIMARY KEY), name, details?, condition?, baseRoomId?, currentRoomId?
        2. locations: id (PRIMARY KEY), name, description?, parentId?, locationCode
        3. asset_movements: id (PRIMARY KEY), assetId, fromRoomId?, toRoomId, condition?, timestamp
        4. audits: id (PRIMARY KEY), name, type, includeChildren, locationId, createdAt, finishedAt?

        Relations:
        - assets.baseRoomId/currentRoomId -> locations.id
        - asset_movements.assetId -> assets.id
        - asset_movements.fromRoomId/toRoomId -> locations.id
        - audits.locationId -> locations.id

        Task:
        Convert the User's natural language question into a valid SQLite query.
        Return ONLY the SQL string. No explanations.

        User Question: "$userQuestion"
        SQL:
        """.trimIndent()

    private fun sanitizeSql(rawOutput: String): String {
        val cleaned = rawOutput
            .replace("```sql", "", ignoreCase = true)
            .replace("```", "")
            .lineSequence()
            .dropWhile { it.trim().startsWith("sql:", ignoreCase = true) }
            .joinToString("\n")
            .trim()

        val oneLine = cleaned.replace("\n", " ").replace(Regex("\\s+"), " ").trim()

        // Keep only the first statement if the model returns extra text/statements.
        val statement = oneLine.substringBefore(";").trim().let { if (it.isEmpty()) oneLine else "$it;" }

        val startsLikeSql = statement.startsWith("SELECT", ignoreCase = true) ||
            statement.startsWith("INSERT", ignoreCase = true) ||
            statement.startsWith("UPDATE", ignoreCase = true) ||
            statement.startsWith("DELETE", ignoreCase = true) ||
            statement.startsWith("WITH", ignoreCase = true)

        return if (startsLikeSql) statement else "SELECT 1;"
    }

    fun isAssetRelatedQuery(userQuestion: String): Boolean {
        val question = userQuestion.lowercase().trim()
        val assetKeywords = listOf("asset", "location", "room", "audit", "movement", "track", "find", "where", "show", "list", "add", "create", "move", "update", "delete", "inventory", "equipment", "device")
        return assetKeywords.any { question.contains(it) } || question.matches(Regex("\\d+")) // Numbers might be IDs
    }

    override fun close() {
        llmInference.close()
    }
}
