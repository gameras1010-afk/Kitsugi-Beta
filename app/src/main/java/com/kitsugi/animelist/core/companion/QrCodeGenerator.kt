package com.kitsugi.animelist.core.companion

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates a QR-code [Bitmap] that encodes a companion URL.
 *
 * The resulting bitmap uses:
 *  - Error correction level M (15 % data recovery)
 *  - Quiet zone of 1 module
 *  - Dark foreground (#1A1A2E) and transparent background for TV readability
 */
object QrCodeGenerator {

    private const val QUIET_ZONE = 1
    private const val DARK_COLOR = Color.BLACK
    private const val LIGHT_COLOR = Color.WHITE

    /**
     * Encodes [content] as a QR code of [sizePx] × [sizePx] pixels.
     *
     * @param content The URL / string to encode.
     * @param sizePx  The desired output bitmap dimension (must be > 0).
     * @return A [Bitmap] or null if encoding fails (e.g. content too long).
     */
    fun generate(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to QUIET_ZONE,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height) { i ->
                val x = i % width
                val y = i / width
                if (bitMatrix[x, y]) DARK_COLOR else LIGHT_COLOR
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (_: WriterException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
