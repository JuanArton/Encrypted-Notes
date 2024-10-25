package com.juanarton.encnotes.core.data.api.note.addnote

import com.google.gson.annotations.SerializedName

data class PostNoteResponse (
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val postNoteData: PostNoteData?,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class PostNoteData(
    @SerializedName("notesId")
    val noteId: String,
)