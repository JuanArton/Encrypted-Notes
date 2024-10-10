package com.juanarton.encnotes.core.data.domain.usecase

import android.app.Activity
import androidx.paging.PagingData
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow

interface NotesAppRepositoryUseCase {
    fun signInWithGoogle(option: GetSignInWithGoogleOption, activity: Activity): Flow<Resource<LoggedUser>>
    fun getNotes(): Flow<PagingData<Notes>>
    fun insertNotes(notes: Notes): Flow<Resource<Boolean>>
}