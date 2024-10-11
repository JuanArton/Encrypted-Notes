package com.juanarton.encnotes.core.data.domain.usecase

import android.app.Activity
import androidx.paging.PagingData
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.INotesAppRepository
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotesAppRepositoryImpl @Inject constructor(
    private val iNotesAppRepository: INotesAppRepository
): NotesAppRepositoryUseCase {
    override fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean> =
        iNotesAppRepository.setIsLoggedIn(isLoggedIn)

    override fun getIsLoggedIn(): Boolean = iNotesAppRepository.getIsLoggedIn()

    override fun signInWithGoogle(
        option: GetSignInWithGoogleOption,
        activity: Activity
    ): Flow<Resource<LoggedUser>> =
        iNotesAppRepository.signInWithGoogle(option, activity)

    override fun getNotes(): Flow<PagingData<Notes>> =
        iNotesAppRepository.getNotes()

    override fun insertNotes(
        ownerId: String,
        title: String,
        content: String
    ): Flow<Resource<Boolean>> =
        iNotesAppRepository.insertNotes(ownerId, title, content)
}