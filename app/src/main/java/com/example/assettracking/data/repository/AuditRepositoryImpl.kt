package com.example.assettracking.data.repository

import androidx.room.withTransaction
import com.example.assettracking.data.local.AssetTrackingDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AuditDao
import com.example.assettracking.data.local.dao.LocationDao
import com.example.assettracking.data.local.entity.AuditEntity
import com.example.assettracking.data.local.model.AuditWithLocationTuple
import com.example.assettracking.data.local.model.LocationParentTuple
import com.example.assettracking.domain.model.AuditCreateRequest
import com.example.assettracking.domain.model.AuditRecord
import com.example.assettracking.domain.model.AuditType
import com.example.assettracking.domain.repository.AuditRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val database: AssetTrackingDatabase,
    private val auditDao: AuditDao,
    private val assetDao: AssetDao,
    private val locationDao: LocationDao
) : AuditRepository {

    override fun observeAudits(): Flow<List<AuditRecord>> =
        auditDao.observeAudits().map { audits -> audits.map { it.toDomainModel() } }

    override suspend fun createAudit(request: AuditCreateRequest): Result<Long> = runCatching {
        database.withTransaction {
            val location = locationDao.getLocationById(request.locationId)
                ?: throw IllegalArgumentException("Location not found")

            val scopeIds = if (request.includeChildren) {
                val hierarchy = locationDao.getLocationHierarchy()
                collectDescendants(request.locationId, hierarchy)
            } else {
                listOf(request.locationId)
            }

            val auditId = auditDao.insert(
                AuditEntity(
                    name = request.name,
                    type = request.type.name,
                    includeChildren = request.includeChildren,
                    locationId = location.id,
                    createdAt = System.currentTimeMillis()
                )
            )

            if (scopeIds.isNotEmpty()) {
                val currentMatches = assetDao.getAssetIdsByCurrentLocations(scopeIds)
                val baseMatches = if (request.type == AuditType.FULL) {
                    assetDao.getAssetIdsByBaseLocations(scopeIds)
                } else {
                    emptyList()
                }
                val idsToClear = (currentMatches + baseMatches).distinct()
                if (idsToClear.isNotEmpty()) {
                    assetDao.clearCurrentLocation(idsToClear)
                }
            }

            auditId
        }
    }

    override suspend fun finishAudit(auditId: Long): Result<Unit> = runCatching {
        auditDao.finishAudit(auditId, System.currentTimeMillis())
    }

    override suspend fun getAudit(auditId: Long): AuditRecord? {
        return auditDao.getAuditById(auditId)?.toDomainModel()
    }

    private fun collectDescendants(targetId: Long, hierarchy: List<LocationParentTuple>): List<Long> {
        if (hierarchy.isEmpty()) return listOf(targetId)
        val childrenByParent = hierarchy.groupBy { it.parentId }
        val queue = ArrayDeque<Long>()
        val result = mutableListOf<Long>()
        queue.add(targetId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            childrenByParent[current]?.forEach { child ->
                queue.add(child.locationId)
            }
        }
        return result
    }

    private fun AuditWithLocationTuple.toDomainModel(): AuditRecord {
        val safeType = runCatching { AuditType.valueOf(type) }.getOrElse { AuditType.MINI }
        return AuditRecord(
            id = auditId,
            name = name,
            type = safeType,
            includeChildren = includeChildren,
            locationId = locationId,
            locationName = locationName,
            locationCode = locationCode,
            createdAt = createdAt,
            finishedAt = finishedAt
        )
    }
}
