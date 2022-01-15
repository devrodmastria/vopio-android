package info.vopio.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WordDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WordDetailFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var fragmentContainer: View
    lateinit var webSettings : WebSettings
    lateinit var localUserEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        fragmentContainer = inflater.inflate(R.layout.fragment_word_detail, container, false)

        val url =
            "https://duckduckgo.com/?q=define+$param1&t=ffab&ia=definition"

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
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}