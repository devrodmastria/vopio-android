package info.vopio.android

import android.view.LayoutInflater
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import info.vopio.android.DataModel.Word

class WordsAdapter(private val onClick: (Word) -> Unit) :
    ListAdapter<Word, WordsAdapter.WordViewHolder>(WordDiffCallback) {

    class WordViewHolder(itemView: View, val onClick: (Word) -> Unit):
            RecyclerView.ViewHolder(itemView){
                private val wordTextView : TextView = itemView.findViewById(R.id.cardText)
                private var currentWord: Word? = null

                init {
                    itemView.setOnClickListener {
                        currentWord?.let {
                            onClick(it)
                        }
                    }
                }

                fun bind(word: Word){
                    currentWord = word
                    wordTextView.text = word.content
                }

            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.library_item, parent, false)
        return WordViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = getItem(position)
        holder.bind(word)
    }

}

object WordDiffCallback : DiffUtil.ItemCallback<Word>(){
    override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
        return oldItem.content == newItem.content
    }

}