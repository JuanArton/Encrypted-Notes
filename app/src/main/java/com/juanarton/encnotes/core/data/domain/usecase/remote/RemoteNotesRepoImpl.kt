package com.juanarton.encnotes.core.data.domain.usecase.remote

import android.app.Activity
import android.content.Context
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.IRemoteNoteRepository
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.ketch.Ketch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteNotesRepoImpl @Inject constructor(
    private val iRemoteNoteRepository: IRemoteNoteRepository
): RemoteNotesRepoUseCase {
    override fun signInWithGoogle(
        option: GetSignInWithGoogleOption,
        activity: Activity
    ): Flow<Resource<LoggedUser>> =
        iRemoteNoteRepository.signInWithGoogle(option, activity)

    override fun logInByEmail(email: String, password: String): Flow<Resource<LoggedUser>> =
        iRemoteNoteRepository.logInByEmail(email, password)

    override fun signInByEmail(email: String, password: String): Flow<Resource<LoggedUser>> =
        iRemoteNoteRepository.signInByEmail(email, password)

    override fun registerUser(id: String, pin: String, username: String): Flow<Resource<String>> =
        iRemoteNoteRepository.registerUser(id, pin, username)

    override fun loginUser(id: String, pin: String): Flow<Resource<Login>> =
        iRemoteNoteRepository.loginUser(id, pin)

    override fun insertNoteRemote(notes: Notes): Flow<Resource<String>> =
        iRemoteNoteRepository.insertNoteRemote(notes)

    override fun getAllNoteRemote(): Flow<Resource<List<Notes>>> =
        iRemoteNoteRepository.getAllNoteRemote()

    override fun updateNoteRemote(notes: Notes): Flow<Resource<String>> =
        iRemoteNoteRepository.updateNoteRemote(notes)

    override fun deleteNoteRemote(id: String): Flow<Resource<String>> =
        iRemoteNoteRepository.deleteNoteRemote(id)

    override fun uploadImageAtt(image: ByteArray, attachment: Attachment): Flow<Resource<Attachment>> =
        iRemoteNoteRepository.uploadImageAttRemote(image, attachment)

    override fun getAttachmentRemote(id: String): Flow<Resource<List<Attachment>>> =
        iRemoteNoteRepository.getAttachmentRemote(id)

    override fun getAllAttRemote(): Flow<Resource<List<Attachment>>> =
        iRemoteNoteRepository.getAllAttRemote()

    override suspend fun downloadAttachment(url: String, ketch: Ketch): Int =
        iRemoteNoteRepository.downloadAttachment(url, ketch)
}