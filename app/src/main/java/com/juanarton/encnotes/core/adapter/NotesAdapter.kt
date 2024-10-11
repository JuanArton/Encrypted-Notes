package com.juanarton.encnotes.core.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.databinding.NoteItemViewBinding

class NotesAdapter (
    private val context: Context
) : PagingDataAdapter<Notes, NotesAdapter.ViewHolder>(NotesComparator) {
    object NotesComparator: DiffUtil.ItemCallback<Notes>() {
        override fun areItemsTheSame(oldItem: Notes, newItem: Notes): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notes, newItem: Notes): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.note_item_view,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotesAdapter.ViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = NoteItemViewBinding.bind(itemView)
        fun bind(notes: Notes) {
            binding.apply {
                tvNotesTitle.text = notes.notesTitle
                tvNotesContent.text = notes.notesContent
            }
        }
    }
}