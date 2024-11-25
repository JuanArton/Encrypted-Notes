package com.juanarton.encnotes.core.data.source.local

import android.util.Log
import com.juanarton.encnotes.core.data.source.local.room.dao.NotesDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity
import com.juanarton.encnotes.core.utils.Cryptography
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteLocalDataSource @Inject constructor(
    private val notesDAO: NotesDAO,
) {
    fun getNotes(): List<NotesEntity> {
        return notesDAO.getNotes()
    }

    fun insertNotes(notes: NotesEntity) {
        notesDAO.insertNotes(notes)
    }

    fun deleteNotes(notesEntity: NotesEntity) {
        notesDAO.deleteNotes(notesEntity)
    }

    fun updateNotes(notesEntity: NotesEntity) {
        notesDAO.updateNotes(notesEntity)
    }

    fun getNotesById(id: String): NotesEntity {
        return notesDAO.getNotesById(id)
    }

    fun permanentDelete(id: String) {
        notesDAO.permanentDelete(id)
    }
}