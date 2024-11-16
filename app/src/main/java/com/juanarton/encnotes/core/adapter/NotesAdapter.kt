package com.juanarton.encnotes.core.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.AlignContent
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.databinding.NoteItemViewBinding
import com.juanarton.encnotes.ui.activity.main.MainViewModel
import com.juanarton.encnotes.ui.utils.Utils


class NotesAdapter (
    private val onClick: (Notes, MaterialCardView) -> Unit,
    private val mainViewModel: MainViewModel
) : RecyclerView.Adapter<NotesAdapter.ViewHolder>(){
    var noteList: ArrayList<Notes> = arrayListOf()
    var attachmentList: ArrayList<Attachment> = arrayListOf()
    var tracker: SelectionTracker<String>? = null

    init {
        setHasStableIds(true)
    }

    fun setData(notes: List<Notes>?, attachment: List<Attachment>?) {
        Log.d("test1", attachment.toString())
        attachmentList.apply {
            clear()
            attachment?.let { addAll(it) }
        }
        noteList.apply {
            clear()
            notes?.let { addAll(it) }
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
                val attachments = attachmentList.filter {
                    it.noteId == notes.id
                }.take(6)

                val context = tvNotesTitle.context

                val flexboxLayoutManager = FlexboxLayoutManager(context).apply {
                    flexDirection = FlexDirection.ROW
                    maxLine = 2
                }
                rvImgAttachment.layoutManager = flexboxLayoutManager
                    //StaggeredGridLayoutManager(2, LinearLayoutManager.HORIZONTAL)
                //rvImgAttachment.addItemDecoration(GridSpacingItemDecoration(Utils.dpToPx(2, context)))
                val rvAdapter = AttachmentAdapter(mainViewModel)
                rvImgAttachment.adapter = rvAdapter
                Log.d("test", attachments.toString())

                rvAdapter.setData(attachments)

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