package info.vopio.android

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.zxing.Result
import info.vopio.android.databinding.ActivityMainBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    lateinit var localUsername: String
    lateinit var localUserEmail: String
    lateinit var thisFirebaseRemoteConfig: FirebaseRemoteConfig
    lateinit var thisFirebaseAuth : FirebaseAuth
    lateinit var thisFirebaseDatabaseReference : DatabaseReference
    lateinit var targetFragment : Fragment

    private lateinit var binding: ActivityMainBinding

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_logout -> {
                thisFirebaseAuth.signOut()
                AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener {
                        startActivity(Intent(this, OnboardingActivity::class.java))
                    }
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val themeColor = applicationContext.getColor(R.color.purple_300)
        window.statusBarColor = themeColor
        toolbar.setBackgroundColor(themeColor)

        Timber.plant(Timber.DebugTree())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        thisFirebaseAuth = FirebaseAuth.getInstance()
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
            finish() // finish it before it shows on the screen
        } else {

            thisFirebaseUser.displayName?.let {
                localUsername = it
            }
            thisFirebaseUser.email?.let {
                localUserEmail = it
            }
        }

        targetFragment = HostFragment.newInstance(localUsername, localUserEmail)
        supportFragmentManager.beginTransaction().replace(R.id.main_fragment_container, targetFragment).commit()

        binding.tabNavigation.setOnItemSelectedListener {

            when (it.itemId){
                R.id.tab_item_prof -> {
                    targetFragment = HostFragment.newInstance(localUsername, localUserEmail)

                }
                R.id.tab_item_stu -> {
                    targetFragment = GuestFragment.newInstance(localUsername, localUserEmail)

                }
                R.id.tab_item_lib -> {
                    targetFragment = LibraryFragment.newInstance(localUsername, localUserEmail)

                }
                else -> {
                    targetFragment = HostFragment.newInstance(localUsername, localUserEmail)
                }
            }

            supportFragmentManager.beginTransaction().replace(R.id.main_fragment_container, targetFragment).commit()
            return@setOnItemSelectedListener true

        }

    }

}
