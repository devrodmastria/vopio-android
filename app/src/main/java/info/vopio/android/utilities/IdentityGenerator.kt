package info.vopio.android.utilities

class IdentityGenerator() {
    fun createUserIdFromEmail(userEmail: String) : String{
        val userId = userEmail.replace("@", "+")
        return userId.replace(".", "_")
    }
}