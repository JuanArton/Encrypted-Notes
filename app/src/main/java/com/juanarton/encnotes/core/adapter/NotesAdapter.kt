package com.juanarton.encnotes.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.databinding.NoteItemViewBinding


class NotesAdapter (
    private val onClick: (Notes, MaterialCardView) -> Unit,
) : RecyclerView.Adapter<NotesAdapter.ViewHolder>(){
    var noteList: ArrayList<Notes> = arrayListOf()
    var tracker: SelectionTracker<String>? = null

    init {
        setHasStableIds(true)
    }

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

    fun deleteItem(index: Int) {
        noteList.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotesAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotesAdapter.ViewHolder, position: Int) {
        val note = noteList[position]
        //holder.bind(note, false)
        tracker?.let { holder.bind(note, it.isSelected(noteList[position].id)) }
    }

    override fun getItemCount(): Int = noteList.size

    override fun getItemId(position: Int): Long {
        return noteList[position].id.hashCode().toLong()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = NoteItemViewBinding.bind(itemView)
        fun bind(notes: Notes, isActive: Boolean) {
            binding.apply {
                val title = notes.notesTitle?.trim()
                val content = notes.notesContent?.trim()
                if (title.isNullOrEmpty()) {
                    tvNotesTitle.visibility = View.GONE
                    (tvNotesContent.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
                } else {
                    tvNotesTitle.visibility = View.VISIBLE
                    tvNotesTitle.text = title
                }

                if (content.isNullOrEmpty()) {
                    tvNotesContent.visibility = View.GONE
                } else {
                    tvNotesContent.visibility = View.VISIBLE
                    tvNotesContent.text = content
                }

                if (isActive) {
                    binding.root.setBackgroundColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.transparentBlack
                        )
                    )
                } else {
                    binding.root.setBackgroundColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.brightSurface
                        ))
                }

                itemView.setOnClickListener {
                    onClick(notes, noteItem)
                }
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): String = noteList[bindingAdapterPosition].id
            }
    }
}