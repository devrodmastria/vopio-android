package info.vopio.android

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.webkit.WebSettings
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.*
import com.google.zxing.Result
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.Services.SpeechService
import info.vopio.android.Services.VoiceRecorder
import info.vopio.android.Utilities.MessageUploader
import info.vopio.android.Utilities.QRgenerator
import info.vopio.android.databinding.ActivityCaptionBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber

class CaptionActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    lateinit var webSettings : WebSettings
    lateinit var thisFirebaseDatabaseReference : DatabaseReference

    lateinit var sessionId : String
    lateinit var captionId : String
    lateinit var captionAuthor : String // author could be any un-muted app user in the session

    lateinit var incomingSessionId : String
    lateinit var thisFirebaseUser : String
    lateinit var thisFirebaseEmail : String
    lateinit var thisFirebaseAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    private var thisSpeechService: SpeechService? = null
    private var thisVoiceRecorder: VoiceRecorder? = null

    private lateinit var binding: ActivityCaptionBinding

    var lastSelectedWord: String = "placeholder"

    companion object{
        const val REQUEST_RECORD_AUDIO_PERMISSION = 1
    }

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var captionTextView: TextView = itemView.findViewById(R.id.captionTextView)
        var authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
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
            if (isFinal) {
                thisVoiceRecorder?.dismiss()
            }
            if (!TextUtils.isEmpty(text)) {
                runOnUiThread {

                    if (isFinal) {
                        Timber.i("-->>SpeechX: CAPTION: $text")
                        MessageUploader().sendCaptions(thisFirebaseDatabaseReference, sessionId, text, thisFirebaseUser)
                    } else {
                        Timber.i("-->>SpeechX: PARTIAL CAPTION: $text")
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.caption_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_leave -> {
                Timber.i("-->>SpeechX: LEAVE SESH")
                stopSession()

            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        Timber.i("-->>SpeechX: onCreate")

        binding.feedbackButton.visibility = View.INVISIBLE

        thisFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        val extras = intent.extras
        if (extras != null) {

            val localUser = extras.getString(MainActivity.SESSION_USERNAME)
            localUser?.let {

                val nameArray = localUser.split(" ").toTypedArray()

                thisFirebaseUser = if (nameArray.size > 1) nameArray[0] + " " + nameArray[1] else nameArray[0]

            }

            val localEmail = extras.getString(MainActivity.SESSION_USER_EMAIL)
            localEmail?.let {
                thisFirebaseEmail = it
            }

            incomingSessionId = extras.getString(MainActivity.SESSION_KEY).toString()

        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true

        binding.micButton.setOnClickListener {

            Timber.i("-->>SpeechX: micButton")

            if (thisVoiceRecorder != null) { // speech service is ON - turn it OFF

                Timber.i("-->>SpeechX: micButton disable_speech")
                binding.micButton.text = getString(R.string.disable_speech)
                stopSession()

            } else { // speech service is OFF - turn it ON

                Timber.i("-->>SpeechX: micButton enable_speech")
                binding.micButton.text = getString(R.string.enable_speech)
                startSession()
            }

        }

        binding.feedbackButton.setOnClickListener {

            // update feedback to database
            val childUpdates = HashMap<String, Any>()
            childUpdates["/feedback/"] = "Review Pronunciation of word: $lastSelectedWord"
            thisFirebaseDatabaseReference.child(sessionId).child(captionId).updateChildren(childUpdates)
            binding.feedbackButton.visibility = View.INVISIBLE

        }

        // setup RecyclerView with last item showing first
        thisLinearLayoutManager = LinearLayoutManager(this)
        thisLinearLayoutManager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = thisLinearLayoutManager

    }


    override fun handleResult(p0: Result?) { //results from QR scanner
        val QRresult: String = p0?.text.toString()

        if (QRresult.length == 20) {
            setContentView(R.layout.activity_caption)
            configureDatabaseSnapshotParser(QRresult)
        }
    }

    override fun onStart() {
        super.onStart()

        incomingSessionId.let {

            if (incomingSessionId.contentEquals(MainActivity.HOST_TAG)){

                binding.webView.visibility = View.INVISIBLE

                this.sessionId = thisFirebaseDatabaseReference.push().key.toString()
                val lastFourDigits = sessionId.substring(sessionId.length.minus(4))

                setTitle("session $lastFourDigits")

                Timber.i("-->>SpeechX: incomingSessionId ${this.sessionId}")

                MessageUploader().sendCaptions(
                    thisFirebaseDatabaseReference,
                    this.sessionId, "[ Session Started ]",
                    thisFirebaseUser)

                Timber.i("-->>SpeechX: AUTO enable_speech")
                binding.micButton.text = getString(R.string.disable_speech)
                startSession()

            } else {

                binding.webView.visibility = View.VISIBLE
                this.sessionId = incomingSessionId

            }
            configureDatabaseSnapshotParser(this.sessionId)
        }

    }

    private fun startSession() {

        if (thisFirebaseUser.isNotEmpty()) {

            // Prepare Cloud Speech API - this starts the Speech API if user is signed in with Google
            bindService(Intent(this, SpeechService::class.java), thisServiceConnection, BIND_AUTO_CREATE)
            Timber.i("-->>SpeechX: SpeechService bindService")

            MessageUploader().sendCaptions(thisFirebaseDatabaseReference, sessionId, "[ $thisFirebaseUser has joined ]", thisFirebaseUser)

            // Start listening to microphone
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
//                showPermissionDialog()
                Timber.wtf("-->>startSpeechService startSession showPermissionMessageDialog")
            } else {
                Timber.wtf("-->>requestMicPermission startSpeechService else")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            }

        }
    }

    private fun stopSession() {
        Timber.i("-->>SpeechX: stopSession ${this.sessionId}")

        // Stop Cloud Speech API
        if (thisSpeechService != null){
            thisSpeechService!!.removeListener(thisSpeechServiceListener)
            thisSpeechService!!.stopSelf()
            unbindService(thisServiceConnection)
        }
        thisFirebaseAdapter.stopListening()
        stopVoiceRecorder()
        thisFirebaseDatabaseReference.child(this.sessionId).removeValue()
        super.onBackPressed()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (permission in grantResults){

            Timber.wtf("-->>onRequestPermissionsResult loop $permission")


            if (permission == PackageManager.PERMISSION_GRANTED){
                Timber.wtf("-->>onRequestPermissionsResult startSession")

                startSession()
            } else {
                Timber.wtf("-->>onRequestPermissionsResult permission DENIED")
                Toast.makeText(this, R.string.permission_message, Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun startVoiceRecorder() {
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

    private fun configureDatabaseSnapshotParser(MESSAGES_CHILD: String) {

        val parser: SnapshotParser<MessageModel> =
            SnapshotParser<MessageModel> { dataSnapshot ->
                val friendlyMessage: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

                if (friendlyMessage != null) {
                    friendlyMessage.id = dataSnapshot.key
                }
                friendlyMessage!!
            }

        val messagesRef: DatabaseReference = thisFirebaseDatabaseReference.child(MESSAGES_CHILD)
        val options: FirebaseRecyclerOptions<MessageModel> =
            FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(messagesRef, parser)
                .build()

        thisFirebaseAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {

                override fun onError(error: DatabaseError) {
                    super.onError(error)

                    Toast.makeText(this@CaptionActivity, "Database access denied", Toast.LENGTH_SHORT).show()
                    
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
                    friendlyMessage: MessageModel
                ) {
                    if (friendlyMessage.text != null) {

                        captionId = friendlyMessage.id
                        captionAuthor = friendlyMessage.name

                        val caption: String = friendlyMessage.text
                        val spanString = SpannableString(caption)

                        if (!caption.contains("[")) { // do not hyperlink system notes

                            val wordArray =
                                caption.split(" ").toTypedArray()
                            for (wordIs in wordArray) {
                                if (wordIs.length > 5) {
                                    val beginIndex = caption.indexOf(wordIs)
                                    val endIndex = beginIndex + wordIs.length
                                    val clickableSpan: ClickableSpan = object : ClickableSpan() {
                                        override fun onClick(@NonNull view: View) {

                                            lastSelectedWord = wordIs
                                            binding.feedbackButton.visibility = View.VISIBLE

                                            val url = "https://duckduckgo.com/?q=define+$wordIs&t=ffab&ia=definition"
                                            binding.webView.loadUrl(url)
                                            binding.webView.visibility = View.VISIBLE

                                            MessageUploader().saveWord(thisFirebaseDatabaseReference, wordIs, thisFirebaseEmail)

                                        }
                                    }
                                    spanString.setSpan(
                                        clickableSpan,
                                        beginIndex,
                                        endIndex,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                            }
                        }

                        viewHolder.authorTextView.text = captionAuthor
                        viewHolder.captionTextView.text = spanString
                        viewHolder.captionTextView.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        thisFirebaseAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, 1)
                val friendlyMessageCount = 1 //mFirebaseAdapter.getItemCount();
                val lastVisiblePosition: Int =
                    thisLinearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                    positionStart >= friendlyMessageCount - 1 &&
                    lastVisiblePosition == positionStart - 1
                ) {
                    binding.messageRecyclerView.scrollToPosition(positionStart)
                }
            }
        })
        binding.messageRecyclerView.adapter = thisFirebaseAdapter
        thisFirebaseAdapter.startListening()
    }

}
