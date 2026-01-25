package com.example.assettracking.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.assettracking.data.local.entity.AuditEntity
import com.example.assettracking.data.local.model.AuditWithLocationTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(audit: AuditEntity): Long

    @Query(
        """
        SELECT a.id AS auditId,
               a.name AS name,
               a.type AS type,
               a.includeChildren AS includeChildren,
               a.locationId AS locationId,
               l.name AS locationName,
               l.locationCode AS locationCode,
               a.createdAt AS createdAt,
               a.finishedAt AS finishedAt
        FROM audits a
        JOIN locations l ON l.id = a.locationId
        ORDER BY a.createdAt DESC
        """
    )
    fun observeAudits(): Flow<List<AuditWithLocationTuple>>

    @Query(
        """
        SELECT a.id AS auditId,
               a.name AS name,
               a.type AS type,
               a.includeChildren AS includeChildren,
               a.locationId AS locationId,
               l.name AS locationName,
               l.locationCode AS locationCode,
               a.createdAt AS createdAt,
               a.finishedAt AS finishedAt
        FROM audits a
        JOIN locations l ON l.id = a.locationId
        WHERE a.id = :auditId
        LIMIT 1
        """
    )
    suspend fun getAuditById(auditId: Long): AuditWithLocationTuple?

    @Query("UPDATE audits SET finishedAt = :finishedAt WHERE id = :auditId")
    suspend fun finishAudit(auditId: Long, finishedAt: Long)
}
