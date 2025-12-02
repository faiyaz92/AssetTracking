package com.example.assettracking.presentation.tabs.model

sealed interface RoomListEvent {
    data class CreateRoom(val name: String, val description: String?) : RoomListEvent
    data class UpdateRoom(val id: Long, val name: String, val description: String?) : RoomListEvent
    data class DeleteRoom(val id: Long) : RoomListEvent
    object ClearMessage : RoomListEvent
}
