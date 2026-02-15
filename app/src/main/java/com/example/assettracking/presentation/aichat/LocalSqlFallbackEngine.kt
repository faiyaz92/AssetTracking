package com.example.assettracking.presentation.aichat

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.assettracking.data.local.AssetTrackingDatabase

/**
 * Deterministic, schema-aware SQL template generator for offline/quota fallback.
 * Keeps logic isolated for easier unit testing.
 */
class LocalSqlFallbackEngine {
    fun generate(userMessage: String): String? {
        val text = userMessage.lowercase()
        val normalized = text
            .replace("romm", "room")
            .replace(Regex("\\bass\\b"), "asset")
            .replace(Regex("\\bmoev\\b|\\bmvoe\\b|\\bmoce\\b|\\bmuv\\b|\\bmve\\b"), "move")
            .replace(Regex("\\bmv\\b|\\bmov\\b|\\bmve\\b"), "move")

        fun safeLike(term: String) = "%" + term.replace("'", "''") + "%"
        fun safe(term: String) = term.replace("'", "''")

        // Extract common tokens
        val assetToken = Regex("asset\\s*(\\d+)").find(text)?.groupValues?.getOrNull(1)
        val whereIsMatch = Regex("where\\s+is\\s+([\\w-]+)").find(text)
        val findMatch = Regex("find\\s+([\\w-]+)").find(text)
        val moveMatch = Regex("move\\s*asset?\\s*([\\w-]+)\\s*(?:to\\s*)?(?:room\\s*)?([\\w-]+)").find(normalized)
        val addLocationMatch = Regex("add\\s+location\\s+([\\w-]+)").find(text)
        val addAssetMatch = Regex("add\\s+asset\\s+([\\w-]+)").find(text)
        val locationInMatch = Regex("(in|at)\\s+([a-z0-9_-]+)").find(text)
        val roomMatch = Regex("room\\s+([a-z0-9_-]+)").find(text)
        val locationCodeMatch = Regex("code\\s+([a-z0-9_-]+)").find(text)
        val locationFindMatch = Regex("location\\s+([\\w-]+)").find(text)
        val locationAliasMatch = Regex("loc\\s*([\\w-]+)").find(text)
        val singleToken = text.trim().takeIf { it.isNotBlank() && !it.contains(" ") }
        val collapsedToken = text.trim().split(Regex("\\s+")).takeIf { it.size == 2 && it.all { part -> part.isNotBlank() } }?.joinToString(separator = "")
        val countRequested = text.contains("count") || text.contains("how many")
        val recentMoveWindow = Regex("last\\s+(\\d+)\\s+(day|days|hour|hours)").find(text)
        val historyAsset = Regex("history\\s+for\\s+asset\\s+([\\w-]+)").find(text)
        val conditionMatch = Regex("condition\\s+([a-z0-9_-]+)").find(text)
        val statusReport = text.contains("status") && (text.contains("report") || text.contains("summary") || text.contains("breakdown"))
        val missingReport = text.contains("missing") && text.contains("report")
        val auditsList = text.contains("audit") && (text.contains("list") || text.contains("pending") || text.contains("open"))
        val startAuditMatch = Regex("start\\s+audit\\s+([\\w-]+)").find(text)
        val finishAuditMatch = Regex("finish\\s+audit\\s*(\\d+)").find(text)
        val movementsForAsset = Regex("moves?\\s+for\\s+asset\\s+([\\w-]+)").find(text)
        val assetsWithoutMoves = text.contains("never moved") || text.contains("no movement")
        val assetsByBaseMatch = Regex("base\\s+(room|location)\\s+([a-z0-9_-]+)").find(text)
        val assetsByCodeMatch = Regex("asset\\s+code\\s+([a-z0-9_-]+)").find(text)
        val locationChildrenMatch = Regex("children\\s+of\\s+([a-z0-9_-]+)").find(text)
        val recentAssets = text.contains("recent") && text.contains("assets")
        val assetsByDetailMatch = Regex("detail[s]?\\s+like\\s+([\"']?)([\\w\\s-]+)\\1").find(text)

        return when {
            // List all assets with status and locations
            text.contains("list") && text.contains("asset") -> {
                "SELECT a.id, a.name, a.details, a.condition, a.baseRoomId, a.currentRoomId, " +
                        "lb.name AS baseLocation, lc.name AS currentLocation, " +
                        "CASE WHEN a.currentRoomId IS NULL THEN 'Missing' " +
                        "WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status " +
                        "FROM assets a " +
                        "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                        "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                        "ORDER BY a.id DESC"
            }

            // General asset query (e.g., "asset", "assets")
            text.contains("asset") && !text.contains("move") && !text.contains("add") && !text.contains("list") && !text.contains("count") && !text.contains("status") && !text.contains("missing") && !text.contains("history") && !text.contains("recent") && !text.contains("never") && !text.contains("base") && !text.contains("code") && !text.contains("detail") && assetToken == null && whereIsMatch == null && findMatch == null -> {
                "SELECT a.id, a.name, a.details, a.condition, a.baseRoomId, a.currentRoomId, " +
                        "lb.name AS baseLocation, lc.name AS currentLocation, " +
                        "CASE WHEN a.currentRoomId IS NULL THEN 'Missing' " +
                        "WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status " +
                        "FROM assets a " +
                        "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                        "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                        "ORDER BY a.id DESC"
            }

            // Assets in a location (current)
            locationInMatch != null && text.contains("asset") -> {
                val loc = safe(locationInMatch.groupValues[2])
                "SELECT a.id, a.name, a.details, a.condition, lc.name AS currentLocation, lb.name AS baseLocation, " +
                        "CASE WHEN a.currentRoomId IS NULL THEN 'Missing' " +
                        "WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status " +
                        "FROM assets a " +
                        "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                        "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                        "WHERE lc.name LIKE '${safeLike(loc)}' OR lc.locationCode LIKE '${safeLike(loc)}'"
            }

            // Assets in a room code match
            roomMatch != null && text.contains("asset") -> {
                val room = safe(roomMatch.groupValues[1])
                "SELECT a.id, a.name, a.details, a.condition, lc.name AS currentLocation, lb.name AS baseLocation, " +
                        "CASE WHEN a.currentRoomId IS NULL THEN 'Missing' " +
                        "WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status " +
                        "FROM assets a " +
                        "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                        "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                        "WHERE lc.locationCode LIKE '${safeLike(room)}' OR lc.name LIKE '${safeLike(room)}'"
            }

                        // Direct "where is asset ID" lookup
                        whereIsMatch != null && assetToken != null && assetToken.toIntOrNull() != null -> {
                            val id = assetToken.toIntOrNull()!!
                            "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, " +
                                    "CASE WHEN a.currentRoomId IS NULL THEN 'Missing' " +
                                    "WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status " +
                                    "FROM assets a " +
                                    "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                                    "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                                    "WHERE a.id = $id"
                        }

                        // Find asset by token/id/name with disambiguation and best guess
                        assetToken != null || whereIsMatch != null || findMatch != null || text.contains("find asset") || text.contains("where is") -> {
                                val termRaw = when {
                                        assetToken != null -> assetToken
                                        whereIsMatch != null -> whereIsMatch.groupValues[1]
                                        findMatch != null -> findMatch.groupValues[1]
                                        text.contains("asset") -> text.substringAfter("asset").trim().split(" ").firstOrNull().orEmpty()
                                        else -> text.trim()
                                }
                                val term = safe(termRaw)
                                val termLike = safeLike(termRaw)
                                """
                                WITH tok AS (SELECT '$term' AS t, '$termLike' AS t_like),
                                asset_hits AS (
                                    SELECT id, name, 3 AS score FROM assets a, tok WHERE LOWER(a.name) = LOWER(tok.t) OR CAST(a.id AS TEXT) = tok.t
                                    UNION ALL
                                    SELECT id, name, 2 AS score FROM assets a, tok WHERE a.name LIKE tok.t_like OR CAST(a.id AS TEXT) LIKE tok.t_like
                                ),
                                asset_final AS (
                                    SELECT DISTINCT id, name, score FROM asset_hits ORDER BY score DESC, id DESC LIMIT 5
                                ),
                                asset_count AS (SELECT COUNT(*) AS c FROM asset_final),
                                chosen_asset AS (SELECT id FROM asset_final ORDER BY score DESC, id DESC LIMIT 1),
                                asset_moves AS (
                                    SELECT m.id, lf.name AS fromLoc, lt.name AS toLoc, m.timestamp
                                    FROM asset_movements m, chosen_asset ca
                                    LEFT JOIN locations lf ON m.fromRoomId = lf.id
                                    LEFT JOIN locations lt ON m.toRoomId = lt.id
                                    WHERE m.assetId = ca.id
                                    ORDER BY m.timestamp DESC
                                    LIMIT 5
                                )
                                SELECT 'disambiguate_asset' AS type, id, name, score, NULL, NULL, NULL, NULL, NULL FROM asset_final WHERE (SELECT c FROM asset_count) > 1
                                UNION ALL
                                SELECT 'best_guess_asset' AS type, id, name, score, NULL, NULL, NULL, NULL, NULL FROM asset_final LIMIT 1
                                UNION ALL
                                SELECT 'asset_card' AS type, a.id, a.name, a.details, a.condition, lb.name AS baseLoc, lc.name AS currentLoc,
                                    CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status,
                                    (SELECT timestamp FROM asset_movements m WHERE m.assetId = a.id ORDER BY m.timestamp DESC LIMIT 1) AS lastMovedAt
                                FROM assets a, chosen_asset ca
                                LEFT JOIN locations lb ON a.baseRoomId = lb.id
                                LEFT JOIN locations lc ON a.currentRoomId = lc.id
                                WHERE a.id = ca.id
                                UNION ALL
                                SELECT 'asset_move' AS type, id, fromLoc, toLoc, timestamp, NULL, NULL, NULL, NULL FROM asset_moves
                                UNION ALL
                                SELECT 'no_match' AS type, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL WHERE (SELECT c FROM asset_count) = 0;
                                """.trimIndent()
                        }

            // Move asset template
            moveMatch != null -> {
                                val t1 = safe(moveMatch.groupValues[1])
                                val t2 = safe(moveMatch.groupValues[2])
                                val t1Like = safeLike(t1)
                                val t2Like = safeLike(t2)
                                """
                                WITH tokens AS (
                                    SELECT 1 AS ord, '$t1' AS token, '$t1Like' AS tok_like
                                    UNION ALL
                                    SELECT 2, '$t2', '$t2Like'
                                ),
                                asset_hits AS (
                                    SELECT ord, id, 3 AS score FROM tokens t JOIN assets a ON LOWER(t.token) = LOWER(CAST(a.id AS TEXT)) OR LOWER(t.token) = LOWER(a.name)
                                    UNION ALL
                                    SELECT ord, id, 2 AS score FROM tokens t JOIN assets a ON a.name LIKE t.tok_like OR CAST(a.id AS TEXT) LIKE t.tok_like
                                ),
                                location_hits AS (
                                    SELECT ord, id, 3 AS score FROM tokens t JOIN locations l ON LOWER(t.token) = LOWER(l.locationCode) OR LOWER(t.token) = LOWER(l.name)
                                    UNION ALL
                                    SELECT ord, id, 2 AS score FROM tokens t JOIN locations l ON l.locationCode LIKE t.tok_like OR l.name LIKE t.tok_like
                                ),
                                asset_final AS (
                                    SELECT id, ord, score FROM asset_hits ORDER BY score DESC, ord LIMIT 5
                                ),
                                location_final AS (
                                    SELECT id, ord, score FROM location_hits ORDER BY score DESC, ord LIMIT 5
                                ),
                                asset_count AS (SELECT COUNT(*) AS c FROM asset_final),
                                loc_count  AS (SELECT COUNT(*) AS c FROM location_final),
                                chosen AS (
                                    SELECT
                                        (SELECT id FROM asset_final ORDER BY score DESC, ord LIMIT 1) AS assetId,
                                        (SELECT id FROM location_final WHERE id NOT IN (SELECT id FROM asset_final) ORDER BY score DESC, ord LIMIT 1) AS locationId,
                                        (SELECT c FROM asset_count) AS assetCount,
                                        (SELECT c FROM loc_count) AS locCount
                                ),
                                asset_before AS (
                                    SELECT id, currentRoomId FROM assets WHERE id = (SELECT assetId FROM chosen)
                                ),
                                do_update AS (
                                    UPDATE assets
                                    SET currentRoomId = (SELECT locationId FROM chosen)
                                    WHERE id = (SELECT assetId FROM chosen)
                                        AND (SELECT assetCount FROM chosen) = 1
                                        AND (SELECT locCount FROM chosen) = 1
                                    RETURNING id
                                ),
                                do_insert AS (
                                    INSERT INTO asset_movements (assetId, fromRoomId, toRoomId, timestamp)
                                    SELECT
                                        (SELECT assetId FROM chosen),
                                        (SELECT currentRoomId FROM asset_before),
                                        (SELECT locationId FROM chosen),
                                        strftime('%s','now')*1000
                                    WHERE (SELECT assetCount FROM chosen) = 1 AND (SELECT locCount FROM chosen) = 1
                                    RETURNING id
                                )
                                SELECT 'disambiguate_asset' AS type, a.id, a.name, a.score, a.ord FROM asset_final a WHERE (SELECT c FROM asset_count) != 1
                                UNION ALL
                                SELECT 'disambiguate_location' AS type, l.id, l.name, l.score, l.ord FROM location_final l WHERE (SELECT c FROM loc_count) != 1
                                UNION ALL
                                SELECT 'best_guess_asset' AS type, a.id, (SELECT name FROM assets WHERE id = a.id) AS name, a.score, a.ord FROM asset_final a LIMIT 1
                                UNION ALL
                                SELECT 'best_guess_location' AS type, l.id, (SELECT name FROM locations WHERE id = l.id) AS name, l.score, l.ord FROM location_final l LIMIT 1
                                UNION ALL
                                SELECT 'move_executed' AS type, (SELECT assetId FROM chosen), (SELECT locationId FROM chosen), NULL AS score, NULL AS ord
                                WHERE (SELECT assetCount FROM chosen) = 1 AND (SELECT locCount FROM chosen) = 1;
                                """.trimIndent()
            }

            // Add location
            addLocationMatch != null -> {
                val name = safe(addLocationMatch.groupValues[1])
                "INSERT INTO locations (name) VALUES ('$name')"
            }

            // Add asset
            addAssetMatch != null -> {
                val name = safe(addAssetMatch.groupValues[1])
                "INSERT INTO assets (name) VALUES ('$name')"
            }

            // Assets by base location
            assetsByBaseMatch != null -> {
                val loc = safe(assetsByBaseMatch.groupValues[2])
                "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation FROM assets a " +
                        "LEFT JOIN locations lb ON a.baseRoomId = lb.id " +
                        "LEFT JOIN locations lc ON a.currentRoomId = lc.id " +
                        "WHERE lb.name LIKE '${safeLike(loc)}' OR lb.locationCode LIKE '${safeLike(loc)}'"
            }

            // Assets by code token
            assetsByCodeMatch != null -> {
                val code = safe(assetsByCodeMatch.groupValues[1])
                "SELECT a.id, a.name, a.details, a.condition, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.id LIKE '${safeLike(code)}'"
            }

            // Assets by detail substring
            assetsByDetailMatch != null -> {
                val term = safe(assetsByDetailMatch.groupValues[2])
                "SELECT a.id, a.name, a.details, a.condition, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.details LIKE '${safeLike(term)}'"
            }

            // Recent moves (optional window)
            text.contains("recent") && text.contains("move") -> {
                val windowMillis = recentMoveWindow?.let {
                    val n = it.groupValues[1].toLongOrNull() ?: 7L
                    val unit = it.groupValues[2]
                    val days = if (unit.startsWith("hour")) n / 24.0 else n.toDouble()
                    (days * 24 * 60 * 60 * 1000).toLong()
                }
                val sinceClause = windowMillis?.let { "WHERE m.timestamp >= (strftime('%s','now')*1000 - $it)" } ?: ""
                "SELECT m.id, a.name AS assetName, lf.name AS fromLocation, lt.name AS toLocation, m.timestamp " +
                        "FROM asset_movements m " +
                        "LEFT JOIN assets a ON m.assetId = a.id " +
                        "LEFT JOIN locations lf ON m.fromRoomId = lf.id " +
                        "LEFT JOIN locations lt ON m.toRoomId = lt.id " +
                        sinceClause +
                        " ORDER BY m.timestamp DESC LIMIT 50"
            }

            // Movements for asset
            movementsForAsset != null -> {
                val term = safe(movementsForAsset.groupValues[1])
                "SELECT m.id, a.name AS assetName, lf.name AS fromLocation, lt.name AS toLocation, m.timestamp " +
                        "FROM asset_movements m " +
                        "LEFT JOIN assets a ON m.assetId = a.id " +
                        "LEFT JOIN locations lf ON m.fromRoomId = lf.id " +
                        "LEFT JOIN locations lt ON m.toRoomId = lt.id " +
                        "WHERE a.name LIKE '${safeLike(term)}' OR a.id LIKE '${safeLike(term)}' " +
                        "ORDER BY m.timestamp DESC"
            }

            // Assets without movements
            assetsWithoutMoves -> {
                "SELECT a.id, a.name, a.details, a.condition FROM assets a WHERE NOT EXISTS (SELECT 1 FROM asset_movements m WHERE m.assetId = a.id)"
            }

            // Asset history
            historyAsset != null -> {
                val term = safe(historyAsset.groupValues[1])
                "SELECT m.id, a.name AS assetName, lf.name AS fromLocation, lt.name AS toLocation, m.timestamp " +
                        "FROM asset_movements m " +
                        "LEFT JOIN assets a ON m.assetId = a.id " +
                        "LEFT JOIN locations lf ON m.fromRoomId = lf.id " +
                        "LEFT JOIN locations lt ON m.toRoomId = lt.id " +
                        "WHERE a.name LIKE '${safeLike(term)}' OR a.id LIKE '${safeLike(term)}' " +
                        "ORDER BY m.timestamp DESC"
            }

            // Counts
            countRequested && text.contains("asset") -> {
                "SELECT COUNT(*) AS assetCount FROM assets"
            }

            // Status breakdown report
            statusReport -> {
                "SELECT " +
                        "SUM(CASE WHEN currentRoomId IS NULL THEN 1 ELSE 0 END) AS missingCount, " +
                        "SUM(CASE WHEN currentRoomId IS NOT NULL AND baseRoomId IS NOT NULL AND currentRoomId = baseRoomId THEN 1 ELSE 0 END) AS atHomeCount, " +
                        "SUM(CASE WHEN currentRoomId IS NOT NULL AND baseRoomId IS NOT NULL AND currentRoomId != baseRoomId THEN 1 ELSE 0 END) AS otherLocationCount, " +
                        "SUM(CASE WHEN baseRoomId IS NULL THEN 1 ELSE 0 END) AS unassignedCount " +
                        "FROM assets"
            }

            // Missing report
            missingReport -> {
                "SELECT a.id, a.name, a.details, lb.name AS baseLocation FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id WHERE a.currentRoomId IS NULL"
            }

            // Missing assets
            text.contains("missing") && text.contains("asset") -> {
                "SELECT a.id, a.name, a.details, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.currentRoomId IS NULL"
            }

            // General location query (e.g., "location", "locations")
            text.contains("location") && !text.contains("add") && !text.contains("list") && !text.contains("children") && locationFindMatch == null && locationAliasMatch == null -> {
                "SELECT id, name, description, locationCode, parentId FROM locations ORDER BY id DESC"
            }

            // Location details / lookup
            locationFindMatch != null || locationAliasMatch != null -> {
                val term = safe(locationFindMatch?.groupValues?.get(1) ?: locationAliasMatch?.groupValues?.get(1).orEmpty())
                "WITH loc AS (\n" +
                    "  SELECT l.id, l.name, l.description, l.parentId, l.locationCode,\n" +
                    "         (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName\n" +
                    "  FROM locations l\n" +
                    "  WHERE l.name LIKE '${safeLike(term)}' OR l.locationCode LIKE '${safeLike(term)}' OR CAST(l.id AS TEXT) LIKE '${safeLike(term)}'\n" +
                    "  ORDER BY l.id DESC\n" +
                    "  LIMIT 1\n" +
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
            }

            // Single token asset name search with simple algo
            singleToken != null && singleToken.toIntOrNull() == null -> {
                val token = safe(singleToken)
                """
                WITH check_asset AS (SELECT COUNT(*) AS c FROM assets WHERE LOWER(name) = LOWER('$token')),
                check_loc AS (SELECT COUNT(*) AS c FROM locations WHERE LOWER(name) = LOWER('$token') OR LOWER(locationCode) = LOWER('$token')),
                asset_detail AS (
                    SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation,
                        CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status
                    FROM assets a
                    LEFT JOIN locations lb ON a.baseRoomId = lb.id
                    LEFT JOIN locations lc ON a.currentRoomId = lc.id
                    WHERE LOWER(a.name) = LOWER('$token')
                ),
                loc_detail AS (
                    SELECT l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName
                    FROM locations l
                    WHERE LOWER(l.name) = LOWER('$token') OR LOWER(l.locationCode) = LOWER('$token')
                ),
                top_matches AS (
                    SELECT 'asset' AS type, a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation,
                        CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status
                    FROM assets a
                    LEFT JOIN locations lb ON a.baseRoomId = lb.id
                    LEFT JOIN locations lc ON a.currentRoomId = lc.id
                    WHERE a.name LIKE '%$token%'
                    ORDER BY a.id DESC LIMIT 3
                    UNION ALL
                    SELECT 'location' AS type, l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName, NULL, NULL
                    FROM locations l
                    WHERE l.name LIKE '%$token%' OR l.locationCode LIKE '%$token%'
                    ORDER BY l.id DESC LIMIT 3
                )
                SELECT * FROM asset_detail WHERE (SELECT c FROM check_asset) > 0
                UNION ALL
                SELECT * FROM loc_detail WHERE (SELECT c FROM check_loc) > 0 AND (SELECT c FROM check_asset) = 0
                UNION ALL
                SELECT * FROM top_matches WHERE (SELECT c FROM check_asset) = 0 AND (SELECT c FROM check_loc) = 0
                """.trimIndent()
            }

                        // Single-token or collapsed pair: Simple search in assets and locations
                        singleToken != null && singleToken.toIntOrNull() != null || collapsedToken != null -> {
                            val raw = singleToken ?: collapsedToken!!
                            val token = safe(raw)
                            val tokenLike = safeLike(raw)
                            """
                            SELECT 'asset' AS type, a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation,
                                CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status
                            FROM assets a
                            LEFT JOIN locations lb ON a.baseRoomId = lb.id
                            LEFT JOIN locations lc ON a.currentRoomId = lc.id
                            WHERE LOWER(a.name) = LOWER('$token') OR CAST(a.id AS TEXT) = '$token' OR a.name LIKE '$tokenLike'
                            UNION ALL
                            SELECT 'location' AS type, l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName, NULL, NULL
                            FROM locations l
                            WHERE LOWER(l.name) = LOWER('$token') OR LOWER(l.locationCode) = LOWER('$token') OR l.name LIKE '$tokenLike' OR l.locationCode LIKE '$tokenLike'
                            ORDER BY type, id DESC LIMIT 10;
                            """.trimIndent()
                        }

            // At home / other location / unassigned
            text.contains("at home") -> {
                "SELECT a.id, a.name, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.currentRoomId = a.baseRoomId"
            }
            text.contains("other location") -> {
                "SELECT a.id, a.name, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.currentRoomId IS NOT NULL AND a.baseRoomId IS NOT NULL AND a.currentRoomId != a.baseRoomId"
            }
            text.contains("not assigned") || text.contains("unassigned") -> {
                "SELECT a.id, a.name, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.baseRoomId IS NULL"
            }

            // List locations
            text.contains("list") && text.contains("location") -> {
                "SELECT id, name, parentId, locationCode FROM locations ORDER BY id DESC"
            }

            // Location by code/name
            locationCodeMatch != null && text.contains("location") -> {
                val code = safe(locationCodeMatch.groupValues[1])
                "SELECT id, name, parentId, locationCode FROM locations WHERE locationCode LIKE '${safeLike(code)}' OR name LIKE '${safeLike(code)}'"
            }

            // Children of location
            locationChildrenMatch != null -> {
                val loc = safe(locationChildrenMatch.groupValues[1])
                "SELECT child.id, child.name, child.parentId, child.locationCode FROM locations parent " +
                        "JOIN locations child ON child.parentId = parent.id " +
                        "WHERE parent.name LIKE '${safeLike(loc)}' OR parent.locationCode LIKE '${safeLike(loc)}'"
            }

            // Condition filter
            conditionMatch != null -> {
                val cond = safeLike(conditionMatch.groupValues[1])
                "SELECT a.id, a.name, a.details, a.condition, a.baseRoomId, a.currentRoomId FROM assets a WHERE a.condition LIKE '$cond'"
            }

            // Audits list / pending
            auditsList -> {
                val pendingOnly = text.contains("pending") || text.contains("open")
                val where = if (pendingOnly) "WHERE finishedAt IS NULL" else ""
                "SELECT id, name, type, includeChildren, locationId, createdAt, finishedAt FROM audits $where ORDER BY createdAt DESC"
            }

            // Start audit (insert)
            startAuditMatch != null -> {
                val loc = safe(startAuditMatch.groupValues[1])
                "INSERT INTO audits (name, type, includeChildren, locationId, createdAt) VALUES (" +
                        "'audit_' || strftime('%s','now'), 'manual', 1, (SELECT id FROM locations WHERE name LIKE '${safeLike(loc)}' OR locationCode LIKE '${safeLike(loc)}'), strftime('%s','now')*1000)"
            }

            // Finish audit
            finishAuditMatch != null -> {
                val id = finishAuditMatch.groupValues[1]
                "UPDATE audits SET finishedAt = strftime('%s','now')*1000 WHERE id = $id"
            }

            // Recent assets (by id desc)
            recentAssets -> {
                "SELECT id, name, details, condition, baseRoomId, currentRoomId FROM assets ORDER BY id DESC LIMIT 50"
            }

            else -> null
        }
    }

