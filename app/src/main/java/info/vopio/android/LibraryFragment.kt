package info.vopio.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import info.vopio.android.DataModel.Word
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.DatabaseStringAdapter
import timber.log.Timber

private const val ARG_USERNAME = "param1"
private const val ARG_USER_EMAIL = "param2"

class LibraryFragment : Fragment() {

    private var localUsername: String? = null
    private var localUserEmail: String? = null

    lateinit var databaseRef : DatabaseReference
    lateinit var dataSnapshotList : DataSnapshot
    private val savedWordsList = mutableListOf<Word>()

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
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        fragmentContainer = inflater.inflate(R.layout.fragment_library, container, false)
        val wordsAdapter = WordsAdapter { word -> adapterOnClick(word) }
        val recyclerView: RecyclerView = fragmentContainer.findViewById(R.id.recyclerView)

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

                val userId = DatabaseStringAdapter().createUserIdFromEmail(localUserEmail.toString())
                val studentDataSnapshot : DataSnapshot = dataSnapshot.child(Constants.STUDENT_LIST).child(userId).child(Constants.SAVED_WORDS)
                if (studentDataSnapshot.hasChildren()){

                    savedWordsList.clear()
                    for (word in studentDataSnapshot.children){
                        val wordItem = word.value.toString()
                        val wordKey = word.key.toString()
                        savedWordsList.add(Word(wordItem, wordKey))
                    }

                } else {
                    savedWordsList.clear()
                    savedWordsList.add(Word("Sample", "sample_key"))
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

    private fun adapterOnClick(word: Word){
        // display info about card
        Timber.i("-->>SpeechX: adapterOnClick CLICK:$word")
        wordDetailFragment = WordDetailFragment.newInstance(word.content, word.itemKey)
        parentFragmentManager.beginTransaction().replace(R.id.main_fragment_container, wordDetailFragment).commit()

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LibraryFragment.
         */
        // TODO: Rename and change types and number of parameters
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