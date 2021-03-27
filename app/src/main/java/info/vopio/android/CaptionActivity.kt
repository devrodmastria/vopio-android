package info.vopio.android

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.zxing.Result
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.databinding.ActivityCaptionBinding
import me.dm7.barcodescanner.zxing.ZXingScannerView
import timber.log.Timber

class CaptionActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    lateinit var xingScannerView : ZXingScannerView
    lateinit var webSettings : WebSettings
    lateinit var thisFirebaseDBref : DatabaseReference

    lateinit var thisFirebaseAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    private lateinit var binding: ActivityCaptionBinding

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.webView.visibility = View.INVISIBLE
        webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true

        binding.scanAgainButton.setOnClickListener {

            xingScannerView = ZXingScannerView(CaptionActivity@this)
            setContentView(xingScannerView)
            xingScannerView.setResultHandler(CaptionActivity@this)
            xingScannerView.startCamera()
        }

        val thisFirebaseAuth = FirebaseAuth.getInstance()
        val thisFirebaseUser = thisFirebaseAuth.currentUser
        if (thisFirebaseUser == null){
            //launch sign in activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // setup RecyclerView with last item showing first
        thisLinearLayoutManager = LinearLayoutManager(this)
        thisLinearLayoutManager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = thisLinearLayoutManager


        val extras = intent.extras
        if (extras != null) {
            val sessionId = extras.getString(MainActivity.SESSION_KEY)
            sessionId?.let {

                configureDatabase(sessionId)
            }
        }

    }

    override fun onPause() {
        thisFirebaseAdapter.stopListening()
        super.onPause()
    }

    override fun handleResult(p0: Result?) { //results from QR scanner
        val QRresult: String = p0?.text.toString()

        if (QRresult.length == 20) {
            xingScannerView.stopCamera()
            setContentView(R.layout.activity_caption)
            configureDatabase(QRresult)
        }
    }

    private fun configureDatabase(MESSAGES_CHILD: String) {
        val parser: SnapshotParser<MessageModel> =

            SnapshotParser<MessageModel> { dataSnapshot ->

                val friendlyMessage: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.key)
                    Timber.wtf("-->>SnapshotParser friendlyMessage " + friendlyMessage.text)
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
                    if (friendlyMessage.getText() != null) {
                        val caption: String = friendlyMessage.getText()
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
                                        binding.webView.loadUrl(url)
                                        binding.webView.visibility = View.VISIBLE
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
                        Timber.wtf("-->>onBindViewHolder spanString " + spanString)

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
                )
                {
                    binding.messageRecyclerView.scrollToPosition(positionStart)
                }
            }
        })
        binding.messageRecyclerView.setAdapter(thisFirebaseAdapter)
        thisFirebaseAdapter.startListening()
    }

}
