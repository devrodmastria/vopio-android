package info.vopio.android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import info.vopio.android.guest_views.GuestFragment
import info.vopio.android.host_views.HostFragment
import info.vopio.android.library_views.LibraryFragment
import info.vopio.android.databinding.ActivityMainBinding
import info.vopio.android.onboarding.OnboardingActivity
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration : AppBarConfiguration

    private var localUsername: String = "user_name"
    private var localUserEmail: String = "user_email"

    lateinit var thisFirebaseRemoteConfig: FirebaseRemoteConfig
    lateinit var thisFirebaseAuth : FirebaseAuth
    lateinit var thisFirebaseDatabaseReference : DatabaseReference
    lateinit var targetFragment : Fragment

    private lateinit var binding: ActivityMainBinding

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu_items, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.option_logout -> {
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

        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainFragmentContainer) as NavHostFragment
        val navController = navHostFragment.navController
//        appBarConfiguration = AppBarConfiguration(
//            setOf(R.id._about_app), binding.drawerLayout)

        Timber.plant(Timber.DebugTree())

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

            targetFragment = HostFragment.newInstance(localUsername, localUserEmail)
            supportFragmentManager.beginTransaction().replace(R.id.mainFragmentContainer, targetFragment).commit()

        }

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
            
            supportFragmentManager.beginTransaction().replace(R.id.mainFragmentContainer, targetFragment).commit()
            return@setOnItemSelectedListener true

        }

    }

}
