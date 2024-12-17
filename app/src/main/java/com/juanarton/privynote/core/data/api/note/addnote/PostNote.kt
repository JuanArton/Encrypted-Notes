package com.juanarton.privynote.core.data.api.note.addnote

data class PostNote(
    val id: String,
    val title: String = "",
    val content: String = "",
    val lastModified: Long
)
