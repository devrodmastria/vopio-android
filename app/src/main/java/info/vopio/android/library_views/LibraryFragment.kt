package info.vopio.android.library_views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import info.vopio.android.data_model.SavedWord
import info.vopio.android.utilities.Constants
import info.vopio.android.utilities.IdentityGenerator
import info.vopio.android.data_model.SavedWordsAdapter
import timber.log.Timber
import info.vopio.android.R

private const val ARG_USERNAME = "param1"
private const val ARG_USER_EMAIL = "param2"

class LibraryFragment : Fragment() {

    private var localUsername: String? = null
    private var localUserEmail: String? = null

    lateinit var databaseRef : DatabaseReference
    lateinit var dataSnapshotList : DataSnapshot
    private val savedWordsList = mutableListOf<SavedWord>()

    lateinit var fragmentContainer: View
    lateinit var wordDetailFragment : Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            localUsername = it.getString(ARG_USERNAME)
            localUserEmail = it.getString(ARG_USER_EMAIL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        fragmentContainer = inflater.inflate(R.layout.fragment_library, container, false)

        val wordsAdapter = SavedWordsAdapter { word -> adapterOnClick(word) }
        val recyclerView: RecyclerView = fragmentContainer.findViewById(R.id.recyclerViewHost)

        activity?.title = getString(R.string.tab_lib)

        val headerView : TextView = fragmentContainer.findViewById(R.id.headerView)
        headerView.text = String.format(resources.getString(R.string.library_header), localUsername)

        databaseRef = FirebaseDatabase.getInstance().reference
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                // check email list against local user email
                dataSnapshotList = dataSnapshot

                val userId = IdentityGenerator().createUserIdFromEmail(localUserEmail.toString())
                val studentDataSnapshot : DataSnapshot = dataSnapshot.child(Constants.STUDENT_LIST).child(userId).child(Constants.SAVED_WORDS)
                if (studentDataSnapshot.hasChildren()){

                    savedWordsList.clear()
                    for (word in studentDataSnapshot.children){
                        val wordItem = word.value.toString()
                        val wordKey = word.key.toString()
                        savedWordsList.add(SavedWord(wordItem, wordKey))
                    }

                } else {
                    savedWordsList.clear()
                    savedWordsList.add(SavedWord(Constants.SAMPLE_WORD, Constants.SAMPLE_KEY))
                }
                wordsAdapter.submitList(savedWordsList)
                recyclerView.adapter = wordsAdapter

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Timber.i("-->>SpeechX: onDataChange Failed to read value:${error.toException()}")
            }
        })

        return fragmentContainer
    }

    private fun adapterOnClick(savedWord: SavedWord){
        // display info about card
        Timber.i("-->>SpeechX: adapterOnClick CLICK:$savedWord")
        wordDetailFragment = LibraryDetailFragment.newInstance(savedWord.content, savedWord.itemKey)
        parentFragmentManager.beginTransaction().replace(R.id.mainFragmentContainer, wordDetailFragment).commit()

    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LibraryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME, param1)
                    putString(ARG_USER_EMAIL, param2)
                }
            }
    }
}