package com.example.assettracking.presentation.aichat

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * Advanced AI Engine for generating intelligent HTML responses.
 * Uses MediaPipe LLM Inference with full schema knowledge.
 */
class AdvancedAiEngine(
    private val context: Context,
    private val modelAssetPath: String = "gem_model.bin"
) : AutoCloseable {

    private val llmInference: LlmInference by lazy {
        val modelPath = ensureModelInInternalStorage(modelAssetPath)

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)  // Increased for richer responses
            .setTemperature(0.7f)  // More creative
            .build()

        LlmInference.createFromOptions(context, options)
    }

    private val schemaInfo = """
        Database Schema:
        1. assets: id (PRIMARY KEY), name, details?, condition?, baseRoomId?, currentRoomId?
        2. locations: id (PRIMARY KEY), name, description?, parentId?, locationCode
        3. asset_movements: id (PRIMARY KEY), assetId, fromRoomId?, toRoomId, condition?, timestamp
        4. audits: id (PRIMARY KEY), name, type, includeChildren, locationId, createdAt, finishedAt?

        Relations:
        - assets.baseRoomId/currentRoomId -> locations.id (rooms are locations)
        - asset_movements.assetId -> assets.id
        - asset_movements.fromRoomId/toRoomId -> locations.id
        - audits.locationId -> locations.id
        - locations.parentId -> locations.id (hierarchy)

        Current Data Summary (approximate):
        - Assets: Various items with names, conditions, locations
        - Locations: Rooms and areas with codes
        - Movements: Track asset transfers between locations
        - Audits: Inventory checks for locations
    """.trimIndent()

    fun generateResponse(userQuery: String): String {
        require(userQuery.isNotBlank()) { "User query cannot be blank." }

        val prompt = buildPrompt(userQuery)
        val raw = llmInference.generateResponse(prompt)
        return sanitizeHtml(raw)
    }

    private fun buildPrompt(userQuery: String): String = """
        You are an advanced AI assistant for an Asset Tracking system. You have full access to the database schema and can reason about data, generate SQL queries internally if needed, and provide intelligent responses.

        $schemaInfo

        Instructions:
        - Be intelligent and helpful. Answer any question about assets, locations, movements, or audits.
        - If the query requires specific data, you can "query" the database conceptually using the schema knowledge.
        - For data-dependent answers, generate appropriate SQL in your reasoning but respond naturally.
        - Respond in valid HTML format for rich display (use tables, lists, bold, etc.).
        - Keep responses informative but concise.
        - If unsure, provide the best possible answer based on schema.

        User Query: "$userQuery"

        Response (in HTML):
    """.trimIndent()

    private fun sanitizeHtml(raw: String): String {
        // Basic sanitization - remove any non-HTML prefixes/suffixes
        val trimmed = raw.trim()
        val start = trimmed.indexOf("<")
        val end = trimmed.lastIndexOf(">")
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            // Fallback: wrap in basic HTML if not HTML
            "<p>$trimmed</p>"
        }
    }

    private fun ensureModelInInternalStorage(assetPath: String): String {
        val assetManager = context.assets
        val file = File(context.filesDir, assetPath)
        if (!file.exists()) {
            assetManager.open(assetPath).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    override fun close() {
        llmInference.close()
    }
}