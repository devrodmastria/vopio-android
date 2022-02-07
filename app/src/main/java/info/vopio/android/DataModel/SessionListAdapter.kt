package info.vopio.android.DataModel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import info.vopio.android.R
import info.vopio.android.Utilities.Constants

class SessionListAdapter(private val sessionSet: List<DataSnapshot>) :
    RecyclerView.Adapter<SessionListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val seshIDTextView: TextView = itemView.findViewById(R.id.sessionIDTextView)

        fun bind(sessionItem: DataSnapshot){
            dateTextView.text = sessionItem.child(Constants.SESSION_DATE).value.toString()

            val sessionKey = sessionItem.key.toString()
            val lastFourDigits = sessionKey.substring(sessionKey.length.minus(4))
            val sessionString = "Session $lastFourDigits"
            seshIDTextView.text = sessionString
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view  = LayoutInflater.from(parent.context)
            .inflate(R.layout.session_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sessionSet.get(position))
    }

    override fun getItemCount(): Int {
        return sessionSet.size
    }

}