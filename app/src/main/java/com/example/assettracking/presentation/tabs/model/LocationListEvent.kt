package com.example.assettracking.presentation.tabs.model

sealed interface LocationListEvent {
    data class CreateLocation(val name: String, val description: String?) : LocationListEvent
    data class UpdateLocation(val id: Long, val name: String, val description: String?) : LocationListEvent
    data class DeleteLocation(val id: Long) : LocationListEvent
    object ClearMessage : LocationListEvent
}
