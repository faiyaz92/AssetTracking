package com.example.assettracking.presentation.aichat

class LocalSqlFallbackServiceImpl(
    private val engine: LocalSqlFallbackEngine = LocalSqlFallbackEngine()
) : LocalSqlFallbackService {
    override fun generate(userMessage: String): String? = engine.generate(userMessage)
}
