package com.juanarton.encnotes.core.data.repository

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.domain.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.INotesAppRepository
import com.juanarton.encnotes.core.data.source.local.LocalDataSource
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity
import com.juanarton.encnotes.core.data.source.remote.FirebaseDataSource
import com.juanarton.encnotes.core.data.source.remote.NetworkBoundRes
import com.juanarton.encnotes.core.data.source.remote.Resource
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import java.util.Date
import javax.inject.Inject

class NotesAppRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val sharedPrefDataSource: SharedPrefDataSource,
    private val firebaseDataSource: FirebaseDataSource,
    private val context: Context
): INotesAppRepository {
    override fun setIsLoggedIn(isLoggedIn: Boolean) = flow {
        emit(sharedPrefDataSource.setIsLoggedIn(isLoggedIn))
    }

    override fun getIsLoggedIn(): Boolean = sharedPrefDataSource.getIsLoggedIn()

    override fun signInWithGoogle(
        option: GetSignInWithGoogleOption,
        activity: Activity
    ): Flow<Resource<LoggedUser>> {
        return object : NetworkBoundRes<LoggedUser, LoggedUser>() {
            override suspend fun createCall(): Flow<APIResponse<LoggedUser>> {
                return firebaseDataSource.signInWithGoogle(option, activity)
            }

            override fun loadFromNetwork(data: LoggedUser): Flow<LoggedUser> {
                return flowOf(data)
            }
        }.asFlow()
    }

    override fun getNotes(): Flow<PagingData<Notes>> {
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
    }

    override fun insertNotes(
        ownerId: String,
        title: String,
        content: String
    ): Flow<Resource<Boolean>> = flow {
        try {
            localDataSource.insertNotes(
                NotesEntity(
                    NanoId.generate(16),
                    ownerId,
                    title,
                    content,
                    Date().time
                )
            )
            emit(Resource.Success(true))
        } catch (e: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.insert_error)))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)
}