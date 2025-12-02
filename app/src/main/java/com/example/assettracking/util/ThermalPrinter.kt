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

            val textToPrint = """
                [C]<b>$assetName</b>
                [C]Code: $assetCode
                [C]${PrinterTextParserImg.bitmapToHexadecimalString(printer, barcodeBitmap)}
                [L]
                [L]
            """.trimIndent()

            printer.printFormattedText(textToPrint)

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