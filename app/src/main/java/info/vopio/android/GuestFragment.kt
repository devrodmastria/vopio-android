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
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import info.vopio.android.Utilities.Constants
import timber.log.Timber

private const val ARG_USERNAME = "param1"
private const val ARG_USER_EMAIL = "param2"

class GuestFragment : Fragment() {

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

        val hintText = "Enter session code"
        val dialogTitle = "Enter session code\n(case sensitive)"

        val textInputView = EditText(fragmentContext)
        textInputView.inputType = InputType.TYPE_CLASS_TEXT
        textInputView.hint = hintText

        val alertDialogBuilder = AlertDialog.Builder(fragmentContext)
        alertDialogBuilder
            .setTitle(dialogTitle)
            .setView(textInputView)
            .setPositiveButton("Submit") { dialog, which ->

                if (dataSnapshotList.hasChildren()) {

                    var matchFound = false

                    for (snapshot in dataSnapshotList.getChildren()) {

                        val snapshotSize = snapshot.key?.length
                        val lastFourDigits = snapshot.key?.substring(snapshotSize!!.minus(4))
                        var inputString = textInputView.text.toString()
                        inputString = inputString.replace(" ", "")

                        if (inputString == lastFourDigits) {

                            matchFound = true

                            val intent = Intent(fragmentContext, GuestSessionActivity::class.java)
                            intent.putExtra(Constants.SESSION_KEY, snapshot.key)
                            intent.putExtra(Constants.SESSION_USERNAME, localUsername)
                            intent.putExtra(Constants.SESSION_USER_EMAIL, localUserEmail)
                            startActivity(intent)
                            break
                        }

                    }

                    if (!matchFound) {
                        Snackbar.make( fragmentContainer.rootView.findViewById(android.R.id.content), "Session not found", Snackbar.LENGTH_LONG).show()
                    }

                } else {
                    Snackbar.make( fragmentContainer.rootView.findViewById(android.R.id.content), "Sessions not available offline.", Snackbar.LENGTH_LONG).show()
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
            localUsername = it.getString(ARG_USERNAME)
            localUserEmail = it.getString(ARG_USER_EMAIL)
        }

        thisFirebaseAuth = FirebaseAuth.getInstance()
        thisFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        // Retrieve active sessions
        thisFirebaseDatabaseReference.child(Constants.SESSION_LIST)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshotList = dataSnapshot
                }

                override fun onCancelled(p0: DatabaseError) {
                    Timber.i("-->>SpeechX: DatabaseError ${p0.message}")
                }

            })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        activity?.title = getString(R.string.tab_student)

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
                    putString(ARG_USERNAME, param1)
                    putString(ARG_USER_EMAIL, param2)
                }
            }
    }
}