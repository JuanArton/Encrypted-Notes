package com.juanarton.encnotes.core.data.api.note.getallnote

import com.google.gson.annotations.SerializedName

data class GetAllNotesRes(
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val noteList: List<NoteData>?,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class NoteData(
    @SerializedName("id")
    val id: String,

    @SerializedName("notesTitle")
    val title: String?,

    @SerializedName("notesContent")
    val content: String,

    @SerializedName("isDelete")
    val isDelete: Boolean,

    @SerializedName("lastModified")
    val lastModified: Long
)
