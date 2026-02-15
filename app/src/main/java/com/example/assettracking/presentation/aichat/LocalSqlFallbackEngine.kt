package com.example.assettracking.presentation.aichat

import com.example.assettracking.data.local.AssetTrackingDatabase

/**
 * Deterministic, schema-aware SQL template generator for offline/quota fallback.
 * Keeps logic isolated for easier unit testing.
 */
class LocalSqlFallbackEngine {

    fun generate(userMessage: String): String? {
        return generateSqlFromMessage(userMessage)
    }

    fun generateOffline(userMessage: String, database: AssetTrackingDatabase): Pair<String?, String?> {
        val sql = generateSqlFromMessage(userMessage)
        return sql to null
    }

    private fun generateSqlFromMessage(userMessage: String): String {
        val message = userMessage.lowercase().trim()

        return when {
            // Asset addition commands
            message.matches(Regex("add asset\\s+.+")) || message.matches(Regex("create asset\\s+.+")) || message.matches(Regex("new asset\\s+.+")) -> {
                generateAssetInsertSql(userMessage)
            }

            // Location addition commands
            message.matches(Regex("add location\\s+.+")) || message.matches(Regex("create location\\s+.+")) || message.matches(Regex("new location\\s+.+")) -> {
                generateLocationInsertSql(userMessage)
            }

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

    private fun generateAssetInsertSql(userMessage: String): String {
        // Parse asset addition commands like:
        // "add asset laptop"
        // "create new asset monitor in room 5"
        // "add asset server with details high-performance"

        val message = userMessage.lowercase()

        // Extract asset name - everything after "add asset", "create asset", etc.
        val name = when {
            message.startsWith("add asset ") -> message.substringAfter("add asset ").trim()
            message.startsWith("create asset ") -> message.substringAfter("create asset ").trim()
            message.startsWith("new asset ") -> message.substringAfter("new asset ").trim()
            message.startsWith("create new asset ") -> message.substringAfter("create new asset ").trim()
            else -> message.substringAfter("asset ").trim()
        }

        // Check for location specification
        val locationMatch = Regex("in (room|location) (\\d+|[a-zA-Z][a-zA-Z0-9]*)").find(message)
        val locationId = if (locationMatch != null) {
            val locationValue = locationMatch.groupValues[2]
            // If it's a number, use it directly, otherwise we'd need to look up by name
            // For now, assume it's a number or we can't resolve it
            locationValue.toLongOrNull() ?: 0L
        } else null

        // Check for details
        val detailsMatch = Regex("with details? (.+)").find(message)
        val details = detailsMatch?.groupValues?.get(1)?.trim()

        // Check for condition
        val conditionMatch = Regex("condition (.+)").find(message)
        val condition = conditionMatch?.groupValues?.get(1)?.trim()

        // Build INSERT statement
        val columns = mutableListOf("name")
        val values = mutableListOf("'$name'")

        if (details != null) {
            columns.add("details")
            values.add("'$details'")
        }

        if (condition != null) {
            columns.add("condition")
            values.add("'$condition'")
        }

        if (locationId != null && locationId > 0) {
            columns.add("baseRoomId")
            values.add("$locationId")
            columns.add("currentRoomId")
            values.add("$locationId")
        }

        return "INSERT INTO assets (${columns.joinToString(", ")}) VALUES (${values.joinToString(", ")})"
    }

    private fun generateLocationInsertSql(userMessage: String): String {
        // Parse location addition commands like:
        // "add location server room"
        // "create new location office floor 2"
        // "add location warehouse with code WH-001"

        val message = userMessage.lowercase()

        // Extract location name
        val name = when {
            message.startsWith("add location ") -> message.substringAfter("add location ").trim()
            message.startsWith("create location ") -> message.substringAfter("create location ").trim()
            message.startsWith("new location ") -> message.substringAfter("new location ").trim()
            message.startsWith("create new location ") -> message.substringAfter("create new location ").trim()
            else -> message.substringAfter("location ").trim()
        }

        // Check for location code
        val codeMatch = Regex("with code (.+)").find(message)
        val locationCode = codeMatch?.groupValues?.get(1)?.trim() ?: ""

        // Check for description
        val descMatch = Regex("description (.+)").find(message)
        val description = descMatch?.groupValues?.get(1)?.trim()

        // Build INSERT statement
        val columns = mutableListOf("name", "locationCode")
        val values = mutableListOf("'$name'", "'$locationCode'")

        if (description != null) {
            columns.add("description")
            values.add("'$description'")
        }

        return "INSERT INTO locations (${columns.joinToString(", ")}) VALUES (${values.joinToString(", ")})"
    }
}
