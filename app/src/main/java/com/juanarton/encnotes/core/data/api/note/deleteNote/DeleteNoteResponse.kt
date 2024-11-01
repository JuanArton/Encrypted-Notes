package com.juanarton.encnotes.core.data.api.note.deleteNote

import com.google.gson.annotations.SerializedName

data class DeleteNoteResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("message")
    val message: String = "No message provided"
)
