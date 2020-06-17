package info.vopio.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_main.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber

class MainActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    lateinit var xingScannerView: ZXingScannerView

    companion object {
        private val REQUEST_CAMERA = 1 // this is used to request permission from user
        val SESSION_KEY = "SESSION_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val thisFirebaseAuth = FirebaseAuth.getInstance()
        val thisFirebaseUser = thisFirebaseAuth.currentUser
        if (thisFirebaseUser == null){
            //launch sign in activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        hostButton.setOnClickListener {
            //createNewSession()
        }

        scanButton.setOnClickListener {

            // Request user permission to use camera
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

                startScanner()

            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    MainActivity.REQUEST_CAMERA
                )
            }

        }

    }

    private fun createNewSession(){

        val intent = Intent(this@MainActivity, HostActivity::class.java)
        startActivity(intent)
        finish()

    }

    private fun startScanner(){
        xingScannerView = ZXingScannerView(this@MainActivity)
        setContentView(xingScannerView)
        xingScannerView.setResultHandler(this@MainActivity)
        xingScannerView.startCamera()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (permission in grantResults){
            if (permission == PackageManager.PERMISSION_GRANTED){
                startScanner()
            }
        }
    }

    override fun handleResult(p0: Result?) { //results from QR scanner

        val QRresult: String = p0?.text.toString()

        xingScannerView.stopCamera()

        if (QRresult.length == 20) {
            xingScannerView.stopCamera()
            val intent = Intent(this@MainActivity, CaptionActivity::class.java)
            intent.putExtra(SESSION_KEY, QRresult)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show()
            setContentView(R.layout.activity_main)
        }

    }

}
