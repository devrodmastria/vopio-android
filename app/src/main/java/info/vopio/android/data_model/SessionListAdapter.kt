package info.vopio.android.data_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import info.vopio.android.R
import info.vopio.android.utilities.Constants

class SessionListAdapter(private val sessionSet: List<DataSnapshot>, private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<SessionListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, val onClick: (String) -> Unit):
        RecyclerView.ViewHolder(itemView){
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val sessionTitleTextView: TextView = itemView.findViewById(R.id.sessionTitleTextView)

        private var sessionID: String? = null

        init {
            itemView.setOnClickListener {
                sessionID?.let {
                    onClick(it)
                }
            }
        }

        fun bind(sessionItem: DataSnapshot){

            dateTextView.text = sessionItem.child(Constants.SESSION_DATE).value.toString()

            var sessionTitle = sessionItem.child(Constants.SESSION_TITLE).value.toString()
            val sessionKey = sessionItem.key.toString()
            sessionID = sessionItem.key.toString()
            val lastFourDigits = sessionKey.substring(sessionKey.length.minus(4))
            if (lastFourDigits == Constants.DEMO_KEY){
                sessionTitle = "Sample: $sessionTitle"
            }

            sessionTitleTextView.text = sessionTitle

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view  = LayoutInflater.from(parent.context)
            .inflate(R.layout.session_item, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sessionSet.get(position))
    }

    override fun getItemCount(): Int {
        return sessionSet.size
    }

}