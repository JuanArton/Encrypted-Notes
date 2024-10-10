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
    override fun signInWithGoogle(
        option: GetSignInWithGoogleOption,
        activity: Activity
    ): Flow<Resource<LoggedUser>> =
        iNotesAppRepository.signInWithGoogle(option, activity)

    override fun getNotes(): Flow<PagingData<Notes>> =
        iNotesAppRepository.getNotes()

    override fun insertNotes(notes: Notes): Flow<Resource<Boolean>> =
        iNotesAppRepository.insertNotes(notes)
}