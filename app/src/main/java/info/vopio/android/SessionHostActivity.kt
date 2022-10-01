package info.vopio.android

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.*
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.Services.SpeechService
import info.vopio.android.Services.VoiceRecorder
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.MessageUploader
import info.vopio.android.Utilities.PhoneticAlphabetPopup
import info.vopio.android.databinding.ActivitySessionHostBinding
import timber.log.Timber

// This class represents: Speaker Session Activity OR Lecture Host Activity
class SessionHostActivity : AppCompatActivity() {

    lateinit var sessionId : String

    lateinit var thisFirebaseUser : String
    lateinit var thisFirebaseEmail : String
    lateinit var thisFirebaseDatabaseReference : DatabaseReference

    // RecyclerView for Captions
    lateinit var thisFirebaseCaptionsAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    // RecyclerView for Questions
    lateinit var thisFirebaseQuestionsAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisQuestionsLinearLayoutManager : LinearLayoutManager

    private var thisSpeechService: SpeechService? = null
    private var thisVoiceRecorder: VoiceRecorder? = null
    private var speechIsBound = false

    private lateinit var binding: ActivitySessionHostBinding

    companion object{
        const val REQUEST_RECORD_AUDIO_PERMISSION = 1
    }

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var captionTextView: TextView = itemView.findViewById(R.id.sessionIDTextView)
        var authorTextView: TextView = itemView.findViewById(R.id.dateTextView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.host_session_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_end_session -> {
                Timber.i("-->>SpeechX: LEAVE SESH")
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                MessageUploader().sendCaptions(thisFirebaseDatabaseReference, sessionId, "[ Session ended by host ]", thisFirebaseUser)
                stopSession()
            }
            R.id.nav_test_session -> {

                MessageUploader().sendCaptions(thisFirebaseDatabaseReference, sessionId, "This is a test - automated captions", thisFirebaseUser)
                val questionIs = "This is a test - can you see this question?"
                MessageUploader().sendQuestion(thisFirebaseDatabaseReference, sessionId, questionIs, thisFirebaseUser)
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionHostBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        Timber.i("-->>SpeechX: onCreate")

        thisFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        val extras = intent.extras
        if (extras != null) {

            val localUser = extras.getString(Constants.SESSION_USERNAME)
            localUser?.let {
                val nameArray = localUser.split(" ").toTypedArray()
                thisFirebaseUser = if (nameArray.size > 1) nameArray[0] + " " + nameArray[1] else nameArray[0]
            }

            val localEmail = extras.getString(Constants.SESSION_USER_EMAIL)
            localEmail?.let {
                thisFirebaseEmail = it
            }

            val newSessionId = extras.getString(Constants.SESSION_KEY)
            newSessionId?.let {
                sessionId = it
            }

        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val lastFourDigits = sessionId.substring(sessionId.length.minus(4))
        val sessionHeader = "session:   $lastFourDigits"
        binding.sessionCodeButton.setText(sessionHeader)
        binding.sessionCodeButton.setOnClickListener {
            PhoneticAlphabetPopup().showPopup(this, lastFourDigits)
        }

    }

    override fun onStart() {
        super.onStart()

        Timber.i("-->>SpeechX: incoming SessionId ${this.sessionId}")
        startSession()
        configureCaptionsSnapshotParser()
        configureQuestionsSnapshotParser()
    }

    override fun onStop() {

        Timber.i("-->>SpeechX: onStop -- Stop Cloud Speech API")

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //Stop the microphone
        stopVoiceRecorder()

        // Stop Cloud Speech API
        if (thisSpeechService != null){
            thisSpeechService?.removeListener(thisSpeechServiceListener)
            if (speechIsBound) {
                unbindService(thisServiceConnection)
            }
            thisSpeechService = null
        }

        super.onStop()
    }

    private val thisVoiceCallback: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {
            Timber.i("-->>SpeechX: HEARING VOICE")

            if (thisSpeechService != null) {
                thisVoiceRecorder?.sampleRate?.let { thisSpeechService?.startRecognizing(it) }
            }
        }

        override fun onVoice(data: ByteArray?, size: Int) {
            if (thisSpeechService != null) {
                thisSpeechService?.recognize(data, size)
            }
        }

        override fun onVoiceEnd() {
            Timber.i("-->>SpeechX: NOT HEARING VOICE")

            if (thisSpeechService != null) {
                thisSpeechService?.finishRecognizing()
            }
        }
    }

    private val thisSpeechServiceListener: SpeechService.Listener =
        SpeechService.Listener { text, isFinal ->

            if (!TextUtils.isEmpty(text)) {

                if (isFinal){
                    runOnUiThread {
                        binding.statusView.text = getString(R.string.session_status)
                    }

                    MessageUploader().sendCaptions(thisFirebaseDatabaseReference, sessionId, text, thisFirebaseUser)
                    thisVoiceRecorder?.dismiss()

                } else {

                    runOnUiThread {
                        binding.statusView.text = text
                    }
                }

            }
        }

    private val thisServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {

            thisSpeechService = SpeechService.from(binder)
            thisSpeechService?.addListener(thisSpeechServiceListener)

            Timber.i("-->>SpeechX: LISTENING")

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            thisSpeechService = null
        }
    }

