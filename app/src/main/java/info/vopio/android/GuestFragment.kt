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
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import info.vopio.android.DataModel.SessionListAdapter
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.IdentityGenerator
import timber.log.Timber

private const val ARG_USERNAME = "param1"
private const val ARG_USER_EMAIL = "param2"

class GuestFragment : Fragment() {

    private var localUsername: String? = null
    private var localUserEmail: String? = null

    lateinit var fragmentContext: Context
    lateinit var fragmentContainer: View

    lateinit var thisFirebaseAuth : FirebaseAuth
    lateinit var databaseRef : DatabaseReference
    lateinit var globalSessionListSnapshot : DataSnapshot

    private var inactiveSessionListSnapshot = mutableListOf<DataSnapshot>()
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    private fun joinSession(){

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

                if (globalSessionListSnapshot.hasChildren()) {

                    var matchFound = false

                    for (snapshot in globalSessionListSnapshot.getChildren()) {

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

        alertDialog.setOnShowListener {
            textInputView.requestFocus()
        }

        alertDialog.show() // show() must be called last

        textInputView.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (hasFocus) {
                    alertDialog.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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

        // Retrieve active sessions
        databaseRef = FirebaseDatabase.getInstance().reference

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        activity?.title = getString(R.string.tab_student)

        fragmentContainer = inflater.inflate(R.layout.fragment_guest, container, false)
        fragmentContext = fragmentContainer.context

        thisLinearLayoutManager = LinearLayoutManager(fragmentContext)
        thisLinearLayoutManager.stackFromEnd = false
        val recyclerView: RecyclerView = fragmentContainer.findViewById(R.id.recyclerViewGuest)
        recyclerView.layoutManager = thisLinearLayoutManager

        val joinSessionButton : Button = fragmentContainer.findViewById(R.id.joinSessionBtn)
        joinSessionButton.setOnClickListener {
            joinSession()
        }

        databaseRef.child(Constants.SESSION_LIST)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    globalSessionListSnapshot = dataSnapshot
                    parseInactiveSessionList(dataSnapshot, recyclerView)
                }

                override fun onCancelled(p0: DatabaseError) {
                    Timber.i("-->>SpeechX: DatabaseError ${p0.message}")
                }
            })

        return fragmentContainer
    }

    private fun parseInactiveSessionList(dataSnapshot: DataSnapshot, recyclerView: RecyclerView){

        val sessionAdapter: SessionListAdapter
        val sessionDataSnapshot : DataSnapshot = dataSnapshot
        if (sessionDataSnapshot.hasChildren()){

            var matchFound = false
            inactiveSessionListSnapshot.clear()

            for (sessionItem in sessionDataSnapshot.children){

                for (sessionSubItem in sessionItem.child(Constants.ATTENDANCE_LIST).children){

                    val userID = IdentityGenerator().createUserIdFromEmail(localUserEmail.toString())
                    if (sessionSubItem.key == userID){
                        matchFound = true
                        inactiveSessionListSnapshot.add(sessionItem)
                    }

                }

            }
            recyclerView.isVisible = matchFound

            sessionAdapter = SessionListAdapter(inactiveSessionListSnapshot)
            recyclerView.adapter = sessionAdapter
        }
    }

    companion object {

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