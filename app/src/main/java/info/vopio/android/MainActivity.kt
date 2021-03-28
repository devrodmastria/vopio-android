package info.vopio.android

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.zxing.Result
import info.vopio.android.databinding.ActivityMainBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber

class MainActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    lateinit var xingScannerView: ZXingScannerView
    lateinit var localUser: String
    lateinit var thisFirebaseRemoteConfig: FirebaseRemoteConfig
    lateinit var thisFirebaseDatabaseReference : DatabaseReference
    lateinit var dataSnapshotList : DataSnapshot
    private lateinit var binding: ActivityMainBinding

    companion object {
        private val REQUEST_CAMERA = 1 // this is used to request permission from user
        val SESSION_KEY = "SESSION_KEY"
        val SESSION_USER = "SESSION_USER"
        val THIS_IS_THE_HOST = "_isHost_"
        val THIS_IS_NOT_A_HOST = "_notHost_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        Timber.plant(Timber.DebugTree())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        thisFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        thisFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(5)
            .build()
        thisFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val thisFirebaseAuth = FirebaseAuth.getInstance()
        val thisFirebaseUser = thisFirebaseAuth.currentUser
        if (thisFirebaseUser == null){
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            localUser = thisFirebaseUser.displayName ?: "username"
        }

        binding.hostSessionButton.setOnClickListener {

            var hostCode = THIS_IS_NOT_A_HOST

            thisFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Timber.i("-->>SpeechX: Fetch and activate succeeded: $updated")
                        hostCode = thisFirebaseRemoteConfig.getLong(THIS_IS_THE_HOST).toString()

                    } else {
                        Timber.i("-->>SpeechX: Fetch and activate failed")
                    }
                }

            val textInputView = EditText(this)
            textInputView.inputType = InputType.TYPE_CLASS_NUMBER

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder
                .setTitle("Please enter host code")
                .setView(textInputView)
                .setCancelable(true)
                .setPositiveButton("Submit") { dialog, which ->
                    if (hostCode != THIS_IS_NOT_A_HOST && hostCode == textInputView.text.toString()) {
                        Timber.i("-->>SpeechX: host code OK!")

                        val intent = Intent(this, CaptionActivity::class.java)
                        intent.putExtra(SESSION_KEY, THIS_IS_THE_HOST)
                        intent.putExtra(SESSION_USER, localUser)
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, "Please try again.", Toast.LENGTH_LONG).show()
                        Timber.i("-->>SpeechX: host code incorrect")
                    }
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            textInputView.setOnFocusChangeListener(object : View.OnFocusChangeListener {
                override fun onFocusChange(v: View?, hasFocus: Boolean) {
                    if (hasFocus) {
                        alertDialog.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    }
                }
            })


        }

        binding.scanButton.setOnClickListener {

            //look for active sessions
            //fetch codes for active sessions
            //show a list of active sessions names?
            //check code against user input

            thisFirebaseDatabaseReference.root
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        dataSnapshotList = dataSnapshot

                        for (snapshot in dataSnapshot.getChildren()) {

                            val snapshotSize = snapshot.key?.length

                            val lastFourDigits = snapshot.key?.substring(snapshotSize!!.minus(4))

                            Timber.i("-->>SpeechX: session id $lastFourDigits")

                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        Timber.i("-->>SpeechX: DatabaseError ${p0.message}")
                    }

                })

            val textInputView = EditText(this)
            textInputView.inputType = InputType.TYPE_CLASS_TEXT

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder
                .setTitle("Please enter session code\n(case sensitive)")
                .setView(textInputView)
                .setNeutralButton("Scan QR") { dialog, which ->

                    //Request user permission to use camera
                    if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startScanner()
                    } else {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            MainActivity.REQUEST_CAMERA
                        )
                    }

                }
                .setPositiveButton("Submit") { dialog, which ->

                    if (dataSnapshotList.exists()) {

                        var matchFound = false

                        for (snapshot in dataSnapshotList.getChildren()) {

                            val snapshotSize = snapshot.key?.length
                            val lastFourDigits = snapshot.key?.substring(snapshotSize!!.minus(4))

                            if (lastFourDigits == textInputView.text.toString()) {

                                matchFound = true

                                Timber.i("-->>SpeechX: session code OK!")

                                val intent = Intent(this@MainActivity, CaptionActivity::class.java)
                                intent.putExtra(SESSION_KEY, snapshot.key)
                                intent.putExtra(SESSION_USER, localUser)
                                startActivity(intent)
                                finish()
                            }

                        }

                        if (!matchFound) {
                            Timber.i("-->>SpeechX: dataSnapshotList Please try again")
                            Toast.makeText(this, "Please try again.", Toast.LENGTH_LONG).show()

                        }
                    } else {
                        Toast.makeText(this, "Sessions not available offline.", Toast.LENGTH_LONG)
                            .show()
                        Timber.i("-->>SpeechX: dataSnapshotList invalid")
                    }

                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            textInputView.setOnFocusChangeListener(object : View.OnFocusChangeListener {
                override fun onFocusChange(v: View?, hasFocus: Boolean) {
                    if (hasFocus) {
                        alertDialog.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    } else {
                        Timber.i("-->>SpeechX: textInputView lost focus")
                    }
                }
            })

        }

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
            intent.putExtra(SESSION_USER, localUser)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show()
            setContentView(R.layout.activity_main)
        }

    }

}
