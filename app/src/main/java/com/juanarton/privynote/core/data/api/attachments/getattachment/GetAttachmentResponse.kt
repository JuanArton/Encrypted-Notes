package com.juanarton.privynote.core.data.api.attachments.getattachment

import com.google.gson.annotations.SerializedName

data class GetAttachmentResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val getAttachmentArray: GetAttachmentArray?,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class GetAttachmentArray(
    @SerializedName("attachments")
    val attachmentData: List<AttachmentData>,
)

data class AttachmentData(
    @SerializedName("id")
    val id: String,

    @SerializedName("notesId")
    val notesId: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("isDelete")
    val isDelete: Boolean = false,

    @SerializedName("type")
    val type: String,

    @SerializedName("lastModified")
    val lastModified: Long
)