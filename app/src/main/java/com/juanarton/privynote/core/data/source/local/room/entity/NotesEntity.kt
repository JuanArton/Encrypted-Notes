package com.juanarton.privynote.core.data.source.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NotesEntity (
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "notes_title")
    val notesTitle: String?,

    @ColumnInfo(name = "notes_content")
    val notesContent: String?,

    @ColumnInfo(name = "is_delete")
    val isDelete: Boolean,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
)