    private fun startSession() {

        if (thisFirebaseUser.isNotEmpty()) {

            // Prepare Cloud Speech API - this starts the Speech API if user is signed in with Google
            speechIsBound = bindService(Intent(this, SpeechService::class.java), thisServiceConnection, BIND_AUTO_CREATE)
            Timber.i("-->>SpeechX: SpeechService bindService")

            // Start listening to microphone
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Timber.wtf("-->>startSpeechService start Session shouldShowRequestPermissionRationale")
            } else {
                Timber.wtf("-->>requestMicPermission startSpeechService else")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            }

        }
    }

    private fun stopSession() {
        Timber.i("-->>SpeechX: stopSession")

        thisFirebaseDatabaseReference.child(Constants.SESSION_LIST).child(this.sessionId).child(Constants.ACTIVE_SESSION).setValue(false)
        thisFirebaseCaptionsAdapter.stopListening()
        thisFirebaseQuestionsAdapter.stopListening()
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (permission in grantResults){

            Timber.wtf("-->>onRequestPermissionsResult loop $permission")


            if (permission == PackageManager.PERMISSION_GRANTED){
                Timber.wtf("-->>onRequestPermissionsResult start Session")

                startSession()
            } else {
                Timber.wtf("-->>onRequestPermissionsResult permission DENIED")
                Toast.makeText(this, R.string.permission_message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVoiceRecorder() {

        // reset it
        if (thisVoiceRecorder != null) {
            thisVoiceRecorder?.stop()
        }

        thisVoiceRecorder = VoiceRecorder(thisVoiceCallback)
        thisVoiceRecorder?.start()
    }

    private fun stopVoiceRecorder() {
        if (thisVoiceRecorder != null) {
            thisVoiceRecorder?.stop()
            thisVoiceRecorder = null
        }
    }

    private fun configureQuestionsSnapshotParser(){

        // setup RecyclerView for Questions
        thisQuestionsLinearLayoutManager = LinearLayoutManager(this)
        thisQuestionsLinearLayoutManager.stackFromEnd = true
        binding.questionRecyclerView.layoutManager = thisQuestionsLinearLayoutManager

        val parser: SnapshotParser<MessageModel> =
            SnapshotParser<MessageModel> { dataSnapshot ->
                val questionItem: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

//                if (questionItem != null) {
//                    questionItem.id = dataSnapshot.key
//                }
                questionItem!!
            }

        val questionsRef: DatabaseReference = thisFirebaseDatabaseReference.child(Constants.SESSION_LIST).child(sessionId).child(Constants.QUESTION_LIST)
        val options: FirebaseRecyclerOptions<MessageModel> =
            FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(questionsRef, parser)
                .build()

        thisFirebaseQuestionsAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {

                override fun onError(error: DatabaseError) {
                    super.onError(error)
                    Toast.makeText(this@SessionHostActivity, "Database access denied", Toast.LENGTH_SHORT).show()
                }

                override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageViewHolder {
                    val inflater = LayoutInflater.from(viewGroup.context)
                    return MessageViewHolder(
                        inflater.inflate(
                            R.layout.caption_item_message,
                            viewGroup,
                            false
                        )
                    )
                }

                override fun onBindViewHolder(
                    viewHolder: MessageViewHolder,
                    position: Int,
                    questionItem: MessageModel
                ) {
                    if (questionItem.text != null) {

                        val caption: String = questionItem.text
                        val spanString = SpannableString(caption)

                        viewHolder.authorTextView.text = questionItem.name
                        viewHolder.captionTextView.text = spanString
                        viewHolder.captionTextView.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        thisFirebaseQuestionsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, 1)
                val captionItemCount = 1 //mFirebaseAdapter.getItemCount();
                val lastVisiblePosition: Int =
                    thisQuestionsLinearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                    positionStart >= captionItemCount - 1 && lastVisiblePosition == positionStart - 1) {

                    binding.questionRecyclerView.scrollToPosition(positionStart)
                }

            }
        })
        binding.questionRecyclerView.adapter = thisFirebaseQuestionsAdapter
        thisFirebaseQuestionsAdapter.startListening()
    }

    private fun configureCaptionsSnapshotParser() {

        // setup RecyclerView with last item showing first
        thisLinearLayoutManager = LinearLayoutManager(this)
        thisLinearLayoutManager.stackFromEnd = true
        binding.hostCaptionRecyclerView.layoutManager = thisLinearLayoutManager

        val parser: SnapshotParser<MessageModel> =
            SnapshotParser<MessageModel> { dataSnapshot ->
                val captionItem: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

                Timber.wtf("-->> snapItem " + captionItem.toString())

                if (captionItem != null) {
                    captionItem.id = dataSnapshot.key
                }
                captionItem!!
            }

        val messagesRef: DatabaseReference = thisFirebaseDatabaseReference.child(Constants.SESSION_LIST).child(sessionId).child(Constants.CAPTION_LIST)
        val options: FirebaseRecyclerOptions<MessageModel> =
            FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(messagesRef, parser)
                .build()

        thisFirebaseCaptionsAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {

                override fun onError(error: DatabaseError) {
                    super.onError(error)
                    Toast.makeText(this@SessionHostActivity, "Database access denied", Toast.LENGTH_SHORT).show()
                }

                override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageViewHolder {
                    val inflater = LayoutInflater.from(viewGroup.context)
                    return MessageViewHolder(
                        inflater.inflate(
                            R.layout.caption_item_message,
                            viewGroup,
                            false
                        )
                    )
                }

                override fun onBindViewHolder(
                    viewHolder: MessageViewHolder,
                    position: Int,
                    captionMessage: MessageModel
                ) {
                    if (captionMessage.text != null) {

                        val caption: String = captionMessage.text
                        val spanString = SpannableString(caption)

                        viewHolder.authorTextView.text = captionMessage.name
                        viewHolder.captionTextView.text = spanString
                        viewHolder.captionTextView.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        thisFirebaseCaptionsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, 1)
                val captionItemCount = 1 //mFirebaseAdapter.getItemCount();
                val lastVisiblePosition: Int =
                    thisLinearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                    positionStart >= captionItemCount - 1 && lastVisiblePosition == positionStart - 1) {

                    binding.hostCaptionRecyclerView.scrollToPosition(positionStart)
                }

            }
        })
        binding.hostCaptionRecyclerView.adapter = thisFirebaseCaptionsAdapter
        thisFirebaseCaptionsAdapter.startListening()
    }

}