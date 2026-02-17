package com.example.assettracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "is_user")
    val isUser: Boolean,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "is_write_query")
    val isWriteQuery: Boolean = false,
    @ColumnInfo(name = "query_executed")
    val queryExecuted: Boolean = false,
    @ColumnInfo(name = "original_query")
    val originalQuery: String? = null
)