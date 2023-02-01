package info.vopio.android

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import info.vopio.android.DataModel.SessionListAdapter
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.SwipeActions
import info.vopio.android.Utilities.SwipeHandler
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_USERNAME = "param1"
private const val ARG_USER_EMAIL = "param2"

// This fragment is the Session Launcher
class HostFragment : Fragment() {

    private var localUsername: String = "user_name"
    private var localUserEmail: String = "user_email"

    lateinit var fragmentContext: Context
    lateinit var fragmentContainer: View
    lateinit var databaseRef : DatabaseReference
    lateinit var newSessionID : String
    lateinit var hostListSnapshot : DataSnapshot
    lateinit var globalSessionListSnapshot : DataSnapshot

    private var inactiveSessionListSnapshot = mutableListOf<DataSnapshot>()
    lateinit var thisLinearLayoutManager : LinearLayoutManager
    lateinit var sampleSnapshot : DataSnapshot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            localUsername = it.getString(ARG_USERNAME).toString()
            localUserEmail = it.getString(ARG_USER_EMAIL).toString()
        }

        databaseRef = FirebaseDatabase.getInstance().reference
        databaseRef.child(Constants.HOST_LIST)
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                // check email list against local user email -- when creating new session must have host credential
                hostListSnapshot = dataSnapshot
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Timber.i("-->>SpeechX: onCancelled:${error.toException()}")
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        activity?.title = getString(R.string.tab_instructor)

        fragmentContainer = inflater.inflate(R.layout.fragment_host, container, false)
        fragmentContext = fragmentContainer.context

        thisLinearLayoutManager = LinearLayoutManager(fragmentContext)
        thisLinearLayoutManager.reverseLayout = true // show latest item on top
        thisLinearLayoutManager.stackFromEnd = true // required to show end as top
        val recyclerView: RecyclerView = fragmentContainer.findViewById(R.id.recyclerViewHost)
        recyclerView.layoutManager = thisLinearLayoutManager

        // todo: hide this delete button inside menu options
        val deleteBtn : Button = fragmentContainer.findViewById(R.id.deleteSessionsBtn)
        deleteBtn.setOnClickListener {

            if (inactiveSessionListSnapshot.isNotEmpty()){
                for (sessionItem in inactiveSessionListSnapshot){

                    databaseRef.child(Constants.SESSION_LIST).child(sessionItem.key.toString()).setValue(null)
                        .addOnFailureListener {
                            Timber.i("-->>HostFragment DELETE Fail")
                        }
                }
            }
        }

        val hostBtn : Button = fragmentContainer.findViewById(R.id.hostSessionBtn)
        hostBtn.setOnClickListener {
            hostNewSession(it)
        }

        databaseRef.child(Constants.SESSION_LIST)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.

                    // check email list against local user email -- when creating new session must have host credential
                    globalSessionListSnapshot = dataSnapshot
                    parseInactiveSessionList(dataSnapshot, recyclerView, deleteBtn)

                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Timber.i("-->>SpeechX: onDataChange Failed to read value:${error.toException()}")
                }
            })

        setupRecyclerSwipe(recyclerView)
        return fragmentContainer
    }

    private fun parseInactiveSessionList(dataSnapshot: DataSnapshot,recyclerView: RecyclerView, deleteBtn: Button){

        val sessionAdapter: SessionListAdapter
        val sessionDataSnapshot : DataSnapshot = dataSnapshot
        if (sessionDataSnapshot.hasChildren()){

            var matchFound = false
            inactiveSessionListSnapshot.clear()

            for (sessionItem in sessionDataSnapshot.children){

                if (sessionItem.key.toString().contains(Constants.DEMO_KEY)){
                    sampleSnapshot = sessionItem
                }

                val hostEmail = sessionItem.child(Constants.HOST_EMAIL).value.toString()
                if (hostEmail == localUserEmail){
                    matchFound = true
                    inactiveSessionListSnapshot.add(sessionItem)
                }

            }

            deleteBtn.isVisible = matchFound // prevent user from deleting DEMO session
            if (!matchFound){
                inactiveSessionListSnapshot.add(sampleSnapshot)
            }

            sessionAdapter = SessionListAdapter(inactiveSessionListSnapshot) { sessionId -> adapterOnClick(sessionId)}
            recyclerView.adapter = sessionAdapter
        }
    }

    private fun setupRecyclerSwipe(recyclerView: RecyclerView){

        val swipeController =
            SwipeHandler(object : SwipeActions() {

                override fun onDeleteClicked(position: Int) {
                    super.onDeleteClicked(position)

                    deleteSingleSession(position)
                }

                override fun onRenameClicked(position: Int) {
                    super.onRenameClicked(position)

                    renameSession(position)
                }
            })

        val itemTouchHelper = ItemTouchHelper(swipeController)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                super.onDraw(c, parent, state)
                swipeController.onDraw(c)
            }
        })
    }

    private fun renameSession(itemIndexPath: Int){

        Timber.i("-->>SpeechX: onRenameClicked value:${inactiveSessionListSnapshot[itemIndexPath].key}")

        val dialogTitle = "Enter new title"
        val dialogMessage = "Title needs to have at least 4 characters"
        val hintText = "At least 4 characters"

        val textInputView = EditText(fragmentContext)
        textInputView.inputType = InputType.TYPE_CLASS_TEXT
        textInputView.hint = hintText

        val alertDialogBuilder = AlertDialog.Builder(fragmentContext)
        alertDialogBuilder
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setView(textInputView)
            .setPositiveButton("Rename") { dialog, which ->

                val inputName = textInputView.text.toString()
                if (inputName.length >= 4){

                    val sessionKey = inactiveSessionListSnapshot[itemIndexPath].key
                    if (inactiveSessionListSnapshot.isNotEmpty()){

                        databaseRef.child(Constants.SESSION_LIST).child(sessionKey.toString()).child(Constants.SESSION_TITLE).setValue(inputName)
                            .addOnFailureListener {
                                Timber.i("-->>HostFragment renameSession Fail")
                            }
                    }
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

    }

    private fun deleteSingleSession(itemIndexPath: Int){

        Timber.i("-->>SpeechX: onDeleteClicked value:${inactiveSessionListSnapshot[itemIndexPath].key}")

        val dialogTitle = "Delete this session?"
        val dialogMessage = "This action cannot be undone"

        val alertDialogBuilder = AlertDialog.Builder(fragmentContext)
        alertDialogBuilder
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton("Yes") { dialog, which ->

                val sessionKey = inactiveSessionListSnapshot[itemIndexPath].key
                if (inactiveSessionListSnapshot.isNotEmpty()){

                    databaseRef.child(Constants.SESSION_LIST).child(sessionKey.toString()).setValue(null)
                        .addOnFailureListener {
                            Timber.i("-->>HostFragment DELETE Fail")
                        }
                }

            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show() // show() must be called last

    }

    private fun adapterOnClick(sessionId: String){

        Toast.makeText(fragmentContext, "No action available", Toast.LENGTH_SHORT).show()

    }

    private fun hostNewSession(view: View){

        // todo: add audios recorder system to student mode

        var allowedToHost = false

        // todo: Move this for-loop to onCreate block
        // Loop through Real Time Database -- look for matching user emails (whitelisted) for professors.
        for (snapshot in hostListSnapshot.children) {

            var hostEmail = snapshot.key
            hostEmail = hostEmail?.replace("+","@")
            hostEmail = hostEmail?.replace("_",".")

            if (hostEmail == localUserEmail){
                allowedToHost = true
                break
            }
        }

        if (allowedToHost) {

            // generate session ID
            this.newSessionID = databaseRef.child(Constants.SESSION_LIST).push().key.toString()

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("MM.dd.yy 'at' HH:mm aa", Locale.getDefault())
            val date = dateFormat.format(calendar.time)

            val sessionModel = hashMapOf<String, Any>()
            sessionModel[Constants.HOST_EMAIL] = localUserEmail.toString()
            sessionModel[Constants.HOST_NAME] = localUsername.toString()
            sessionModel[Constants.ACTIVE_SESSION] = true
            sessionModel[Constants.SESSION_DATE] = date
            sessionModel[Constants.SESSION_TITLE] = "Session by $localUsername"

            databaseRef.child(Constants.SESSION_LIST).child(newSessionID).setValue(sessionModel)

            sessionModel.clear()
            sessionModel[Constants.CAPTION_AUTHOR] = localUsername.toString()
            sessionModel[Constants.CAPTION_TEXT] = "[ Session started by $localUsername]"
            sessionModel[Constants.CAPTION_FEEDBACK] = "n/a"
            val captionID = databaseRef.child(Constants.SESSION_LIST).child(newSessionID).child(Constants.CAPTION_LIST).push().key.toString()
            databaseRef.child(Constants.SESSION_LIST).child(newSessionID).child(Constants.CAPTION_LIST).child(captionID).setValue(sessionModel)

            // based on Navigation Graph
            view.findNavController().navigate(HostFragmentDirections
                .actionHostFragmentToSessionHostActivity(localUserEmail, localUsername, this.newSessionID))

        } else {

            val alertDialogBuilder = AlertDialog.Builder(fragmentContext)
            alertDialogBuilder
                .setTitle("Oops!")
                .setMessage("Hosting is not available with your account.")
                .setCancelable(true)
                .setNeutralButton("Become a host") { dialog, which ->
                    val url : String = "https://vopio.tech/contact/"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(url))
                    startActivity(intent)

                }

                .setPositiveButton("Dismiss") { dialog, which ->

                    dialog.cancel()
                }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HostFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME, param1)
                    putString(ARG_USER_EMAIL, param2)
                }
            }
    }
}