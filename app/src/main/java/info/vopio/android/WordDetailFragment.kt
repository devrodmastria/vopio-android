package info.vopio.android

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
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.DatabaseStringAdapter
import timber.log.Timber

private const val ARG_WORD = "param1"
private const val ARG_WORD_KEY = "param2"

class WordDetailFragment : Fragment() {

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
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

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
            parentFragmentManager.beginTransaction().replace(R.id.main_fragment_container, libFragment).commit()
        }

        val delBtn : Button = fragmentContainer.findViewById(R.id.deleteWordButton)
        delBtn.setOnClickListener {

            val userId = DatabaseStringAdapter().createUserIdFromEmail(localUserEmail)
            databaseRef.child(Constants.STUDENT_LIST).child(userId)
                .child(Constants.SAVED_WORDS).child(selectedWordKey.toString()).setValue(null).addOnSuccessListener {

                    val libFragment = LibraryFragment.newInstance("localUsername", localUserEmail)
                    parentFragmentManager.beginTransaction().replace(R.id.main_fragment_container, libFragment).commit()
                }
                .addOnFailureListener {
                    Timber.i("-->>WordDetailFragment DELETE Fail")
                }
        }

        return fragmentContainer
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WordDetailFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WordDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORD, param1)
                    putString(ARG_WORD_KEY, param2)
                }
            }
    }
}