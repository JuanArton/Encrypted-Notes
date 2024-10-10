package com.juanarton.encnotes.core.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.juanarton.encnotes.core.data.source.local.room.dao.NotesDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity

@Database(entities = [
    NotesEntity::class
], version = 2, exportSchema = false)
abstract class NotesAppDatabase : RoomDatabase() {
    abstract fun notesDAO(): NotesDAO
}