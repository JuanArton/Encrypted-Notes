package com.juanarton.encnotes.core.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity

@Dao
interface NotesDAO {
    @Query("SELECT * FROM notes ORDER BY last_modified LIMIT :limit OFFSET :offset")
    suspend fun getNotes(limit: Int, offset: Int): List<NotesEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT, entity = NotesEntity::class)
    fun insertNotes(notesEntity: NotesEntity)
}