package com.juanarton.encnotes.core.data.source.local

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.room.dao.NotesDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity
import com.juanarton.encnotes.core.data.utils.DataMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSource @Inject constructor(
    private val notesDAO: NotesDAO
) {
    fun getNotes(): PagingSource<Int, Notes> {
        return object : PagingSource<Int, Notes>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Notes> {
                val page = params.key ?: 0

                return try {
                    val noteList = DataMapper.mapNotesEntityToDomain(
                        notesDAO.getNotes(params.loadSize, page * params.loadSize)
                    )

                    LoadResult.Page(
                        data = noteList,
                        prevKey = if (page == 0) null else page -1,
                        nextKey = if (noteList.isEmpty()) null else page + 1
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
    }

    fun insertNotes(notes: NotesEntity) {
        notesDAO.insertNotes(notes)
    }
}