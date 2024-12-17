package com.juanarton.privynote.core.data.api.note.deleteNote

import com.google.gson.annotations.SerializedName

data class DeleteResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String = "No message provided"
)
