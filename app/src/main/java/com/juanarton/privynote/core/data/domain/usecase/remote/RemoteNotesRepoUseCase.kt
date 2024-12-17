package com.juanarton.privynote.core.data.domain.usecase.remote

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.domain.model.LoggedUser
import com.juanarton.privynote.core.data.domain.model.Login
import com.juanarton.privynote.core.data.domain.model.Notes
import com.juanarton.privynote.core.data.domain.model.TwoFactor
import com.juanarton.privynote.core.data.source.remote.Resource
import com.ketch.Ketch
import kotlinx.coroutines.flow.Flow

interface RemoteNotesRepoUseCase {
    fun signInWithGoogle(option: GetSignInWithGoogleOption, activity: Activity): Flow<Resource<LoggedUser>>

    fun logInByEmail(email: String, password: String): Flow<Resource<LoggedUser>>

    fun signInByEmail(email: String, password: String): Flow<Resource<LoggedUser>>

    fun registerUser(id: String, pin: String, username: String): Flow<Resource<String>>

    fun loginUser(id: String, pin: String, otp: String): Flow<Resource<Login>>

    fun insertNoteRemote(notes: Notes): Flow<Resource<String>>

    fun getAllNoteRemote(): Flow<Resource<List<Notes>>>

    fun updateNoteRemote(notes: Notes): Flow<Resource<String>>

    fun deleteNoteRemote(id: String): Flow<Resource<String>>

    fun uploadImageAtt(image: ByteArray, attachment: Attachment): Flow<Resource<Attachment>>

    fun getAttachmentRemote(id: String): Flow<Resource<List<Attachment>>>

    fun getAllAttRemote(): Flow<Resource<List<Attachment>>>

    suspend fun downloadAttachment(url: String, ketch: Ketch): Int

    fun deleteAttById(id: String): Flow<Resource<String>>

    fun logoutUser(refreshToken: String): Flow<Resource<String>>

    fun setTwoFactorAuth(id: String, pin: String): Flow<Resource<TwoFactor>>

    fun disableTwoFactorAuth(id: String, pin: String): Flow<Resource<String>>

    fun checkTwoFactorSet(id: String): Flow<Resource<Boolean>>

    fun checkIsRegistered(id: String): Flow<Resource<Boolean>>

    fun deleteAllNote(): Flow<Resource<String>>

    fun twoFactorAuth(id: String, pin: String, otp: String): Flow<Resource<Login>>
}