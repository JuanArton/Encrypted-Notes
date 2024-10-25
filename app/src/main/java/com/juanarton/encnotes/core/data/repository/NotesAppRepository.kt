package com.juanarton.encnotes.core.data.repository

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.authentications.login.LoginData
import com.juanarton.encnotes.core.data.api.user.register.RegisterData
import com.juanarton.encnotes.core.data.domain.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.INotesAppRepository
import com.juanarton.encnotes.core.data.source.local.LocalDataSource
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity
import com.juanarton.encnotes.core.data.source.remote.FirebaseDataSource
import com.juanarton.encnotes.core.data.source.remote.NetworkBoundRes
import com.juanarton.encnotes.core.data.source.remote.RemoteDataSource
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.core.utils.DataMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class NotesAppRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
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

    override fun logInByEmail(email: String, password: String): Flow<Resource<LoggedUser>> {
        return object : NetworkBoundRes<LoggedUser, LoggedUser>() {
            override suspend fun createCall(): Flow<APIResponse<LoggedUser>> {
                return firebaseDataSource.loginByEmail(email, password)
            }

            override fun loadFromNetwork(data: LoggedUser): Flow<LoggedUser> {
                return flowOf(data)
            }
        }.asFlow()
    }

    override fun signInByEmail(email: String, password: String): Flow<Resource<LoggedUser>> {
        return object : NetworkBoundRes<LoggedUser, LoggedUser>() {
            override suspend fun createCall(): Flow<APIResponse<LoggedUser>> {
                return firebaseDataSource.signInByEmail(email, password)
            }

            override fun loadFromNetwork(data: LoggedUser): Flow<LoggedUser> {
                return flowOf(data)
            }
        }.asFlow()
    }

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
            val key = sharedPrefDataSource.getCipherKey()
            if (!key.isNullOrEmpty()) {
                val deserializedKey = Cryptography.deserializeKeySet(key)
                localDataSource.insertNotes(
                    NotesEntity(
                        notes.id,
                        notes.ownerId,
                        notes.notesTitle?.let { Cryptography.encrypt(it, deserializedKey) },
                        Cryptography.encrypt(notes.notesContent, deserializedKey),
                        notes.isDelete,
                        notes.lastModified
                    )
                )
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(context.getString(R.string.unable_retrieve_key)))
            }
        } catch (e: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.insert_error)))
        } catch (e: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun registerUser(id: String, pin: String, username: String): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, RegisterData>() {
            override fun loadFromNetwork(data: RegisterData): Flow<String> {
                return flowOf(data.userId)
            }

            override suspend fun createCall(): Flow<APIResponse<RegisterData>> {
                return remoteDataSource.registerUser(id, pin, username)
            }
        }.asFlow()
    }

    override fun loginUser(id: String, pin: String): Flow<Resource<Login>> {
        return object : NetworkBoundRes<Login, LoginData>() {
            override fun loadFromNetwork(data: LoginData): Flow<Login> {
                return flowOf(Login(data.accessToken, data.refreshToken))
            }

            override suspend fun createCall(): Flow<APIResponse<LoginData>> {
                return remoteDataSource.loginUser(id, pin)
            }
        }.asFlow()
    }

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
}