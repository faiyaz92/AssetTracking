@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.domain.model.RoomSummary
import com.example.assettracking.presentation.tabs.model.RoomListEvent
import com.example.assettracking.presentation.tabs.viewmodel.RoomListViewModel

@Composable
fun RoomsScreen(
    onBack: () -> Unit,
    onOpenRoom: (Long) -> Unit,
    viewModel: RoomListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message?.text
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(RoomListEvent.ClearMessage)
        }
    }

    var showRoomDialog by rememberSaveable { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<RoomSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rooms") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRoom = null
                    showRoomDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add room")
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(paddingValues).fillMaxSize())
            state.rooms.isEmpty() -> EmptyState(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                message = "No rooms yet. Tap + to add one."
            )
            else -> LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.rooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        onClick = { onOpenRoom(room.id) },
                        onEdit = {
                            editingRoom = room
                            showRoomDialog = true
                        },
                        onDelete = {
                            viewModel.onEvent(RoomListEvent.DeleteRoom(room.id))
                        }
                    )
                }
            }
        }
    }

    if (showRoomDialog) {
        val room = editingRoom
        RoomFormDialog(
            title = if (room == null) "Add Room" else "Edit Room",
            initialName = room?.name.orEmpty(),
            initialDescription = room?.description.orEmpty(),
            onDismiss = { showRoomDialog = false },
            onConfirm = { name, description ->
                if (room == null) {
                    viewModel.onEvent(RoomListEvent.CreateRoom(name, description))
                } else {
                    viewModel.onEvent(RoomListEvent.UpdateRoom(room.id, name, description))
                }
                showRoomDialog = false
            }
        )
    }
}

@Composable
private fun RoomCard(
    room: RoomSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = room.name, style = MaterialTheme.typography.titleMedium)
            room.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Assets: ${room.assetCount}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            RowActions(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun RowActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEdit) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit room")
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete room")
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier, message: String) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message)
    }
}

@Composable
private fun RoomFormDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by rememberSaveable(initialName, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName))
    }
    var description by rememberSaveable(initialDescription, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialDescription))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.text, description.text.takeIf { it.isNotBlank() })
                },
                enabled = name.text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
