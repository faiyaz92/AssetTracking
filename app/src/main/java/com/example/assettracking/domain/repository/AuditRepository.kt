package com.example.assettracking.domain.repository

import com.example.assettracking.domain.model.AuditCreateRequest
import com.example.assettracking.domain.model.AuditRecord
import kotlinx.coroutines.flow.Flow

interface AuditRepository {
    fun observeAudits(): Flow<List<AuditRecord>>
    suspend fun createAudit(request: AuditCreateRequest): Result<Long>
    suspend fun finishAudit(auditId: Long): Result<Unit>
    suspend fun getAudit(auditId: Long): AuditRecord?
}
