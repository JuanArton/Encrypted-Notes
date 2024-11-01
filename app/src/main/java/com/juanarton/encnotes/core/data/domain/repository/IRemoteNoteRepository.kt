package com.juanarton.encnotes.core.data.domain.repository

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow

interface IRemoteNoteRepository {
    fun signInWithGoogle(option: GetSignInWithGoogleOption, activity: Activity): Flow<Resource<LoggedUser>>

    fun logInByEmail(email: String, password: String): Flow<Resource<LoggedUser>>

    fun signInByEmail(email: String, password: String): Flow<Resource<LoggedUser>>

    fun registerUser(id: String, pin: String, username: String): Flow<Resource<String>>

    fun loginUser(id: String, pin: String): Flow<Resource<Login>>

    fun insertNoteRemote(notes: Notes): Flow<Resource<String>>

    fun getAllNoteRemote(): Flow<Resource<List<Notes>>>

    fun updateNoteRemote(notes: Notes): Flow<Resource<String>>

    fun deleteNoteRemote(id: String): Flow<Resource<String>>
}