package com.juanarton.privynote.core.data.api.note.updateNote

import com.google.gson.annotations.SerializedName

data class PutNoteResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String = "No message provided"
)
