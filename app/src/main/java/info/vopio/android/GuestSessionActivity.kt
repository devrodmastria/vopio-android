package info.vopio.android

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import info.vopio.android.DataModel.MessageModel
import info.vopio.android.DataModel.Word
import info.vopio.android.Utilities.Constants
import info.vopio.android.Utilities.IdentityGenerator
import info.vopio.android.Utilities.MessageUploader
import info.vopio.android.databinding.ActivityGuestSessionBinding
import timber.log.Timber

class GuestSessionActivity : AppCompatActivity() {



    lateinit var sessionId : String
    lateinit var captionId : String
    lateinit var captionAuthor : String

    private var popupWindow: PopupWindow? = null
    private var popupView: View? = null
    private var webView: WebView? = null
    private var webSettings : WebSettings? = null

    private var selectedWord: String = "_word_"
    private var reviewMode = false

    lateinit var thisFirebaseUser : String
    lateinit var thisFirebaseEmail : String

    lateinit var thisCaptionsAdapter : FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>
    lateinit var thisCaptionsLinearLayoutManager : LinearLayoutManager

    lateinit var thisWordsLinearLayoutManager : LinearLayoutManager
    lateinit var recyclerView: RecyclerView

    lateinit var databaseRef : DatabaseReference
    lateinit var dataSnapshotList : DataSnapshot
    private val savedWordsList = mutableListOf<Word>()

    private lateinit var binding: ActivityGuestSessionBinding

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
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                thisCaptionsAdapter.stopListening()

                if (!reviewMode){ leaveAttendance() }

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

        recyclerView = view.findViewById(R.id.savedWordsRecyclerView)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        Timber.i("-->>SpeechX: onCreate")

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

            val sessionMode = extras.getBoolean(Constants.REVIEW_MODE)
            sessionMode.let {
                reviewMode = it
            }

        }

        val lastFourDigits = sessionId.substring(sessionId.length.minus(4))
        val sessionHeader = "session ID:   $lastFourDigits"
        binding.statusBarTextView.text = sessionHeader

        // setup RecyclerView with last item showing first if session is not on review mode
        thisCaptionsLinearLayoutManager = LinearLayoutManager(this)
        thisCaptionsLinearLayoutManager.stackFromEnd = (!reviewMode)
        binding.liveCaptionRecyclerView.layoutManager = thisCaptionsLinearLayoutManager

        thisWordsLinearLayoutManager = LinearLayoutManager(this)
        thisWordsLinearLayoutManager.stackFromEnd = false
        binding.savedWordsRecyclerView.layoutManager = thisWordsLinearLayoutManager

        initPopupDictionary()

        configureSavedWordParser()

    }

    private fun adapterOnClick(word: Word){
        // display info about card
        Timber.i("-->>SpeechX: adapterOnClick CLICK:$word")
    }

    private fun initPopupDictionary(){

        popupView = layoutInflater.inflate(R.layout.dictionary_popup_window, binding.root, false)

        popupView?.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
            popupWindow?.dismiss()
        }



        popupView?.findViewById<Button>(R.id.askProfButton)?.setOnClickListener {

            val questionIs = "Please clarify: $selectedWord"
            MessageUploader().sendQuestion(databaseRef, sessionId, questionIs, thisFirebaseUser)

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder
                .setTitle(Constants.POP_TITLE_DONE)
                .setMessage(Constants.POP_MESSAGE_SUCCESS)
                .setNeutralButton("Ok") { dialog, which ->
                    popupWindow?.dismiss()
                    dialog.cancel()
                }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        val viewHeight = ViewGroup.LayoutParams.WRAP_CONTENT
        val viewWidth = ViewGroup.LayoutParams.MATCH_PARENT

        popupView?.measure(viewWidth, viewHeight)

        popupWindow = PopupWindow(
            popupView,
            viewWidth,
            viewHeight,
            true
        )

        webView = popupView?.findViewById<WebView>(R.id.webView)

    }

    private fun showInPopup(wordIs: String){

        MessageUploader().saveWord(databaseRef, wordIs, thisFirebaseEmail)

        val location = IntArray(2)
        val viewAnchor = binding.root.rootView
        viewAnchor.getLocationInWindow(location)

        popupWindow?.showAtLocation(viewAnchor, Gravity.BOTTOM, location[0], location[1])

        val url = "https://duckduckgo.com/?q=define+$wordIs&t=ffab&ia=definition"
        webView?.loadUrl(url)
        webSettings = webView!!.settings
        webSettings!!.javaScriptEnabled = true


    }

    override fun onStart() {
        super.onStart()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        configureCaptionSnapshotParser()
        configureSavedWordParser()
        if (!reviewMode){ declareAttendance() }
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun declareAttendance(){
        MessageUploader().declareAttendance(databaseRef, sessionId, thisFirebaseUser, thisFirebaseEmail)
    }

    private fun leaveAttendance(){
        val questionIs = "$thisFirebaseUser left this session"
        MessageUploader().sendQuestion(databaseRef, sessionId, questionIs, thisFirebaseUser)
    }

    private fun configureSavedWordParser(){

        val wordsAdapter = WordsAdapter { word -> adapterOnClick(word) }

        databaseRef = FirebaseDatabase.getInstance().reference
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                // check email list against local user email
                dataSnapshotList = dataSnapshot

                val userId = IdentityGenerator().createUserIdFromEmail(thisFirebaseEmail)
                val studentDataSnapshot : DataSnapshot = dataSnapshot.child(Constants.STUDENT_LIST).child(userId).child(Constants.SAVED_WORDS)
                if (studentDataSnapshot.hasChildren()){

                    savedWordsList.clear()
                    for (word in studentDataSnapshot.children){
                        val wordItem = word.value.toString()
                        val wordKey = word.key.toString()
                        savedWordsList.add(Word(wordItem, wordKey))
                    }

                } else {
                    savedWordsList.clear()
                    savedWordsList.add(Word("Sample", "sample_key"))
                }
                wordsAdapter.submitList(savedWordsList)
                recyclerView.adapter = wordsAdapter


            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Timber.i("-->>SpeechX: onDataChange Failed to read value:${error.toException()}")
            }
        })

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
                                if (wordIs.length > 3) {
                                    val beginIndex = caption.indexOf(wordIs)
                                    val endIndex = beginIndex + wordIs.length
                                    val clickableSpan: ClickableSpan = object : ClickableSpan() {
                                        override fun onClick(@NonNull view: View) {

                                            selectedWord = wordIs
                                            showInPopup(wordIs)

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
        thisCaptionsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, 1)
                val friendlyMessageCount = 1 //mFirebaseAdapter.getItemCount();
                val lastVisiblePosition: Int =
                    thisCaptionsLinearLayoutManager.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.

                if (lastVisiblePosition == -1 ||
                    positionStart >= friendlyMessageCount - 1 &&
                    lastVisiblePosition == positionStart - 1
                ) {
                    if (!reviewMode) { binding.liveCaptionRecyclerView.scrollToPosition(positionStart) }
                }
            }
        })
        binding.liveCaptionRecyclerView.adapter = thisCaptionsAdapter
        thisCaptionsAdapter.startListening()
    }

}