    fun generateSingleTokenIntelligent(token: String, database: AssetTrackingDatabase): String? {
        val trimmed = token.trim()
        if (trimmed.isBlank() || trimmed.contains(" ") || trimmed.toIntOrNull() != null) return null

        // Intelligent progressive search
        var table: String? = null
        var current = trimmed
        while (current.length >= 3) {
            val assetCount = database.openHelper.writableDatabase.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM assets WHERE name LIKE ?", arrayOf("%$current%"))).use { it.moveToFirst(); it.getInt(0) }
            if (assetCount > 0) {
                table = "asset"
                break
            }
            val locCount = database.openHelper.writableDatabase.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM locations WHERE name LIKE ? OR locationCode LIKE ?", arrayOf("%$current%", "%$current%"))).use { it.moveToFirst(); it.getInt(0) }
            if (locCount > 0) {
                table = "location"
                break
            }
            current = current.dropLast(1)
        }
        if (table != null) {
            // Progressive search in the table
            var searchToken = trimmed
            var lastCount = 0
            var lastToken = ""
            while (searchToken.length >= 3) {
                val count = if (table == "asset") {
                    database.openHelper.writableDatabase.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM assets WHERE name LIKE ?", arrayOf("%$searchToken%"))).use { it.moveToFirst(); it.getInt(0) }
                } else {
                    database.openHelper.writableDatabase.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM locations WHERE name LIKE ? OR locationCode LIKE ?", arrayOf("%$searchToken%", "%$searchToken%"))).use { it.moveToFirst(); it.getInt(0) }
                }
                if (count > 0) {
                    lastCount = count
                    lastToken = searchToken
                } else {
                    break
                }
                searchToken = searchToken.dropLast(1)
            }
            if (lastCount > 0) {
                if (lastToken == trimmed && lastCount == 1) {
                    // Exact match, show detail
                    val sql = if (table == "asset") {
                        "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE LOWER(a.name) = LOWER('$trimmed')"
                    } else {
                        "SELECT l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName FROM locations l WHERE LOWER(l.name) = LOWER('$trimmed') OR LOWER(l.locationCode) = LOWER('$trimmed')"
                    }
                    return sql
                } else {
                    // Show top 3 with intersection
                    val remaining = trimmed.substring(lastToken.length)
                    val sql = if (table == "asset") {
                        if (remaining.isNotEmpty()) {
                            "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE a.name LIKE '%$lastToken%' AND a.name LIKE '%$remaining%' ORDER BY a.id DESC LIMIT 3"
                        } else {
                            "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE a.name LIKE '%$lastToken%' ORDER BY a.id DESC LIMIT 3"
                        }
                    } else {
                        if (remaining.isNotEmpty()) {
                            "SELECT l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName FROM locations l WHERE (l.name LIKE '%$lastToken%' OR l.locationCode LIKE '%$lastToken%') AND (l.name LIKE '%$remaining%' OR l.locationCode LIKE '%$remaining%') ORDER BY l.id DESC LIMIT 3"
                        } else {
                            "SELECT l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName FROM locations l WHERE l.name LIKE '%$lastToken%' OR l.locationCode LIKE '%$lastToken%' ORDER BY l.id DESC LIMIT 3"
                        }
                    }
                    return sql
                }
            }
        }
        // No match in progressive, show top 3 from both
        val sql = "SELECT 'asset' AS type, a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE a.name LIKE '%$trimmed%' ORDER BY a.id DESC LIMIT 3 UNION ALL SELECT 'location' AS type, l.id, l.name, l.description, l.locationCode, (SELECT name FROM locations p WHERE p.id = l.parentId) AS parentName, NULL, NULL FROM locations l WHERE l.name LIKE '%$trimmed%' OR l.locationCode LIKE '%$trimmed%' ORDER BY l.id DESC LIMIT 3"
        return sql
    }

