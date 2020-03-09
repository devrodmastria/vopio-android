package info.vopio.captions

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.Result
import info.vopio.captions.DataModel.MessageModel
import info.vopio.captions.DataModel.MessageUploader
import info.vopio.captions.Services.SpeechService
import info.vopio.captions.Services.VoiceRecorder
import kotlinx.android.synthetic.main.activity_caption.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber

class CaptionActivity : AppCompatActivity(),
    GoogleApiClient.OnConnectionFailedListener, ZXingScannerView.ResultHandler {

    lateinit var xingScannerView : ZXingScannerView
    lateinit var webSettings : WebSettings
    lateinit var thisFirebaseDBref : DatabaseReference
    lateinit var sessionId : String
    lateinit var localUser : String

    lateinit var thisFirebaseUser : FirebaseUser
    lateinit var thisFirebaseAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisGoogleApiClient : GoogleApiClient
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    lateinit var thisSpeechService: SpeechService
    private var thisVoiceRecorder: VoiceRecorder? = null

    companion object{
        const val REQUEST_RECORD_AUDIO_PERMISSION = 1
    }

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
    }

    private val thisVoiceCallback: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {
            Timber.i("-->>SpeechX: HEARING VOICE")

            if (thisSpeechService != null) {
                thisSpeechService.startRecognizing(thisVoiceRecorder!!.sampleRate)
            }

        }

        override fun onVoice(data: ByteArray?, size: Int) {
            if (thisSpeechService != null) {
                thisSpeechService.recognize(data, size)
            }

        }

        override fun onVoiceEnd() {
            Timber.i("-->>SpeechX: NOT HEARING VOICE")

            if (thisSpeechService != null) {
                thisSpeechService.finishRecognizing()
            }
        }
    }

    private val thisServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {

            thisSpeechService = SpeechService.from(binder)
            thisSpeechService.addListener(thisSpeechServiceListener)

            Timber.i("-->>SpeechX: LISTENING")

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            thisSpeechService.stopSelf()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caption)

        Timber.i("-->>SpeechX: onCreate")


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView.visibility = View.INVISIBLE
        webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        micButton.setOnClickListener {

            if (thisVoiceRecorder != null) { // it's listening
                micButton.setText(R.string.enable_speech)
                stopSession()
            } else { // it's off
                micButton.setText(R.string.disable_speech)
                requestMicPermission()
            }

            //request access to mic
            //start/stop voice recorder service
            //start/stop speech service listener
            //send captions to Firebase
            //MessageUploader().sendCaptions(thisFirebaseDBref, sessionId, "test 20200308", this.localUserEmail)

        }

        scanAgainButton.setOnClickListener {

            xingScannerView = ZXingScannerView(CaptionActivity@this)
            setContentView(xingScannerView)
            xingScannerView.setResultHandler(CaptionActivity@this)
            xingScannerView.startCamera()
        }

        val thisFirebaseAuth = FirebaseAuth.getInstance()
        thisFirebaseUser = thisFirebaseAuth.currentUser!!
        if (thisFirebaseUser == null){
            //launch sign in activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        thisGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
            .addApi(Auth.GOOGLE_SIGN_IN_API)
            .build()

        // setup RecyclerView with last item showing first
        thisLinearLayoutManager = LinearLayoutManager(this)
        thisLinearLayoutManager.stackFromEnd = true
        messageRecyclerView.layoutManager = thisLinearLayoutManager


        val extras = intent.extras
        if (extras != null) {

            val sessionId = extras.getString(MainActivity.SESSION_KEY)
            sessionId?.let {

                this.sessionId = sessionId
                configureDatabase(sessionId)
            }

            val localUser = extras.getString(MainActivity.SESSION_USER)
            localUser?.let {
                this.localUser = localUser
            }

        }

    }

    override fun onPause() {
        thisFirebaseAdapter.stopListening()
        stopSession()
        super.onPause()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this, "Google Play Services error: " + p0.errorMessage, Toast.LENGTH_SHORT).show()

    }

    override fun handleResult(p0: Result?) { //results from QR scanner
        val QRresult: String = p0?.text.toString()

        if (QRresult.length == 20) {
            xingScannerView.stopCamera()
            setContentView(R.layout.activity_caption)
            configureDatabase(QRresult)
        }
    }

    private fun requestMicPermission(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

            Timber.wtf("-->>requestMicPermission GRANTED")

            startSession()

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    private fun showPermissionMessageDialog(){
        Toast.makeText(this, R.string.permission_message, Toast.LENGTH_SHORT).show()

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
                        MessageUploader().sendCaptions(thisFirebaseDBref, sessionId, "$localUser $text", this.localUser)

                    } else {
                        Timber.i("-->>SpeechX: PARTIAL CAPTION: $text")
                    }
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

    private fun startSession() {
        if (thisFirebaseUser != null) {

            // Prepare Cloud Speech API - this starts the Speech API if user is signed in with Google
            bindService(Intent(this, SpeechService::class.java), thisServiceConnection, BIND_AUTO_CREATE)

            MessageUploader().sendCaptions(thisFirebaseDBref, sessionId, "$localUser has joined", this.localUser)
        }

        startVoiceRecorder()

    }

    private fun stopSession() {
        Timber.i("-->>SpeechX: stopSession")

        stopVoiceRecorder()

        // Stop Cloud Speech API
        if (thisSpeechServiceListener != null) {
            if (thisSpeechService != null){
                thisSpeechService?.removeListener(thisSpeechServiceListener)
                thisSpeechService?.stopSelf()
            }
            unbindService(thisServiceConnection)
        }

    }

    private fun configureDatabase(MESSAGES_CHILD: String) {
        val parser: SnapshotParser<MessageModel> =

            SnapshotParser<MessageModel> { dataSnapshot ->

                val friendlyMessage: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

                if (friendlyMessage != null) {
                    friendlyMessage.id = dataSnapshot.key
                }
                friendlyMessage!!
            }

        thisFirebaseDBref = FirebaseDatabase.getInstance().reference.child(MESSAGES_CHILD)

        val messagesRef: DatabaseReference = thisFirebaseDBref
        val options: FirebaseRecyclerOptions<MessageModel> =
            FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(messagesRef, parser)
                .build()
        thisFirebaseAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {

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
                        val caption: String = friendlyMessage.text
                        val spanString = SpannableString(caption)
                        val wordArray =
                            caption.split(" ").toTypedArray()
                        for (wordIs in wordArray) {
                            if (wordIs.length > 8) {
                                val beginIndex = caption.indexOf(wordIs)
                                val endIndex = beginIndex + wordIs.length
                                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                                    override fun onClick(@NonNull view: View) {
                                        val url =
                                            "https://duckduckgo.com/?q=define+$wordIs&t=ffab&ia=definition"
                                        webView.loadUrl(url)
                                        webView.visibility = View.VISIBLE
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

                        viewHolder.messageTextView.text = spanString
                        viewHolder.messageTextView.movementMethod = LinkMovementMethod.getInstance()
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
