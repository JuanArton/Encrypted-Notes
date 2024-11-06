package com.juanarton.encnotes.core.data.repository

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.authentications.login.LoginData
import com.juanarton.encnotes.core.data.api.note.addnote.PostNoteData
import com.juanarton.encnotes.core.data.api.note.getallnote.NoteData
import com.juanarton.encnotes.core.data.api.user.register.RegisterData
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.IRemoteNoteRepository
import com.juanarton.encnotes.core.data.source.remote.FirebaseDataSource
import com.juanarton.encnotes.core.data.source.remote.NetworkBoundRes
import com.juanarton.encnotes.core.data.source.remote.RemoteDataSource
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.DataMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class RemoteNoteRepository @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val firebaseDataSource: FirebaseDataSource,
): IRemoteNoteRepository {
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

    override fun insertNoteRemote(notes: Notes): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, PostNoteData>() {
            override fun loadFromNetwork(data: PostNoteData): Flow<String> {
                return flowOf(data.noteId)
            }

            override suspend fun createCall(): Flow<APIResponse<PostNoteData>> {
                return remoteDataSource.insertNote(notes)
            }
        }.asFlow()
    }

    override fun getAllNoteRemote(): Flow<Resource<List<Notes>>> {
        return object : NetworkBoundRes<List<Notes>, List<NoteData>>() {
            override fun loadFromNetwork(data: List<NoteData>): Flow<List<Notes>> {
                return flowOf(DataMapper.mapNotesRemoteToDomain(data))
            }

            override suspend fun createCall(): Flow<APIResponse<List<NoteData>>> {
                delay(500)
                return remoteDataSource.getAllNote()
            }
        }.asFlow()
    }

    override fun updateNoteRemote(notes: Notes): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return remoteDataSource.updateNote(notes)
            }
        }.asFlow()
    }

    override fun deleteNoteRemote(id: String): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return remoteDataSource.deleteNote(id)
            }
        }.asFlow()
    }
}