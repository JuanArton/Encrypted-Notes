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

    /*fun getNotes(): PagingSource<Int, Notes> {
        return object : PagingSource<Int, Notes>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Notes> {
                val page = params.key ?: 0

                return try {
                    val noteList = DataMapper.mapNotesEntityToDomain(
                        notesDAO.getNotes(params.loadSize, page * params.loadSize)
                    )

                    val key = sharedPrefDataSource.getCipherKey()
                    val deserializedKey = Cryptography.deserializeKeySet(key!!)

                    val decryptedNoteList = noteList.map {
                        Notes(
                            it.id,
                            it.ownerId,
                            it.notesTitle?.let { it1 -> Cryptography.decrypt(it1, deserializedKey) },
                            Cryptography.decrypt(it.notesContent, deserializedKey),
                            it.lastModified
                        )
                    }

                    LoadResult.Page(
                        data = decryptedNoteList,
                        prevKey = if (page == 0) null else page -1,
                        nextKey = if (decryptedNoteList.isEmpty()) null else page + 1
                    )
                } catch (e: Exception) {
                    LoadResult.Error(e)
                }
            }

            override fun getRefreshKey(state: PagingState<Int, Notes>): Int? {
                return state.anchorPosition?.let { anchorPosition ->
                    val anchorPage = state.closestPageToPosition(anchorPosition)
                    anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
                }
            }
        }
    }*/

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