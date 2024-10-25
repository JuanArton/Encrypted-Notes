package com.juanarton.encnotes.core.adapter

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.databinding.NoteItemViewBinding


class NotesAdapter (
    private val onClick: (Notes) -> Unit,
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
                val title = notes.notesTitle
                if (title.isNullOrEmpty()) {
                    tvNotesTitle.visibility = View.GONE
                    tvNotesContent.layoutParams = (binding.tvNotesContent.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        topMargin = 0
                    }
                } else {
                    tvNotesTitle.text = title
                }
                tvNotesContent.text = notes.notesContent
            }
        }
    }
}