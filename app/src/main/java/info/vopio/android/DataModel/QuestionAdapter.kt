package info.vopio.android.DataModel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.vopio.android.R

class QuestionAdapter(private val questionSet: List<MessageModel>) :
    RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val nameTextView: TextView = view.findViewById(R.id.authorTextView)
        private val questionTextView: TextView = view.findViewById(R.id.captionTextView)

        fun bind(messageItem: MessageModel){
            nameTextView.text = messageItem.name.toString()
            questionTextView.text = messageItem.text.toString()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view  = LayoutInflater.from(parent.context).inflate(R.layout.caption_item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(questionSet.get(position))
    }

    override fun getItemCount(): Int {
        return questionSet.size
    }

}