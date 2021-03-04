package com.reborn.medianote.frontend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.reborn.medianote.R
import com.reborn.medianote.model.note.Note
import java.util.*

class NoteGridAdapter (
        private var noteDataSet: List<Note>,
        private val onItemClickListener: (Note, Int) -> Unit
) : RecyclerView.Adapter<NoteGridAdapter.ViewHolder>(), Filterable {
    var dataSet: List<Note> = noteDataSet

    inner class ViewHolder (view: View, onItemClickListener: (Note, Int) -> Unit) : RecyclerView.ViewHolder (view) {
        val noteTitle: TextView = view.findViewById(R.id.gridNoteTitle)
        val noteContent: TextView = view.findViewById(R.id.gridNoteContent)

        init {
            view.setOnClickListener {
                if (adapterPosition != NO_POSITION) {
                    onItemClickListener(dataSet[adapterPosition], adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.grid_note, parent, false)

        return ViewHolder(view, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.noteTitle.text = dataSet[position].title

        val postContent = dataSet[position].content
        holder.noteContent.text = postContent.substring(0, postContent.length.coerceAtMost(150)).plus("...")
    }

    fun getNoteAt(position: Int) : Note {
        return dataSet[position]
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val search = constraint.toString()
                if (search.isEmpty()) {
                    dataSet = noteDataSet
                } else {
                    val result = arrayListOf<Note>()
                    for (note in noteDataSet) {
                        if (noteContainsSearch(note, search)) {
                            result.add(note)
                        }
                    }
                    dataSet = result
                }

                val filteredList = FilterResults()
                filteredList.values = dataSet
                return filteredList
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                dataSet = results?.values as List<Note>
                notifyDataSetChanged()
            }

            fun noteContainsSearch(note: Note, search: String): Boolean {
                return (note.title.toLowerCase(Locale.getDefault()).contains(search.toLowerCase(Locale.getDefault())) || note.content.toLowerCase(Locale.getDefault()).contains(search.toLowerCase(Locale.getDefault())))
            }
        }
    }
}