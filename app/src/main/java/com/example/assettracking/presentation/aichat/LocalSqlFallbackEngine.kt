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
}
