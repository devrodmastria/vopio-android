package info.vopio.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import info.vopio.android.DataModel.SessionListAdapter
import info.vopio.android.Utilities.Constants
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_USERNAME = "param1"
private const val ARG_USER_EMAIL = "param2"

class HostFragment : Fragment() {

    private var localUsername: String? = null
    private var localUserEmail: String? = null


    lateinit var fragmentContext: Context
    lateinit var fragmentContainer: View
    lateinit var databaseRef : DatabaseReference
    lateinit var newSessionID : String
    lateinit var hostListSnapshot : DataSnapshot
    lateinit var globalSessionListSnapshot : DataSnapshot

    private var inactiveSessionListSnapshot = mutableListOf<DataSnapshot>()
    lateinit var thisLinearLayoutManager : LinearLayoutManager
    lateinit var sampleSnapshot : DataSnapshot

    private fun hostNewSession(){

        var allowedToHost = false

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


            val intent = Intent(fragmentContext, HostSessionActivity::class.java)
            intent.putExtra(Constants.SESSION_USERNAME, localUsername)
            intent.putExtra(Constants.SESSION_USER_EMAIL, localUserEmail)
            intent.putExtra(Constants.SESSION_KEY, this.newSessionID)
            startActivity(intent)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            localUsername = it.getString(ARG_USERNAME)
            localUserEmail = it.getString(ARG_USER_EMAIL)
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
                Timber.i("-->>SpeechX: onDataChange Failed to read value:${error.toException()}")
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
        thisLinearLayoutManager.stackFromEnd = false
        val recyclerView: RecyclerView = fragmentContainer.findViewById(R.id.recyclerViewHost)
        recyclerView.layoutManager = thisLinearLayoutManager

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
            hostNewSession()
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

    private fun adapterOnClick(sessionId: String){

        val intent = Intent(fragmentContext, ReviewSessionActivity::class.java)
        intent.putExtra(Constants.SESSION_KEY, sessionId)
        intent.putExtra(Constants.SESSION_USERNAME, localUsername)
        intent.putExtra(Constants.SESSION_USER_EMAIL, localUserEmail)
        startActivity(intent)

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