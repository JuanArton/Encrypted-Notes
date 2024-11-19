package com.juanarton.encnotes.core.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.utils.ImageLoader
import com.juanarton.encnotes.databinding.AttachmentItemViewBinding
import com.juanarton.encnotes.ui.activity.main.MainViewModel
import com.juanarton.encnotes.ui.utils.Utils


class AttachmentAdapter(
    private val mainViewModel: MainViewModel
) : RecyclerView.Adapter<AttachmentAdapter.ViewHolder>() {
    var attachmentList: ArrayList<Attachment> = arrayListOf()

    fun setData(attachment: List<Attachment>?) {
        attachmentList.apply {
            clear()
            attachment?.let { addAll(it) }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AttachmentAdapter.ViewHolder {
        val lifeCycleOwner = parent.context as LifecycleOwner
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attachment_item_view, parent, false)
        return ViewHolder(view, lifeCycleOwner)
    }

    override fun onBindViewHolder(holder: AttachmentAdapter.ViewHolder, position: Int) {
        val attachment = attachmentList[position]
        holder.bind(attachment, position)
    }

    override fun getItemCount(): Int = attachmentList.size

    override fun getItemId(position: Int): Long {
        return attachmentList[position].id.hashCode().toLong()
    }

    inner class ViewHolder(itemView: View, private val lifecycleOwner: LifecycleOwner) : RecyclerView.ViewHolder(itemView) {
        private val binding = AttachmentItemViewBinding.bind(itemView)

        fun bind(attachment: Attachment, position: Int) {
            binding.apply {
                root.post {
                    val context = ivAttachmentImg.context


                    val imageLoader = ImageLoader()
                    imageLoader.loadImage(
                        context, attachment.url, binding, mainViewModel, lifecycleOwner
                    )
                }
            }
        }
    }
}