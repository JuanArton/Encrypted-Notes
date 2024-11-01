package com.juanarton.encnotes.core.data.domain.usecase.remote

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteNotesRepoImpl @Inject constructor(
    private val iRemoteNotesRepoUseCase: RemoteNotesRepoUseCase
): RemoteNotesRepoUseCase {
    override fun signInWithGoogle(
        option: GetSignInWithGoogleOption,
        activity: Activity
    ): Flow<Resource<LoggedUser>> =
        iRemoteNotesRepoUseCase.signInWithGoogle(option, activity)

    override fun logInByEmail(email: String, password: String): Flow<Resource<LoggedUser>> =
        iRemoteNotesRepoUseCase.logInByEmail(email, password)

    override fun signInByEmail(email: String, password: String): Flow<Resource<LoggedUser>> =
        iRemoteNotesRepoUseCase.signInByEmail(email, password)

    override fun registerUser(id: String, pin: String, username: String): Flow<Resource<String>> =
        iRemoteNotesRepoUseCase.registerUser(id, pin, username)

    override fun loginUser(id: String, pin: String): Flow<Resource<Login>> =
        iRemoteNotesRepoUseCase.loginUser(id, pin)

    override fun insertNoteRemote(notes: Notes): Flow<Resource<String>> =
        iRemoteNotesRepoUseCase.insertNoteRemote(notes)

    override fun getAllNoteRemote(): Flow<Resource<List<Notes>>> =
        iRemoteNotesRepoUseCase.getAllNoteRemote()

    override fun updateNoteRemote(notes: Notes): Flow<Resource<String>> =
        iRemoteNotesRepoUseCase.updateNoteRemote(notes)

    override fun deleteNoteRemote(id: String): Flow<Resource<String>> =
        iRemoteNotesRepoUseCase.deleteNoteRemote(id)
}