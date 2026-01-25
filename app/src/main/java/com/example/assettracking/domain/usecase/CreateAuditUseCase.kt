package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AuditCreateRequest
import com.example.assettracking.domain.model.AuditType
import com.example.assettracking.domain.repository.AuditRepository
import javax.inject.Inject

class CreateAuditUseCase @Inject constructor(
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(
        name: String,
        locationId: Long,
        type: AuditType,
        includeChildren: Boolean
    ): Result<Long> {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Audit name required"))
        }
        val request = AuditCreateRequest(
            name = normalizedName,
            locationId = locationId,
            type = type,
            includeChildren = includeChildren
        )
        return auditRepository.createAudit(request)
    }
}
