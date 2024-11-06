package com.juanarton.encnotes.core.data.domain.usecase.local

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow

interface LocalNotesRepoUseCase {
    fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean>

    fun getIsLoggedIn(): Boolean

    //fun getNotes(): Flow<PagingData<Notes>>

    fun getNotes(): Flow<List<Notes>>

    fun insertNotes(notes: Notes): Flow<Resource<Boolean>>

    fun setAccessKey(accessKey: String): Flow<Boolean>

    fun getAccessKey(): String?

    fun setRefreshKey(refreshKey: String): Flow<Boolean>

    fun getRefreshKey(): String?

    fun setCipherKey(cipherKey: String): Flow<Boolean>

    fun getCipherKey(): String?

    fun deleteNotes(notes: Notes): Flow<Resource<Boolean>>

    fun getNotesById(id: String): Flow<Notes>

    fun updateNotes(notes: Notes): Flow<Resource<Boolean>>

    fun permanentDeleteNotes(id: String)
}