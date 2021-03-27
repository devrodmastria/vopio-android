package info.vopio.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import info.vopio.android.databinding.ActivitySignInBinding


class SignInActivity : AppCompatActivity() {

    lateinit var thisGoogleSignInClient: GoogleSignInClient
    lateinit var thisFirebaseAuth : FirebaseAuth
    private lateinit var binding: ActivitySignInBinding

    companion object {
        private const val TAG = "SignInActivity"
        const val GOOGLE_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        thisFirebaseAuth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        thisGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.signInButton.setOnClickListener {
            signIn()
        }

    }

    private fun signIn() {
        val signInIntent: Intent = thisGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGooogle:" + acct.id)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        thisFirebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                Log.d(TAG,
                    "signInWithCredential:onComplete:" + task.isSuccessful)

                // If sign in fails, display a message to the user. If sign in succeeds
                // the auth state listener will be notified and logic to handle the
                // signed in user can be handled in the listener.
                if (!task.isSuccessful) {
                    Log.w(TAG, "signInWithCredential", task.exception)
                    Toast.makeText(this@SignInActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                    finish()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result != null && result.isSuccess) {
                // Google Sign-In was successful, authenticate with Firebase
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                // Google Sign-In failed
                Log.e("signInWithCredential", "Google Sign-In failed.")
            }
        }

    }
}
