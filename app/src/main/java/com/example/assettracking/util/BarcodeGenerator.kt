package com.example.assettracking.util

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@Composable
fun rememberBarcodeImage(
    content: String,
    width: Int = 600,
    height: Int = 200
): ImageBitmap? {
    return remember(content, width, height) {
        if (content.isBlank()) {
            null
        } else {
            generateBarcodeBitmap(content, width, height)?.asImageBitmap()
        }
    }
}

fun generateBarcodeBitmap(
    content: String,
    width: Int,
    height: Int
): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.CODE_128,
            width,
            height
        )
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (exception: Exception) {
        null
    }
}
