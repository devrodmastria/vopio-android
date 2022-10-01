package info.vopio.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.Utilities.Constants
import info.vopio.android.databinding.ActivityReviewSessionBinding
import timber.log.Timber

class SessionReviewActivity : AppCompatActivity() {

    lateinit var thisCaptionsAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisCaptionsLinearLayoutManager : LinearLayoutManager
    lateinit var recyclerView: RecyclerView

    lateinit var databaseRef : DatabaseReference
    lateinit var thisFirebaseUser : String
    lateinit var thisFirebaseEmail : String

    lateinit var sessionId : String
    lateinit var captionId : String
    lateinit var captionAuthor : String

    private lateinit var binding: ActivityReviewSessionBinding

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var captionTextView: TextView = itemView.findViewById(R.id.sessionIDTextView)
        var authorTextView: TextView = itemView.findViewById(R.id.dateTextView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.caption_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.nav_leave -> {

                Timber.i("-->>SpeechX: LEAVE SESH")
                thisCaptionsAdapter.stopListening()

                finish()

            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewSessionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        recyclerView = view.findViewById(R.id.captionRecyclerView)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        databaseRef = FirebaseDatabase.getInstance().reference

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

        val lastFourDigits = sessionId.substring(sessionId.length.minus(4))
        val sessionHeader = "session ID:   $lastFourDigits"
        binding.statusBarTextView.text = sessionHeader

        // setup RecyclerView with last item showing first
        thisCaptionsLinearLayoutManager = LinearLayoutManager(this)
        thisCaptionsLinearLayoutManager.stackFromEnd = false
        binding.captionRecyclerView.layoutManager = thisCaptionsLinearLayoutManager
    }

    override fun onStart() {
        super.onStart()
        configureCaptionSnapshotParser()
    }

    private fun configureCaptionSnapshotParser() {

        val parser: SnapshotParser<MessageModel> =
            SnapshotParser<MessageModel> { dataSnapshot ->
                val captionItemMessage: MessageModel? = dataSnapshot.getValue(MessageModel::class.java)

                if (captionItemMessage != null) {
                    captionItemMessage.id = dataSnapshot.key
                }
                captionItemMessage!!
            }

        val messagesRef: DatabaseReference = databaseRef.child(Constants.SESSION_LIST).child(sessionId).child(Constants.CAPTION_LIST)
        val options: FirebaseRecyclerOptions<MessageModel> =
            FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(messagesRef, parser)
                .build()

        thisCaptionsAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {

                override fun onError(error: DatabaseError) {
                    super.onError(error)

                    Toast.makeText(this@SessionReviewActivity, "Database access denied", Toast.LENGTH_SHORT).show()

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

//                        if (!caption.contains("[")) { // do not hyperlink system notes
//
//                            val wordArray =
//                                caption.split(" ").toTypedArray()
//                            for (wordIs in wordArray) {
//                                if (wordIs.length > 3) {
//                                    val beginIndex = caption.indexOf(wordIs)
//                                    val endIndex = beginIndex + wordIs.length
//                                    val clickableSpan: ClickableSpan = object : ClickableSpan() {
//                                        override fun onClick(@NonNull view: View) {
//
////                                            selectedWord = wordIs
////                                            showInPopup(wordIs)
//
//                                        }
//                                    }
//                                    spanString.setSpan(
//                                        clickableSpan,
//                                        beginIndex,
//                                        endIndex,
//                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                                    )
//                                }
//                            }
//                        }

                        viewHolder.authorTextView.text = captionAuthor
                        viewHolder.captionTextView.text = spanString
                        viewHolder.captionTextView.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }

//        thisCaptionsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
//            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//                super.onItemRangeInserted(positionStart, 1)
//                val friendlyMessageCount = 1 //mFirebaseAdapter.getItemCount();
//                val lastVisiblePosition: Int =
//                    thisCaptionsLinearLayoutManager.findLastCompletelyVisibleItemPosition()
//                // If the recycler view is initially being loaded or the
//                // user is at the bottom of the list, scroll to the bottom
//                // of the list to show the newly added message.
//                if (lastVisiblePosition == -1 ||
//                    positionStart >= friendlyMessageCount - 1 &&
//                    lastVisiblePosition == positionStart - 1
//                ) {
//                    binding.captionRecyclerView.scrollToPosition(positionStart)
//                }
//            }
//        })

        binding.captionRecyclerView.adapter = thisCaptionsAdapter
        thisCaptionsAdapter.startListening()
    }
}