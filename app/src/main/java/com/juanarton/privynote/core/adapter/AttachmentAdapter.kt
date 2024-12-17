package com.juanarton.privynote.core.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.privynote.core.utils.ImageLoader
import com.juanarton.privynote.databinding.AttachmentItemViewBinding
import com.ketch.Ketch

class AttachmentAdapter(
    private val onClick: (Attachment, ImageView) -> Unit,
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase,
    private val ketch: Ketch,
    private val context: Context
) : RecyclerView.Adapter<AttachmentAdapter.ViewHolder>() {
    private var attachmentList: ArrayList<Attachment> = arrayListOf()

    fun setData(attachment: List<Attachment>?) {
        attachmentList.apply {
            clear()
            attachment?.let { addAll(it) }
            notifyDataSetChanged()
        }
    }

    fun addData(attachment: Attachment) {
        attachmentList.add(0, attachment)
        notifyItemInserted(0)
    }

    fun deleteData(index: Int) {
        attachmentList.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val lifeCycleOwner = context as LifecycleOwner
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attachment_item_view, parent, false)
        return ViewHolder(view, lifeCycleOwner)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attachment = attachmentList[position]
        holder.bind(attachment)
    }

    override fun getItemCount(): Int = attachmentList.size

    override fun getItemId(position: Int): Long {
        return attachmentList[position].id.hashCode().toLong()
    }

    inner class ViewHolder(itemView: View, private val lifecycleOwner: LifecycleOwner) : RecyclerView.ViewHolder(itemView) {
        private val binding = AttachmentItemViewBinding.bind(itemView)

        fun bind(attachment: Attachment) {
            binding.apply {
                root.post {
                    val context = ivAttachmentImg.context

                    val imageLoader = ImageLoader()
                    imageLoader.loadImage(
                        context, attachment.url, ivAttachmentImg, ivAttachmentImgBg, localNotesRepoUseCase,
                        remoteNotesRepoUseCase, lifecycleOwner, ketch, cpiLoading, tvProgress, true
                    )

                    ivAttachmentImg.setOnClickListener {
                        onClick(attachment, ivAttachmentImg)
                    }
                }
            }
        }
    }
}