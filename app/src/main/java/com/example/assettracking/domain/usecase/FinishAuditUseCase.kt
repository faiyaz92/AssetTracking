package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AuditRepository
import javax.inject.Inject

class FinishAuditUseCase @Inject constructor(
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(auditId: Long): Result<Unit> = auditRepository.finishAudit(auditId)
}
