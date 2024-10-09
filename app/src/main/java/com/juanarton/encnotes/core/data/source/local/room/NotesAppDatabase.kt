package com.juanarton.encnotes.core.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.juanarton.encnotes.core.data.source.local.room.dao.AppStateDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.AppStateEntity

@Database(entities = [
    AppStateEntity::class
], version = 2, exportSchema = false)
abstract class NotesAppDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDAO
}