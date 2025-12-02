package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.RoomSummary
import com.example.assettracking.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRoomsUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    operator fun invoke(): Flow<List<RoomSummary>> = roomRepository.observeRoomSummaries()
}
