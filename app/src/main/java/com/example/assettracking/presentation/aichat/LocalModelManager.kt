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
            downloadUrl = "https://transfer.sh/gem_model.bin", // Replace with your transfer.sh URL
            sizeBytes = 1_150_000_000L
        )
        LocalModel.TinyLlama -> LocalModelInfo(
            id = model,
            displayName = "TinyLlama",
            fileName = "tinyllama_fb.tflite",
            downloadUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0/resolve/main/model.safetensors?download=true",
            sizeBytes = 1_150_000_000L
        )
        LocalModel.Gemma3nE2B -> LocalModelInfo(
            id = model,
            displayName = "Gemma-3n-E2B-it",
            fileName = "gemma-3n-E2B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/ba9ca88da013b537b6ed38108be609b8db1c3a16/gemma-3n-E2B-it-int4.litertlm?download=true",
            sizeBytes = 3_656_827_456L, // 3.6GB
            description = "Gemma 3n E2B with vision and audio support, 4096 context length",
            supportsImage = true,
            supportsAudio = true,
            minMemoryGB = 8
        )
        LocalModel.Gemma3nE4B -> LocalModelInfo(
            id = model,
            displayName = "Gemma-3n-E4B-it",
            fileName = "gemma-3n-E4B-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/297ed75955702dec3503e00c2c2ecbbf475300bc/gemma-3n-E4B-it-int4.litertlm?download=true",
            sizeBytes = 4_919_541_760L, // 4.9GB
            description = "Gemma 3n E4B with vision and audio support, 4096 context length",
            supportsImage = true,
            supportsAudio = true,
            minMemoryGB = 12
        )
        LocalModel.Gemma31B -> LocalModelInfo(
            id = model,
            displayName = "Gemma3-1B-IT",
            fileName = "gemma3-1b-it-int4.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/42d538a932e8d5b12e6b3b455f5572560bd60b2c/gemma3-1b-it-int4.litertlm?download=true",
            sizeBytes = 584_417_280L, // 584MB
            description = "Gemma 3 1B model with 4-bit quantization, text-only",
            supportsImage = false,
            supportsAudio = false,
            minMemoryGB = 6
        )
        LocalModel.Qwen25_15B -> LocalModelInfo(
            id = model,
            displayName = "Qwen2.5-1.5B-Instruct",
            fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/19edb84c69a0212f29a6ef17ba0d6f278b6a1614/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
            sizeBytes = 1_597_931_520L, // 1.6GB
            description = "Qwen 2.5 1.5B instruction-tuned model, text-only",
            supportsImage = false,
            supportsAudio = false,
            minMemoryGB = 6
        )
        LocalModel.Phi4Mini -> LocalModelInfo(
            id = model,
            displayName = "Phi-4-mini-instruct",
            fileName = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/054f4e2694a86f81a129a40596e08b8d74770a9d/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
            sizeBytes = 3_910_090_752L, // 3.9GB
            description = "Microsoft Phi-4 mini instruction-tuned model, text-only",
            supportsImage = false,
            supportsAudio = false,
            minMemoryGB = 6
        )
        LocalModel.DeepSeekR1 -> LocalModelInfo(
            id = model,
            displayName = "DeepSeek-R1-Distill-Qwen-1.5B",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/e34bb88632342d1f9640bad579a45134eb1cf988/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
            sizeBytes = 1_833_451_520L, // 1.8GB
            description = "DeepSeek R1 distilled Qwen 1.5B model, text-only",
            supportsImage = false,
            supportsAudio = false,
            minMemoryGB = 6
        )
        LocalModel.TinyGarden -> LocalModelInfo(
            id = model,
            displayName = "TinyGarden-270M",
            fileName = "tiny_garden.litertlm",
            downloadUrl = "https://huggingface.co/google/functiongemma-270m-it/resolve/f54f8715e2b205f72c350f6efa748fd29fa19d98/tiny_garden.litertlm?download=true",
            sizeBytes = 288_440_320L, // 288MB
            description = "Function Gemma 270M for Tiny Garden game with function calling",
            supportsImage = false,
            supportsAudio = false,
            minMemoryGB = 6
        )
    }

    fun fileFor(model: LocalModel): File {
        val info = infoFor(model)
        return File(modelsDir, info.fileName)
    }

    fun isDownloaded(model: LocalModel): Boolean = fileFor(model).exists()

    suspend fun downloadModel(
        model: LocalModel,
        onProgress: (progress: Int, downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val info = infoFor(model)
        val targetFile = fileFor(model)
        val tmpFile = File(modelsDir, "${info.fileName}.part")

        val request = Request.Builder()
            .url(info.downloadUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .addHeader("Accept-Language", "en-US,en;q=0.5")
            .addHeader("Accept-Encoding", "identity")
            .addHeader("Connection", "keep-alive")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("Cache-Control", "max-age=0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: ${response.code}")

            val body = response.body ?: throw IOException("Empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes

            body.byteStream().use { input ->
                tmpFile.outputStream().use { output ->
                    copyWithProgress(input, output, total) { progress, downloadedBytes ->
                        onProgress(progress, downloadedBytes, total)
                    }
                }
            }
        }

        // Check file size
        val downloadedSize = tmpFile.length()
        if (downloadedSize < info.sizeBytes * 0.9) { // Allow 10% tolerance
            tmpFile.delete()
            throw IOException("Downloaded file size (${downloadedSize} bytes) is too small. Expected ~${info.sizeBytes} bytes. Download may have failed.")
        }

        // Replace old file if exists
        if (targetFile.exists()) targetFile.delete()
        if (!tmpFile.renameTo(targetFile)) throw IOException("Failed to rename temp model file")
        onProgress(100, downloadedSize, info.sizeBytes)
        return@withContext targetFile
    }

    private fun copyWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (progress: Int, downloadedBytes: Long) -> Unit
    ): Long {
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
                onProgress(progress, bytesCopied)
            }
        }
        return bytesCopied
    }
}

enum class LocalModel { 
    Gemma, 
    TinyLlama,
    Gemma3nE2B, 
    Gemma3nE4B, 
    Gemma31B, 
    Qwen25_15B,
    Phi4Mini,
    DeepSeekR1,
    TinyGarden 
}

data class LocalModelInfo(
    val id: LocalModel,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val description: String = "",
    val supportsImage: Boolean = false,
    val supportsAudio: Boolean = false,
    val minMemoryGB: Int = 6
)

data class ModelStatus(
    val isDownloaded: Boolean,
    val progress: Int = 0,
    val error: String? = null,
    val filePath: String? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadSpeed: String? = null
)
