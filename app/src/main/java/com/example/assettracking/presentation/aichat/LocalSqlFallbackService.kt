package com.example.assettracking.presentation.aichat

interface LocalSqlFallbackService {
    fun generate(userMessage: String): String?
}
