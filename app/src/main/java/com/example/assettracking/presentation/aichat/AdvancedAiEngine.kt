package com.example.assettracking.presentation.aichat

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

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
            .setMaxTokens(512)  // Reduced for compatibility
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
        val modelDir = File(context.filesDir, "models")
        if (!modelDir.exists()) modelDir.mkdirs()
        val modelFile = File(modelDir, assetPath)
        if (!modelFile.exists()) {
            assetManager.open(assetPath).use { input ->
                if (isZipFile(input)) {
                    unzip(input, modelDir)
                    // Assume the model file is inside, perhaps find the .bin or .tflite
                    val files = modelDir.listFiles()
                    val model = files?.find { it.name.endsWith(".bin") || it.name.endsWith(".tflite") }
                    return model?.absolutePath ?: throw IllegalStateException("Model file not found in zip")
                } else {
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    return modelFile.absolutePath
                }
            }
        } else {
            return modelFile.absolutePath
        }
    }

    private fun isZipFile(input: java.io.InputStream): Boolean {
        val buffer = ByteArray(4)
        input.read(buffer)
        input.reset()
        return buffer[0] == 0x50.toByte() && buffer[1] == 0x4B.toByte()
    }

    private fun unzip(input: java.io.InputStream, destDir: File) {
        ZipInputStream(input).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = destDir.absolutePath + File.separator + entry.name
                if (!entry.isDirectory) {
                    File(filePath).parentFile?.mkdirs()
                    FileOutputStream(filePath).use { fos ->
                        zipIn.copyTo(fos)
                    }
                } else {
                    File(filePath).mkdirs()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    override fun close() {
        llmInference.close()
    }
}