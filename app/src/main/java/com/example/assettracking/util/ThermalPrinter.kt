package com.example.assettracking.util

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

// Function to convert bitmap to ESC-POS image format
fun bitmapToEscPosImage(bitmap: Bitmap, maxWidth: Int = 384): ByteArray {
    // Resize bitmap if too wide
    val scaledBitmap = if (bitmap.width > maxWidth) {
        val scale = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * scale).toInt()
        Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, false)
    } else {
        bitmap
    }

    val width = scaledBitmap.width
    val height = scaledBitmap.height
    val output = ByteArrayOutputStream()

    try {
        // ESC-POS command to print bitmap
        // GS v 0 m xL xH yL yH d1...dk
        output.write(0x1D) // GS
        output.write(0x76) // v
        output.write(0x30) // 0
        output.write(0x00) // m (normal mode)

        // Width in bytes (8 dots per byte)
        val widthBytes = (width + 7) / 8
        output.write(widthBytes % 256) // xL
        output.write(widthBytes / 256) // xH

        // Height
        output.write(height % 256) // yL
        output.write(height / 256) // yH

        // Bitmap data
        val pixels = IntArray(width * height)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byte = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = pixels[y * width + x]
                        val gray = (pixel shr 16 and 0xFF) * 0.299 + (pixel shr 8 and 0xFF) * 0.587 + (pixel and 0xFF) * 0.114
                        if (gray < 128) { // Dark pixel
                            byte = byte or (1 shl (7 - bit))
                        }
                    }
                }
                output.write(byte)
            }
        }

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }

    return output.toByteArray()
}

fun printBarcode(context: Context, assetCode: String, assetName: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val printerConnection = BluetoothPrintersConnections.selectFirstPaired()
            if (printerConnection == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val printer = EscPosPrinter(printerConnection, 203, 58f, 32) // 58mm width

            // Generate barcode bitmap (similar to rememberBarcodeImage but synchronous)
            val barcodeBitmap = generateBarcodeBitmap(assetCode, 400, 100) // Smaller for 58mm

            if (barcodeBitmap == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to generate barcode", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Print the receipt with proper formatting - compact spacing
            val textToPrint = """
                [C]<b>$assetName</b>
                [C]Code: $assetCode
            """.trimIndent()

            // Print text first
            printer.printFormattedText(textToPrint)

            // Print the barcode bitmap using ESC-POS commands
            val imageData = bitmapToEscPosImage(barcodeBitmap, 300) // 300px width for 58mm printer
            printerConnection.write(imageData)

            // Add minimal spacing at the end
            printer.printFormattedText("[L]\n")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Printed successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}