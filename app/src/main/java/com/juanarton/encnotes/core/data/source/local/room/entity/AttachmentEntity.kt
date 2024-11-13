package com.juanarton.encnotes.core.data.source.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class AttachmentEntity (
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "notes_id")
    val noteId: String?,

    @ColumnInfo(name = "url")
    val url: String?,

    @ColumnInfo(name = "is_delete")
    val isDelete: Boolean,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
)