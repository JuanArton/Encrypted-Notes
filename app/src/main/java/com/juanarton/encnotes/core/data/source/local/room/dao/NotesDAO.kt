package com.juanarton.encnotes.core.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity

@Dao
interface NotesDAO {
    @Query("SELECT * FROM notes ORDER BY last_modified DESC")
    fun getNotes(): List<NotesEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT, entity = NotesEntity::class)
    fun insertNotes(notesEntity: NotesEntity)

    @Update
    fun deleteNotes(notesEntity: NotesEntity)

    @Update
    fun updateNotes(notesEntity: NotesEntity)
}