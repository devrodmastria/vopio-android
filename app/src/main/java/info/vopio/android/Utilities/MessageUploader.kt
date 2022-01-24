package info.vopio.android.Utilities

import com.google.firebase.database.DatabaseReference
import info.vopio.android.DataModel.MessageModel

class MessageUploader() {

    fun sendCaptions(sessionReferenceInDatabase : DatabaseReference, sessionId : String, caption: String, username: String){
        if (caption.isNotEmpty()){

            val captionMessage = MessageModel(caption, username)

            sessionReferenceInDatabase.child(Constants.SESSION_LIST).child(sessionId).child(Constants.CAPTION_LIST).push().setValue(captionMessage)
        }
    }

    fun saveWord(thisFirebaseRef : DatabaseReference, word: String, user_email: String){

        val userId = DatabaseStringAdapter().createUserIdFromEmail(user_email)
        thisFirebaseRef.child(Constants.STUDENT_LIST).child(userId).child(Constants.SAVED_WORDS).push().setValue(word)
    }
}