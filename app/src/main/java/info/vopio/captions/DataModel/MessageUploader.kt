package info.vopio.captions.DataModel

import com.google.firebase.database.DatabaseReference
import timber.log.Timber

class MessageUploader() {

    fun sendCaptions(dbRef : DatabaseReference, sessionId : String, caption: String, username: String){
        if (caption.isNotEmpty()){

            val captionMessage = MessageModel(caption, username)

            dbRef.push().setValue(captionMessage) // the dbRef is already aware of the session node, so push value directly to new node.

        }
    }

}