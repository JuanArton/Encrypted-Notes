package com.juanarton.privynote.core.data.api.note.updateNote

data class PutNote(
    val title: String,
    val content: String,
    val lastModified: Long
)
