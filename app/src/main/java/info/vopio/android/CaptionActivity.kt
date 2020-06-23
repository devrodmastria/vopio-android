package info.vopio.android

import android.Manifest
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.database.*
import com.google.zxing.Result
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.Services.SpeechService
import info.vopio.android.Services.VoiceRecorder
import info.vopio.android.Utilities.MessageUploader
import info.vopio.android.Utilities.QRgenerator
import kotlinx.android.synthetic.main.activity_caption.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber

class CaptionActivity : AppCompatActivity(),
    GoogleApiClient.OnConnectionFailedListener, ZXingScannerView.ResultHandler {

    lateinit var xingScannerView : ZXingScannerView
    lateinit var webSettings : WebSettings
    lateinit var thisFirebaseDatabaseReference : DatabaseReference

    lateinit var sessionId : String
    lateinit var captionId : String
    lateinit var captionAuthor : String // author could be any un-muted app user in the session

    lateinit var incomingSessionId : String
    lateinit var thisFirebaseUser : String
    lateinit var thisFirebaseAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisGoogleApiClient : GoogleApiClient
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    private var thisSpeechService: SpeechService? = null
    private var thisVoiceRecorder: VoiceRecorder? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caption)

        Timber.i("-->>SpeechX: onCreate")

        feedbackButton.visibility = View.INVISIBLE

        thisFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        val extras = intent.extras
        if (extras != null) {

            val localUser = extras.getString(MainActivity.SESSION_USER)
            localUser?.let {

                val nameArray =
                    localUser.split(" ").toTypedArray()

                thisFirebaseUser = nameArray[0] + " " + nameArray[1].first()
            }

            incomingSessionId = extras.getString(MainActivity.SESSION_KEY).toString()

        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        micButton.setOnClickListener {

            Timber.i("-->>SpeechX: micButton")

            if (thisVoiceRecorder != null) { // speech service is ON - turn it OFF

                Timber.i("-->>SpeechX: micButton disable_speech")
                micButton.text = getString(R.string.disable_speech)
                stopSession()

            } else { // speech service is OFF - turn it ON

                Timber.i("-->>SpeechX: micButton enable_speech")
                micButton.text = getString(R.string.enable_speech)
                startSession()
            }

        }

        feedbackButton.setOnClickListener {

            // update feedback to database
            val childUpdates = HashMap<String, Any>()
            childUpdates["/feedback/"] = "Review Pronunciation of word: $lastSelectedWord"
            thisFirebaseDatabaseReference.child(sessionId).child(captionId).updateChildren(childUpdates)
            feedbackButton.visibility = View.INVISIBLE

        }

        thisGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
            .addApi(Auth.GOOGLE_SIGN_IN_API)
            .build()

        // setup RecyclerView with last item showing first
        thisLinearLayoutManager = LinearLayoutManager(this)
        thisLinearLayoutManager.stackFromEnd = true
        messageRecyclerView.layoutManager = thisLinearLayoutManager

    }


    override fun handleResult(p0: Result?) { //results from QR scanner
        val QRresult: String = p0?.text.toString()

        if (QRresult.length == 20) {
            xingScannerView.stopCamera()
            setContentView(R.layout.activity_caption)
            configureDatabaseSnapshotParser(QRresult)
        }
    }

    override fun onStart() {
        super.onStart()

        incomingSessionId.let {

            if (incomingSessionId.contentEquals(MainActivity.THIS_IS_THE_HOST)){

                webView.visibility = View.INVISIBLE
                QRimageView.visibility = View.VISIBLE

                this.sessionId = thisFirebaseDatabaseReference.push().key.toString()
                val lastFourDigits = sessionId.substring(sessionId.length.minus(4))

                setTitle("session code: $lastFourDigits")
                QRimageView.visibility = View.INVISIBLE

                Timber.i("-->>SpeechX: incomingSessionId ${this.sessionId}")

                val sessionBitmapQR : Bitmap = QRgenerator().encodeToQR(this.sessionId, this)
                QRimageView.setImageBitmap(sessionBitmapQR)

                MessageUploader().sendCaptions(
                    thisFirebaseDatabaseReference,
                    this.sessionId, "[ Session Started ]",
                    thisFirebaseUser)

                Timber.i("-->>SpeechX: AUTO enable_speech")
                micButton.text = getString(R.string.disable_speech)
                startSession()

            } else {

                webView.visibility = View.VISIBLE
                QRimageView.visibility = View.INVISIBLE
                this.sessionId = incomingSessionId

            }
            configureDatabaseSnapshotParser(this.sessionId)
        }

    }

    override fun onPause() {
        super.onPause()
        if (incomingSessionId.contentEquals(MainActivity.THIS_IS_THE_HOST)) {
            MessageUploader().sendCaptions(thisFirebaseDatabaseReference, sessionId, "[ Session Ended ]", thisFirebaseUser)
        }

        if (thisVoiceRecorder != null) { // speech service is ON - turn it OFF

            Timber.i("-->>SpeechX: onPause disable_speech")
            micButton.text = getString(R.string.disable_speech)
            stopSession()
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
        Timber.i("-->>SpeechX: stopSession")

        thisFirebaseDatabaseReference.child(this.sessionId).removeValue()

        stopVoiceRecorder()

        // Stop Cloud Speech API
        if (thisSpeechService != null){
            thisSpeechService!!.removeListener(thisSpeechServiceListener)
            thisSpeechService!!.stopSelf()
            unbindService(thisServiceConnection)
        }
        thisSpeechService = null

        thisFirebaseAdapter.stopListening()

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this, "Google Play Services error: " + p0.errorMessage, Toast.LENGTH_SHORT).show()

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
                                            feedbackButton.visibility = View.VISIBLE

                                            if (QRimageView.visibility == View.INVISIBLE) {
                                                val url =
                                                    "https://duckduckgo.com/?q=define+$wordIs&t=ffab&ia=definition"
                                                webView.loadUrl(url)
                                                webView.visibility = View.VISIBLE

                                            }
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
                    messageRecyclerView.scrollToPosition(positionStart)
                }
            }
        })
        messageRecyclerView.adapter = thisFirebaseAdapter
        thisFirebaseAdapter.startListening()
    }

}
