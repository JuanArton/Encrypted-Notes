package com.juanarton.encnotes.core.adapter

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.NotesPair
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.databinding.NoteItemViewBinding
import com.juanarton.encnotes.ui.utils.Utils
import com.ketch.Ketch

class NotesAdapter (
    private val onClick: (NotesPair, MaterialCardView) -> Unit,
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase,
    private val ketch: Ketch
) : RecyclerView.Adapter<NotesAdapter.ViewHolder>(){
    var noteList: ArrayList<NotesPair> = arrayListOf()
    var tracker: SelectionTracker<String>? = null

    init {
        setHasStableIds(true)
    }

    fun setData(notes: List<NotesPair>?) {
        noteList.apply {
            clear()
            notes?.let { addAll(it) }
            notifyDataSetChanged()
        }
    }

    fun prependItem(item: NotesPair) {
        noteList.add(0, item)
        notifyItemInserted(0)
    }

    fun updateItem(index: Int, notes: NotesPair) {
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
        tracker?.let { holder.bind(note, it.isSelected(noteList[position].notes.id)) }
    }

    override fun getItemCount(): Int = noteList.size

    override fun getItemId(position: Int): Long {
        return noteList[position].notes.id.hashCode().toLong()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = NoteItemViewBinding.bind(itemView)
        fun bind(notes: NotesPair, isActive: Boolean) {
            binding.apply {
                val title = notes.notes.notesTitle?.trim()
                val content = notes.notes.notesContent?.trim()
                val attachment = notes.attachmentList.asReversed().take(6)

                val context = tvNotesTitle.context

                val flexboxLayoutManager = FlexboxLayoutManager(context).apply {
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.SPACE_BETWEEN
                    maxLine = 2
                }
                val divider = ContextCompat.getDrawable(context, R.drawable.divider)!!
                val dividerItemDecoration = DividerItemDecoration(divider)

                val span = if (attachment.size in 1..2) attachment.size else 3

                if (attachment.size < 3) {
                    rvImgAttachment.layoutManager = GridLayoutManager(context, span)
                } else if (attachment.size in 3..5) {
                    rvImgAttachment.layoutManager = flexboxLayoutManager
                    rvImgAttachment.addItemDecoration(dividerItemDecoration)
                } else {
                    rvImgAttachment.layoutManager = GridLayoutManager(context, span)
                }

                if (rvImgAttachment.itemDecorationCount < 1) {
                    rvImgAttachment.addItemDecoration(GridSpacingItemDecoration(Utils.dpToPx(2, context), 0, 0))
                }

                val listener: (
                    Attachment, ImageView
                ) -> Unit = { _, _ ->
                    }

                val rvAdapter = AttachmentAdapter(
                    listener, localNotesRepoUseCase, remoteNotesRepoUseCase, ketch
                )
                rvImgAttachment.adapter = rvAdapter

                rvAdapter.setData(attachment)

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

                if (title.isNullOrEmpty() && content.isNullOrEmpty() && notes.attachmentList.isNotEmpty()) {
                    llNotes.visibility = View.GONE
                } else llNotes.visibility = View.VISIBLE

                val typedValue = TypedValue()
                clickMask.context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                if (isActive) {
                    binding.apply {
                        clickMask.setBackgroundColor(
                            ContextCompat.getColor(
                                binding.clickMask.context,
                                R.color.transparentBlack
                            )
                        )
                        noteItem.strokeColor = typedValue.data
                    }
                } else {
                    binding.clickMask.setBackgroundColor(Color.TRANSPARENT)
                    noteItem.strokeColor = ContextCompat.getColor(
                        binding.clickMask.context,
                        R.color.outlineColor
                    )
                }

                binding.clickMask.setOnClickListener {
                    onClick(notes, noteItem)
                }
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): String = noteList[bindingAdapterPosition].notes.id
            }
    }
}