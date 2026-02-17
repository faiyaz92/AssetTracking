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

    fun generateOffline(fullPrompt: String, database: AssetTrackingDatabase): Pair<String?, String?> {
        // Extract user message from the full AI prompt
        val userMessage = extractUserMessageFromPrompt(fullPrompt)
        val sql = generateSqlFromMessage(userMessage)
        return sql to null
    }

    private fun extractUserMessageFromPrompt(fullPrompt: String): String {
        // Extract the user message from after the last "User: " until "Generate SQL query:"
        val userMarker = "User: "
        val queryMarker = "Generate SQL query:"
        
        val lastUserIndex = fullPrompt.lastIndexOf(userMarker)
        if (lastUserIndex == -1) return fullPrompt // fallback
        
        val userMessageStart = lastUserIndex + userMarker.length
        val queryIndex = fullPrompt.indexOf(queryMarker, userMessageStart)
        
        val userMessage = if (queryIndex != -1) {
            fullPrompt.substring(userMessageStart, queryIndex).trim()
        } else {
            fullPrompt.substring(userMessageStart).trim()
        }
        
        // Remove any trailing newlines
        return userMessage.trim()
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

            // Asset queries - support both ID and name (must start with "asset")
            message.matches(Regex("^asset\\s+.+")) && !message.contains("add asset") && !message.contains("create asset") -> {
                val assetQuery = message.substringAfter("asset ").trim()
                // Always search by name: exact match first, then heuristic
                val escapedQuery = assetQuery.replace("'", "''")
                "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE a.name = '$escapedQuery' UNION " +
                "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE a.name LIKE '%$escapedQuery%' AND a.name != '$escapedQuery' LIMIT 5"
            }

            // Specific location assets queries (e.g., "location 1 assets", "floor 1 asset")
            message.matches(Regex(".+\\s+(?:assets?|asset)$")) && !message.contains("list") && !message.contains("show") && !message.contains("all") && !message.contains("missing") -> {
                val location = message.substringBeforeLast(" ").trim()
                "SELECT a.id, a.name, a.details, a.condition, bl.name AS base_location, cl.name AS current_location FROM assets a LEFT JOIN locations bl ON a.baseRoomId = bl.id LEFT JOIN locations cl ON a.currentRoomId = cl.id WHERE bl.name LIKE '%$location%' OR bl.locationCode LIKE '%$location%' OR cl.name LIKE '%$location%' OR cl.locationCode LIKE '%$location%'"
            }

            message.contains("all assets") || message.contains("show assets") || message.contains("list assets") || message.contains("asset list") || message.contains("list of assets") ->
                "SELECT a.id, a.name, a.details, a.condition, l.name AS location FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id"

            // Location queries - "where/wer is X" treats X as asset name
            (message.matches(Regex("^(where|wer)\\s+is\\s+.+")) || message.matches(Regex("^(where|wer)\\s+.+"))) && !message.contains("asset") && !message.contains("location") && !message.matches(Regex("^(where|wer)\\s+is\\s+(location|asset)\\s+.+")) -> {
                val isWhereIs = message.contains(" is ")
                val assetQuery = if (isWhereIs) {
                    message.substringAfter(" is ").trim()
                } else {
                    // Remove "where " or "wer " from start
                    message.substringAfter(" ").trim()
                }
                // Remove trailing question marks
                val cleanQuery = assetQuery.removeSuffix("?").trim()
                // Search by name: exact match first, then heuristic
                val escapedQuery = cleanQuery.replace("'", "''")
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name = '$escapedQuery' UNION " +
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name LIKE '%$escapedQuery%' AND a.name != '$escapedQuery' LIMIT 5"
            }

            // "X where/wer" or "X where?/wer?" patterns
            message.matches(Regex(".+\\s+(where|wer)\\??$")) && !message.contains("add") && !message.contains("create") && !message.contains("move") -> {
                // Remove " where", " wer", " where?", " wer?" from end
                val query = message.replace(Regex("\\s+(where|wer)\\??$"), "").trim()
                // Search by name: exact match first, then heuristic
                val escapedQuery = query.replace("'", "''")
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name = '$escapedQuery' UNION " +
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name LIKE '%$escapedQuery%' AND a.name != '$escapedQuery' LIMIT 5"
            }

            // Location queries - support both ID and name
            (message.matches(Regex("(where|wer)\\s+is\\s+asset\\s+.+")) || message.matches(Regex("(where|wer)\\s+asset\\s+.+")) || message.contains("location of asset")) && !message.contains("add asset") && !message.contains("create asset") -> {
                val assetQuery = message.substringAfter("asset ").trim()
                // Always search by name: exact match first, then heuristic
                val escapedQuery = assetQuery.replace("'", "''")
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name = '$escapedQuery' UNION " +
                "SELECT a.id, a.name, l.name AS location, l.locationCode FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name LIKE '%$escapedQuery%' AND a.name != '$escapedQuery' LIMIT 5"
            }

            message.contains("all locations") || message.contains("show locations") || message.contains("list locations") || message.contains("location list") || message.contains("list of locations") ->
                "SELECT l.id, l.name, l.locationCode, COUNT(a.id) as asset_count FROM locations l LEFT JOIN assets a ON l.id = a.currentRoomId GROUP BY l.id"

            // Missing assets queries
            message.matches(Regex(".*missing (?:asset|assets).*(?:of|in|at|for)\\s+(.+)")) -> {
                val location = Regex(".*missing (?:asset|assets).*(?:of|in|at|for)\\s+(.+)").find(message)?.groupValues?.get(1)?.trim()
                "SELECT a.id, a.name, a.details, a.condition, bl.name AS base_location FROM assets a LEFT JOIN locations bl ON a.baseRoomId = bl.id WHERE a.baseRoomId IS NOT NULL AND a.currentRoomId IS NULL AND (bl.name LIKE '%$location%' OR bl.locationCode LIKE '%$location%')"
            }
            message.contains("missing assets") || message.contains("list of missing assets") || message.contains("show missing assets") || message.contains("missing asset") ->
                "SELECT a.id, a.name, a.details, a.condition, bl.name AS base_location FROM assets a LEFT JOIN locations bl ON a.baseRoomId = bl.id WHERE a.baseRoomId IS NOT NULL AND a.currentRoomId IS NULL"

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
            message.length >= 2 -> {
                val escapedMessage = userMessage.replace("'", "''") // Escape single quotes for SQL
                // Always try exact match first, then partial matches as fallback
                "SELECT 'Asset' AS type, a.id, a.name, l.name AS location, CASE WHEN a.name = '$escapedMessage' THEN 1 ELSE 2 END AS priority FROM assets a LEFT JOIN locations l ON a.currentRoomId = l.id WHERE a.name LIKE '%$escapedMessage%' UNION " +
                "SELECT 'Location' AS type, l.id, l.name, NULL, CASE WHEN l.name = '$escapedMessage' THEN 1 ELSE 2 END AS priority FROM locations l WHERE l.name LIKE '%$escapedMessage%' " +
                "ORDER BY priority, type, name"
            }

            // Default fallback - partial match
            else -> {
                val escapedMessage = userMessage.replace("'", "''") // Escape single quotes for SQL
                "SELECT * FROM assets WHERE name LIKE '%$escapedMessage%' LIMIT 5"
            }
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

    fun isAssetRelatedQuery(userMessage: String): Boolean {
        val message = userMessage.lowercase().trim()
        val assetKeywords = listOf("asset", "location", "room", "audit", "movement", "track", "find", "where", "show", "list", "add", "create", "move", "update", "delete", "inventory", "equipment", "device")
        return assetKeywords.any { message.contains(it) } || message.matches(Regex("\\d+")) // Numbers might be IDs
    }
}
