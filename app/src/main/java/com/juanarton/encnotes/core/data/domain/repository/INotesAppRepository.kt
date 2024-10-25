package com.juanarton.encnotes.core.data.domain.repository

import android.app.Activity
import androidx.paging.PagingData
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow

interface INotesAppRepository {
    fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean>

    fun getIsLoggedIn(): Boolean

    fun signInWithGoogle(option: GetSignInWithGoogleOption, activity: Activity): Flow<Resource<LoggedUser>>

    fun logInByEmail(email: String, password: String): Flow<Resource<LoggedUser>>

    fun signInByEmail(email: String, password: String): Flow<Resource<LoggedUser>>

    //fun getNotes(): Flow<PagingData<Notes>>

    fun getNotes(): Flow<List<Notes>>

    fun insertNotes(notes: Notes): Flow<Resource<Boolean>>

    fun registerUser(id: String, pin: String, username: String): Flow<Resource<String>>

    fun loginUser(id: String, pin: String): Flow<Resource<Login>>

    fun setAccessKey(accessKey: String): Flow<Boolean>

    fun getAccessKey(): String?

    fun setRefreshKey(refreshKey: String): Flow<Boolean>

    fun getRefreshKey(): String?

    fun setCipherKey(cipherKey: String): Flow<Boolean>

    fun getCipherKey(): String?
}