    fun generateOffline(userMessage: String, database: AssetTrackingDatabase): Pair<String?, String?> {
        val trimmed = userMessage.trim()

        // Single word, not number
        if (trimmed.isNotBlank() && !trimmed.contains(" ") && trimmed.toIntOrNull() == null) {
            val sql = generateSingleTokenIntelligent(trimmed, database)
            return sql to null
        }

        // Special handling for "where is [term]"
        val whereIsMatch = Regex("where\\s+is\\s+(.+)", RegexOption.IGNORE_CASE).find(trimmed)
        if (whereIsMatch != null) {
            val term = whereIsMatch.groupValues[1].trim()
            if (term.isNotBlank()) {
                val exactCount = database.openHelper.writableDatabase.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM assets WHERE LOWER(name) = LOWER('$term') OR CAST(id AS TEXT) = '$term'")).use { it.moveToFirst(); it.getInt(0) }
                if (exactCount > 0) {
                    val sql = "SELECT a.id, a.name, a.details, a.condition, lb.name AS baseLocation, lc.name AS currentLocation, CASE WHEN a.currentRoomId IS NULL THEN 'Missing' WHEN a.currentRoomId = a.baseRoomId THEN 'At Home' ELSE 'At Other Location' END AS status FROM assets a LEFT JOIN locations lb ON a.baseRoomId = lb.id LEFT JOIN locations lc ON a.currentRoomId = lc.id WHERE LOWER(a.name) = LOWER('$term') OR CAST(a.id AS TEXT) = '$term' LIMIT 1"
                    return sql to null
                } else {
                    return null to "No exact match found for '$term'."
                }
            }
        }

        // Other offline queries
        val sql = generate(userMessage)
        return sql to null
    }
}
