package com.example.assettracking.presentation.aichat

import com.example.assettracking.data.local.AssetTrackingDatabase

/**
 * Deterministic, schema-aware SQL template generator for offline/quota fallback.
 * Keeps logic isolated for easier unit testing.
 */
class LocalSqlFallbackEngine {
    fun generate(userMessage: String): String? {
        // Placeholder - call generateOffline if needed
        return "SELECT * FROM assets LIMIT 5"
    }

    fun generateOffline(userMessage: String, database: AssetTrackingDatabase): Pair<String?, String?> {
        // Placeholder implementation - replace with full logic later
        return "SELECT * FROM assets LIMIT 5" to null
    }
}
