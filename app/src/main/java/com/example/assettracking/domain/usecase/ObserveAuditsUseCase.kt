package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AuditRecord
import com.example.assettracking.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuditsUseCase @Inject constructor(
    private val auditRepository: AuditRepository
) {
    operator fun invoke(): Flow<List<AuditRecord>> = auditRepository.observeAudits()
}
