package com.juanarton.encnotes.core.data.api.note.updateNote

data class PutNote(
    val title: String,
    val content: String,
    val lastModified: Long
)
