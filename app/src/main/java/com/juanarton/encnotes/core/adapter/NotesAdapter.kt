package com.juanarton.encnotes.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.databinding.NoteItemViewBinding


class NotesAdapter (
    private val onClick: (Notes, MaterialCardView) -> Unit,
    private val noteList: ArrayList<Notes>
) : RecyclerView.Adapter<NotesAdapter.ViewHolder>(){

    fun setData(items: List<Notes>?) {
        noteList.apply {
            clear()
            items?.let { addAll(it) }
            notifyDataSetChanged()
        }
    }

    fun prependItem(item: Notes) {
        noteList.add(0, item)
        notifyItemInserted(0)
    }

    fun updateItem(index: Int, notes: Notes) {
        noteList[index] = notes
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotesAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotesAdapter.ViewHolder, position: Int) = holder.bind(noteList[position])

    override fun getItemCount(): Int = noteList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = NoteItemViewBinding.bind(itemView)
        fun bind(notes: Notes) {
            binding.apply {
                val title = notes.notesTitle?.trim()
                val content = notes.notesContent?.trim()
                if (title.isNullOrEmpty()) {
                    tvNotesTitle.visibility = View.GONE
                    (tvNotesContent.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
                } else {
                    tvNotesTitle.visibility = View.VISIBLE
                    tvNotesTitle.text = title
                    (tvNotesContent.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 10
                }

                if (content.isNullOrEmpty()) {
                    tvNotesContent.visibility = View.GONE
                } else {
                    tvNotesContent.visibility = View.VISIBLE
                    tvNotesContent.text = content
                }

                itemView.setOnClickListener {
                    onClick(notes, noteItem)
                }
            }
        }
    }
}