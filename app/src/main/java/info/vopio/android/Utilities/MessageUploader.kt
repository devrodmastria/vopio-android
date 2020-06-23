package info.vopio.android.Utilities

import com.google.firebase.database.DatabaseReference
import info.vopio.android.DataModel.MessageModel

class MessageUploader() {

    fun sendCaptions(sessionReferenceInDatabase : DatabaseReference, sessionId : String, caption: String, username: String){
        if (caption.isNotEmpty()){

            val captionMessage =
                MessageModel(caption, username)

            sessionReferenceInDatabase.child(sessionId).push().setValue(captionMessage)
        }
    }
}