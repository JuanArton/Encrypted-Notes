package com.juanarton.encnotes.core.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.juanarton.encnotes.core.data.source.local.room.dao.AttachmentsDAO
import com.juanarton.encnotes.core.data.source.local.room.dao.NotesDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.AttachmentEntity
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity

@Database(entities = [
    NotesEntity::class,
    AttachmentEntity::class
], version = 1, exportSchema = false)
abstract class NotesAppDatabase : RoomDatabase() {
    abstract fun notesDAO(): NotesDAO
    abstract fun attachmentDAO(): AttachmentsDAO
}