package app.src.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utilidad para generar códigos QR como imágenes Bitmap
 */
object QRCodeGenerator {

    /**
     * Genera un código QR como Bitmap
     * @param text Texto a codificar en el QR
     * @param width Ancho de la imagen en píxeles (por defecto 512)
     * @param height Alto de la imagen en píxeles (por defecto 512)
     * @return Bitmap con el código QR o null si hay error
     */
    fun generateQRCode(
        text: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix: BitMatrix = qrCodeWriter.encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }

            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Versión optimizada para generar QR con mejor calidad visual
     * @param text Texto a codificar
     * @param size Tamaño del QR (cuadrado)
     * @return Bitmap del QR o null si hay error
     */
    fun generateHighQualityQRCode(
        text: String,
        size: Int = 400
    ): Bitmap? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix: BitMatrix = qrCodeWriter.encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
