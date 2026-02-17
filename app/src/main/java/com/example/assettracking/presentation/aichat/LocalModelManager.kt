package com.example.assettracking.presentation.aichat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt

/**
 * Shared manager to handle model availability and downloads.
 */
class LocalModelManager(private val context: Context) {

    private val client = OkHttpClient()
    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply { mkdirs() }
    }

    fun infoFor(model: LocalModel): LocalModelInfo = when (model) {
        LocalModel.Gemma -> LocalModelInfo(
            id = model,
            displayName = "Gemma",
            fileName = "gem_model.bin",
            // HuggingFace LiteRT Gemma3 1B IT int4 task
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
            sizeBytes = 1_150_000_000L
        )
        LocalModel.TinyLlama -> LocalModelInfo(
            id = model,
            displayName = "TinyLlama",
            fileName = "tinyllama_fb.tflite",
            // HuggingFace LiteRT TinyLlama 1.1B Chat q8 multi-prefill
            downloadUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
            sizeBytes = 1_150_000_000L
        )
    }

    fun fileFor(model: LocalModel): File {
        val info = infoFor(model)
        return File(modelsDir, info.fileName)
    }

    fun isDownloaded(model: LocalModel): Boolean = fileFor(model).exists()

    suspend fun downloadModel(
        model: LocalModel,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val info = infoFor(model)
        val targetFile = fileFor(model)
        val tmpFile = File(modelsDir, "${info.fileName}.part")

        val request = Request.Builder().url(info.downloadUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")

            val body = response.body ?: throw IOException("Empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes

            body.byteStream().use { input ->
                tmpFile.outputStream().use { output ->
                    copyWithProgress(input, output, total, onProgress)
                }
            }
        }

        // Replace old file if exists
        if (targetFile.exists()) targetFile.delete()
        if (!tmpFile.renameTo(targetFile)) throw IOException("Failed to rename temp model file")
        onProgress(100)
        return@withContext targetFile
    }

    private fun copyWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (Int) -> Unit
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesCopied: Long = 0
        var read: Int
        while (true) {
            read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            bytesCopied += read
            if (totalBytes > 0) {
                val progress = ((bytesCopied.toDouble() / totalBytes.toDouble()) * 100).roundToInt().coerceIn(0, 100)
                onProgress(progress)
            }
        }
    }
}

enum class LocalModel { Gemma, TinyLlama }

data class LocalModelInfo(
    val id: LocalModel,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class ModelStatus(
    val isDownloaded: Boolean,
    val progress: Int = 0,
    val error: String? = null,
    val filePath: String? = null
)
