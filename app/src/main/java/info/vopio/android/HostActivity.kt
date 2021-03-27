package info.vopio.android

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import info.vopio.android.databinding.ActivityHostBinding
import timber.log.Timber
import java.lang.IllegalArgumentException

class HostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.plant(Timber.DebugTree())

    }

    override fun onStart() {
        super.onStart()

        startSession()

    }

    private fun startSession(){

        try {
            val bitMapQR = encodeToQR("LTU session key")
            binding.QRimageView.setImageBitmap(bitMapQR)
        } catch (illegalArg: WriterException) {
            Timber.i("-->>SpeechX: QR error %s", illegalArg)

        }

    }

    private fun encodeToQR(qrValue : String): Bitmap {

        var bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                qrValue,
                BarcodeFormat.QR_CODE,
                500, 500, null
            )

        } catch (illegalArgumentException: IllegalArgumentException) {

            bitMatrix = MultiFormatWriter().encode(
                "HostActivity encodeToQR catch",
                BarcodeFormat.QR_CODE,
                500, 500, null
            )

        }

        val bitMatrixWidth = bitMatrix.width

        val bitMatrixHeight = bitMatrix.height

        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)

        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth

            for (x in 0 until bitMatrixWidth) {

                pixels[offset + x] = if (bitMatrix.get(x, y))
                    ContextCompat.getColor(this, android.R.color.black) // black
                else
                    ContextCompat.getColor(this, android.R.color.transparent) // white
            }
        }
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888)

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap

    }

}
