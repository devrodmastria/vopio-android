package info.vopio.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import info.vopio.android.Utilities.Constants
import timber.log.Timber

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GuestFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GuestFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var localUsername: String? = null
    private var localUserEmail: String? = null

    lateinit var fragmentContext: Context
    lateinit var fragmentContainer: View

    lateinit var thisFirebaseAuth : FirebaseAuth
    lateinit var thisFirebaseDatabaseReference : DatabaseReference
    lateinit var dataSnapshotList : DataSnapshot

    fun joinSession(){

        //look for active sessions
        //fetch codes for active sessions
        //show a list of active sessions names?
        //check code against user input

        // Retrieve active sessions
        thisFirebaseDatabaseReference.root
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshotList = dataSnapshot
                }

                override fun onCancelled(p0: DatabaseError) {
                    Timber.i("-->>SpeechX: DatabaseError ${p0.message}")
                }

            })

        val textInputView = EditText(fragmentContext)
        textInputView.inputType = InputType.TYPE_CLASS_TEXT

        val alertDialogBuilder = AlertDialog.Builder(fragmentContext)
        alertDialogBuilder
            .setTitle("Enter session code\n(case sensitive)")
            .setView(textInputView)
            .setPositiveButton("Submit") { dialog, which ->

                if (dataSnapshotList.exists()) {

                    var matchFound = false

                    for (snapshot in dataSnapshotList.getChildren()) {

                        val snapshotSize = snapshot.key?.length
                        val lastFourDigits = snapshot.key?.substring(snapshotSize!!.minus(4))

                        if (lastFourDigits == textInputView.text.toString()) {

                            matchFound = true

                            Timber.i("-->>SpeechX: session code OK!")

                            val intent = Intent(fragmentContext, CaptionActivity::class.java)
                            intent.putExtra(Constants.SESSION_KEY, snapshot.key)
                            intent.putExtra(Constants.SESSION_USERNAME, localUsername)
                            intent.putExtra(Constants.SESSION_USER_EMAIL, localUserEmail)
                            startActivity(intent)
                            break
                        }

                    }

                    if (!matchFound) {
                        Timber.i("-->>SpeechX: dataSnapshotList Please try again")
                        Snackbar.make( fragmentContainer.rootView.findViewById(android.R.id.content), "Session not found", Snackbar.LENGTH_LONG).show()

                    }
                } else {
                    Toast.makeText(fragmentContext, "Sessions not available offline.", Toast.LENGTH_LONG)
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
                    alertDialog.getWindow()
                        ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                } else {
                    Timber.i("-->>SpeechX: textInputView lost focus")
                }
            }
        })

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            localUsername = it.getString(ARG_PARAM1)
            localUserEmail = it.getString(ARG_PARAM2)
        }

        thisFirebaseAuth = FirebaseAuth.getInstance()
        thisFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        fragmentContainer = inflater.inflate(R.layout.fragment_guest, container, false)
        fragmentContext = fragmentContainer.context

        val joinSessionButton : Button = fragmentContainer.findViewById(R.id.joinSessionBtn)
        joinSessionButton.setOnClickListener {
            joinSession()
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
         * @return A new instance of fragment GuestFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GuestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}