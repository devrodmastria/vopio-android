package info.vopio.android.Utilities

class DatabaseStringAdapter() {
    fun createUserIdFromEmail(userEmail: String) : String{
        val userId = userEmail.replace("@", "+")
        return userId.replace(".", "_")
    }
}