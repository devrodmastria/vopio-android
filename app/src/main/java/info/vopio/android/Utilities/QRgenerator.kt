package info.vopio.android.Utilities

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.lang.IllegalArgumentException

class QRgenerator() {

    fun encodeToQR(qrValue : String, context : Context): Bitmap {

        val strideSize = 300

        var bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                qrValue,
                BarcodeFormat.QR_CODE,
                strideSize, strideSize, null
            )

        } catch (illegalArgumentException: IllegalArgumentException) {

            bitMatrix = MultiFormatWriter().encode(
                "QRgenerator encodeToQR catch",
                BarcodeFormat.QR_CODE,
                strideSize, strideSize, null
            )

        }

        val bitMatrixWidth = bitMatrix.width

        val bitMatrixHeight = bitMatrix.height

        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)

        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth

            for (x in 0 until bitMatrixWidth) {

                pixels[offset + x] = if (bitMatrix.get(x, y))
                    ContextCompat.getColor(context, android.R.color.black) // black
                else
                    ContextCompat.getColor(context, android.R.color.transparent) // white
            }
        }
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888)

        bitmap.setPixels(pixels, 0, strideSize, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap

    }

}