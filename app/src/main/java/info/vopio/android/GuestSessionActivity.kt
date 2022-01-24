package info.vopio.android

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.webkit.WebSettings
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.*
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.MessageUploader
import info.vopio.android.databinding.ActivityGuestSessionBinding
import timber.log.Timber

class GuestSessionActivity : AppCompatActivity() {

    lateinit var webSettings : WebSettings
    lateinit var thisFirebaseDatabaseReference : DatabaseReference

    lateinit var sessionId : String
    lateinit var captionId : String
    lateinit var captionAuthor : String // author could be any un-muted app user in the session

    lateinit var thisFirebaseUser : String
    lateinit var thisFirebaseEmail : String
    lateinit var thisFirebaseAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisLinearLayoutManager : LinearLayoutManager

    private lateinit var binding: ActivityGuestSessionBinding

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var captionTextView: TextView = itemView.findViewById(R.id.captionTextView)
        var authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.caption_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_leave -> {
                Timber.i("-->>SpeechX: LEAVE SESH")
                thisFirebaseAdapter.stopListening()
                finish()
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestSessionBinding.inflate(layoutInflater)
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

        webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true

        // setup RecyclerView with last item showing first
        thisLinearLayoutManager = LinearLayoutManager(this)
        thisLinearLayoutManager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = thisLinearLayoutManager

    }

    override fun onStart() {
        super.onStart()

        binding.webView.visibility = View.VISIBLE
        configureDatabaseSnapshotParser()

    }

    private fun configureDatabaseSnapshotParser() {

        val parser: SnapshotParser<MessageModel> =
            SnapshotParser<MessageModel> { dataSnapshot ->
                val friendlyMessage: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

                if (friendlyMessage != null) {
                    friendlyMessage.id = dataSnapshot.key
                }
                friendlyMessage!!
            }

        val messagesRef: DatabaseReference = thisFirebaseDatabaseReference.child(Constants.SESSION_LIST).child(sessionId).child(Constants.CAPTION_LIST)
        val options: FirebaseRecyclerOptions<MessageModel> =
            FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(messagesRef, parser)
                .build()

        thisFirebaseAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {

                override fun onError(error: DatabaseError) {
                    super.onError(error)

                    Toast.makeText(this@GuestSessionActivity, "Database access denied", Toast.LENGTH_SHORT).show()
                    
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
