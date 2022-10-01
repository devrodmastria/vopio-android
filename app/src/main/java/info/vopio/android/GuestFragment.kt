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

// this fragment connects a Session Guest to a Session Host
class GuestFragment : Fragment() {

    private var localUsername: String? = null
    private var localUserEmail: String? = null

    lateinit var fragmentContext: Context
    lateinit var fragmentView: View

    lateinit var thisFirebaseAuth : FirebaseAuth
    lateinit var databaseRef : DatabaseReference
    lateinit var globalSessionListSnapshot : DataSnapshot

    private var inactiveSessionListSnapshot = mutableListOf<DataSnapshot>()
    lateinit var thisLinearLayoutManager : LinearLayoutManager
    lateinit var sampleSnapshot : DataSnapshot

    private fun joinSession(){

        // Logic pattern:
        // >look for active sessions
        // >fetch codes for active sessions
        // >show a list of active sessions names?
        // >check code against user input

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

                            val intent = Intent(fragmentContext, SessionGuestActivity::class.java)
                            intent.putExtra(Constants.SESSION_KEY, snapshot.key)
                            intent.putExtra(Constants.SESSION_USERNAME, localUsername)
                            intent.putExtra(Constants.SESSION_USER_EMAIL, localUserEmail)
                            startActivity(intent)
                            break
                        }

                    }

                    if (!matchFound) {
                        Snackbar.make( fragmentView.rootView.findViewById(android.R.id.content), "Session not found", Snackbar.LENGTH_LONG).show()
                    }

                } else {
                    Snackbar.make( fragmentView.rootView.findViewById(android.R.id.content), "Sessions not available offline.", Snackbar.LENGTH_LONG).show()
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
        savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment

        activity?.title = getString(R.string.tab_student)

        fragmentView = inflater.inflate(R.layout.fragment_guest, container, false)
        fragmentContext = fragmentView.context

        thisLinearLayoutManager = LinearLayoutManager(fragmentContext)
        thisLinearLayoutManager.reverseLayout = true // show latest item on top
        thisLinearLayoutManager.stackFromEnd = true // required to show end as top
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.recyclerViewGuest)
        recyclerView.layoutManager = thisLinearLayoutManager

        val joinSessionButton : Button = fragmentView.findViewById(R.id.joinSessionBtn)
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

        return fragmentView
    }

    private fun parseInactiveSessionList(dataSnapshot: DataSnapshot, recyclerView: RecyclerView){

        val sessionAdapter : SessionListAdapter
        val sessionDataSnapshot : DataSnapshot = dataSnapshot
        if (sessionDataSnapshot.hasChildren()){

            var matchFound = false
            inactiveSessionListSnapshot.clear()

            for (sessionItem in sessionDataSnapshot.children){

                if (sessionItem.key.toString().contains(Constants.DEMO_KEY)){
                    sampleSnapshot = sessionItem
                }

                for (sessionSubItem in sessionItem.child(Constants.ATTENDANCE_LIST).children){

                    val userID = IdentityGenerator().createUserIdFromEmail(localUserEmail.toString())
                    if (sessionSubItem.key == userID){

                        matchFound = true
                        inactiveSessionListSnapshot.add(sessionItem)
                    }
                }

            }

            if (!matchFound){
                inactiveSessionListSnapshot.clear()
                inactiveSessionListSnapshot.add(sampleSnapshot)
            }

            sessionAdapter = SessionListAdapter(inactiveSessionListSnapshot) { sessionId -> adapterOnClick(sessionId)}
            recyclerView.adapter = sessionAdapter
        }
    }

    private fun adapterOnClick(sessionId: String){

        val intent = Intent(fragmentContext, SessionGuestActivity::class.java)
        intent.putExtra(Constants.SESSION_KEY, sessionId)
        intent.putExtra(Constants.SESSION_USERNAME, localUsername)
        intent.putExtra(Constants.SESSION_USER_EMAIL, localUserEmail)
        intent.putExtra(Constants.REVIEW_MODE, true)
        startActivity(intent)
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