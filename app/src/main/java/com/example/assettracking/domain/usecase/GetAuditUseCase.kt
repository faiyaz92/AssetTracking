package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AuditRecord
import com.example.assettracking.domain.repository.AuditRepository
import javax.inject.Inject

class GetAuditUseCase @Inject constructor(
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(auditId: Long): AuditRecord? = auditRepository.getAudit(auditId)
}
