package info.vopio.android.library_views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import info.vopio.android.utilities.Constants
import info.vopio.android.utilities.IdentityGenerator
import timber.log.Timber
import info.vopio.android.R

private const val ARG_WORD = "param1"
private const val ARG_WORD_KEY = "param2"

class LibraryDetailFragment : Fragment() {

    private var selectedWord: String? = null
    private var selectedWordKey: String? = null

    lateinit var fragmentContainer: View
    lateinit var webSettings : WebSettings
    lateinit var localUserEmail: String
    lateinit var databaseRef : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedWord = it.getString(ARG_WORD)
            selectedWordKey = it.getString(ARG_WORD_KEY)
        }

        databaseRef = FirebaseDatabase.getInstance().reference

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        fragmentContainer = inflater.inflate(R.layout.fragment_word_detail, container, false)

        val url =
            "https://duckduckgo.com/?q=define+$selectedWord&t=ffab&ia=definition"

        val webView: WebView = fragmentContainer.findViewById(R.id.webView)

        webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        webView.loadUrl(url)

        val thisFirebaseAuth = FirebaseAuth.getInstance()
        val thisFirebaseUser = thisFirebaseAuth.currentUser
        if (thisFirebaseUser != null){
            localUserEmail = thisFirebaseUser.email.toString()
        }

        val backBtn : Button = fragmentContainer.findViewById(R.id.backButton)
        backBtn.setOnClickListener {

            Timber.i("-->>SpeechX: backBtn CLICK")

            val libFragment = LibraryFragment.newInstance("localUsername", localUserEmail)
            parentFragmentManager.beginTransaction().replace(R.id.mainFragmentContainer, libFragment).commit()
        }

        val delBtn : Button = fragmentContainer.findViewById(R.id.deleteWordButton)
        delBtn.isEnabled = (selectedWord != Constants.SAMPLE_WORD)
        delBtn.setOnClickListener {

            val userId = IdentityGenerator().createUserIdFromEmail(localUserEmail)
            databaseRef.child(Constants.STUDENT_LIST).child(userId)
                .child(Constants.SAVED_WORDS).child(selectedWordKey.toString()).setValue(null).addOnSuccessListener {

                    val libFragment = LibraryFragment.newInstance("localUsername", localUserEmail)
                    parentFragmentManager.beginTransaction().replace(R.id.mainFragmentContainer, libFragment).commit()
                }
                .addOnFailureListener {
                    Timber.i("-->>WordDetailFragment DELETE Fail")
                }
        }

        return fragmentContainer
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LibraryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORD, param1)
                    putString(ARG_WORD_KEY, param2)
                }
            }
    }
}