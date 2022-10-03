package info.vopio.android.OnboardingViews

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import info.vopio.android.MainActivity
import info.vopio.android.R

import info.vopio.android.databinding.ActivitySignInBinding
import timber.log.Timber


class SignInActivity : AppCompatActivity(){

    private val signIn: ActivityResultLauncher<Intent> = registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult)
    private lateinit var binding: ActivitySignInBinding

    lateinit var thisFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        thisFirebaseAnalytics = FirebaseAnalytics.getInstance(applicationContext)

    }

    override fun onStart() {
        super.onStart()

        if (Firebase.auth.currentUser == null) {
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.mipmap.ic_launcher)
                .setAvailableProviders(listOf(
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                ))
                .build()

            signIn.launch(signInIntent)
        } else {
            startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {

            val eventMessage = "Finished_onboarding"
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "finished_onboard")
            thisFirebaseAnalytics.logEvent(eventMessage, bundle)

            startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        } else {
            Snackbar.make(binding.root, "No internet connection?", Snackbar.LENGTH_LONG).show()
            Timber.wtf("-->> onSignInResult " + result.resultCode)

            val eventMessage = "Login_error :" + result.resultCode
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "login_error")
            thisFirebaseAnalytics.logEvent(eventMessage, bundle)
        }
    }

}
