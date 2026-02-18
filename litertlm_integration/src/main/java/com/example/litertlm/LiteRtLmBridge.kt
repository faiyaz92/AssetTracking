package com.example.litertlm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig

private const val TAG = "LiteRtLmBridge"

object LiteRtLmBridge {
  fun canInitialize(): Boolean {
    return try {
      // Simple sanity: attempt to reference Engine class
      Engine::class.java
      true
    } catch (e: Throwable) {
      false
    }
  }

  fun initializeEngine(context: Context, modelPath: String): Boolean {
    return try {
      val config = EngineConfig(modelPath = modelPath)
      val engine = Engine(config)
      engine.initialize()
      // Keep engine alive via a singleton if desired. For now we just init.
      Log.i(TAG, "Engine initialized for $modelPath")
      true
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to initialize engine", e)
      false
    }
  }
}
