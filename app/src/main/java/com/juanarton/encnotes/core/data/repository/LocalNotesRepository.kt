package com.juanarton.encnotes.core.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.ILocalNotesRepository
import com.juanarton.encnotes.core.data.source.local.LocalDataSource
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.DataMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocalNotesRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val sharedPrefDataSource: SharedPrefDataSource,
    private val context: Context
): ILocalNotesRepository {
    override fun setIsLoggedIn(isLoggedIn: Boolean) = flow {
        emit(sharedPrefDataSource.setIsLoggedIn(isLoggedIn))
    }

    override fun getIsLoggedIn(): Boolean = sharedPrefDataSource.getIsLoggedIn()

    override fun getNotes(): Flow<List<Notes>> = flow {
        emit(DataMapper.mapNotesEntityToDomain(localDataSource.getNotes()))
    }.flowOn(Dispatchers.IO)

    /*override fun getNotes(): Flow<PagingData<Notes>> {
        return Pager(
            config = PagingConfig(
                pageSize = 2,
                enablePlaceholders = false,
                initialLoadSize = 10
            ),
            pagingSourceFactory = {
                localDataSource.getNotes()
            }
        ).flow
    }*/

    override fun insertNotes(notes: Notes): Flow<Resource<Boolean>> = flow {
        try {
            localDataSource.insertNotes(
                DataMapper.mapNotesDomainToEntity(notes)
            )
            emit(Resource.Success(true))
        } catch (e: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.insert_error)))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun setAccessKey(accessKey: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setAccessKey(accessKey))
    }

    override fun getAccessKey(): String? = sharedPrefDataSource.getAccessKey()

    override fun setRefreshKey(refreshKey: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setRefreshKey(refreshKey))
    }

    override fun getRefreshKey(): String? = sharedPrefDataSource.getRefreshKey()

    override fun setCipherKey(cipherKey: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setCipherKey(cipherKey))
    }

    override fun getCipherKey(): String? = sharedPrefDataSource.getCipherKey()

    override fun deleteNotes(notes: Notes): Flow<Resource<Boolean>> = flow {
        try {
            localDataSource.deleteNotes(
                NotesEntity(
                    notes.id,
                    "",
                    "",
                    true,
                    notes.lastModified
                )
            )
            emit(Resource.Success(true))
        } catch (e: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.insert_error)))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun getNotesById(id: String): Flow<Notes> = flow {
        emit(DataMapper.mapNoteEntityToDomain(localDataSource.getNotesById(id)))
    }.flowOn(Dispatchers.IO)

    override fun updateNotes(notes: Notes): Flow<Resource<Boolean>> = flow {
        try {
            localDataSource.updateNotes(
                DataMapper.mapNotesDomainToEntity(notes)
            )
            emit(Resource.Success(true))
        } catch (e: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.update_error)))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun permanentDeleteNotes(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            localDataSource.permanentDelete(id)
        }
    }
}