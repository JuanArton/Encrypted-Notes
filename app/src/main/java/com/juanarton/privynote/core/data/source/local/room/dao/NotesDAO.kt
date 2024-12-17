package com.juanarton.privynote.core.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanarton.privynote.core.data.source.local.room.entity.NotesEntity

@Dao
interface NotesDAO {
    @Query("SELECT * FROM notes")
    fun getNotes(): List<NotesEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT, entity = NotesEntity::class)
    fun insertNotes(notesEntity: NotesEntity)

    @Update
    fun deleteNotes(notesEntity: NotesEntity)

    @Update
    fun updateNotes(notesEntity: NotesEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNotesById(id: String): NotesEntity

    @Query("DELETE FROM notes WHERE id = :id")
    fun permanentDelete(id: String)
}