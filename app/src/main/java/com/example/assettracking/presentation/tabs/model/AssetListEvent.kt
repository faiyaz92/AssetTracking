package com.example.assettracking.presentation.tabs.model

sealed interface AssetListEvent {
    data class CreateAsset(val name: String, val details: String?, val condition: String?, val baseRoomId: Long?) : AssetListEvent
    data class UpdateAsset(val id: Long, val name: String, val details: String?, val condition: String?, val baseRoomId: Long?) : AssetListEvent
    data class DeleteAsset(val id: Long) : AssetListEvent
    data class UpdateSearch(val query: String) : AssetListEvent
    object ClearMessage : AssetListEvent
}
