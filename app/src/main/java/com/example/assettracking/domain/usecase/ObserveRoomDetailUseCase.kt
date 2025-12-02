package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.RoomDetail
import com.example.assettracking.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRoomDetailUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    operator fun invoke(roomId: Long): Flow<RoomDetail?> = roomRepository.observeRoomDetail(roomId)
}
