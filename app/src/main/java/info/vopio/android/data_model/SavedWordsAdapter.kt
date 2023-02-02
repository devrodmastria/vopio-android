package info.vopio.android.data_model

import android.view.LayoutInflater
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import info.vopio.android.R

class SavedWordsAdapter(private val onClick: (SavedWord) -> Unit) :
    ListAdapter<SavedWord, SavedWordsAdapter.WordViewHolder>(WordDiffCallback) {

    class WordViewHolder(itemView: View, val onClick: (SavedWord) -> Unit):
            RecyclerView.ViewHolder(itemView){
                private val wordTextView : TextView = itemView.findViewById(R.id.cardText)
                private var currentSavedWord: SavedWord? = null

                init {
                    itemView.setOnClickListener {
                        currentSavedWord?.let {
                            onClick(it)
                        }
                    }
                }

                fun bind(savedWord: SavedWord){
                    currentSavedWord = savedWord
                    wordTextView.text = savedWord.content
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

object WordDiffCallback : DiffUtil.ItemCallback<SavedWord>(){
    override fun areItemsTheSame(oldItem: SavedWord, newItem: SavedWord): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: SavedWord, newItem: SavedWord): Boolean {
        return oldItem.content == newItem.content
    }

}