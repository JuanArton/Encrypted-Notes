package com.juanarton.encnotes.core.data.api.note.postAttachment

import com.google.gson.annotations.SerializedName

class PostAttachmentResponse (
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val postAttachmentData: PostAttachmentData?,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class PostAttachmentData(
    @SerializedName("id")
    val id: String,

    @SerializedName("url")
    val url: String,
)