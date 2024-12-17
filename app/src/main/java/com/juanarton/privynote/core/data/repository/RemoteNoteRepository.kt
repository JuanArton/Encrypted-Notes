package com.juanarton.privynote.core.data.repository

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.privynote.core.data.api.APIResponse
import com.juanarton.privynote.core.data.api.attachments.getattachment.AttachmentData
import com.juanarton.privynote.core.data.api.authentications.login.LoginData
import com.juanarton.privynote.core.data.api.authentications.twofacor.TwoFactorData
import com.juanarton.privynote.core.data.api.note.addnote.PostNoteData
import com.juanarton.privynote.core.data.api.note.getallnote.NoteData
import com.juanarton.privynote.core.data.api.note.postAttachment.PostAttachmentData
import com.juanarton.privynote.core.data.api.user.register.RegisterData
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.domain.model.LoggedUser
import com.juanarton.privynote.core.data.domain.model.Login
import com.juanarton.privynote.core.data.domain.model.Notes
import com.juanarton.privynote.core.data.domain.model.TwoFactor
import com.juanarton.privynote.core.data.domain.repository.IRemoteNoteRepository
import com.juanarton.privynote.core.data.source.remote.AttachmentRemoteDataSource
import com.juanarton.privynote.core.data.source.remote.FirebaseDataSource
import com.juanarton.privynote.core.data.source.remote.NetworkBoundRes
import com.juanarton.privynote.core.data.source.remote.NoteRemoteDataSource
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.utils.DataMapper
import com.ketch.Ketch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class RemoteNoteRepository @Inject constructor(
    private val noteRemoteDataSource: NoteRemoteDataSource,
    private val firebaseDataSource: FirebaseDataSource,
    private val attachmentRemoteDataSource: AttachmentRemoteDataSource
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
                return noteRemoteDataSource.registerUser(id, pin, username)
            }
        }.asFlow()
    }

    override fun loginUser(id: String, pin: String, otp: String): Flow<Resource<Login>> {
        return object : NetworkBoundRes<Login, LoginData>() {
            override fun loadFromNetwork(data: LoginData): Flow<Login> {
                return flowOf(Login(data.accessToken, data.refreshToken))
            }

            override suspend fun createCall(): Flow<APIResponse<LoginData>> {
                return noteRemoteDataSource.loginUser(id, pin, otp)
            }
        }.asFlow()
    }

    override fun insertNoteRemote(notes: Notes): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, PostNoteData>() {
            override fun loadFromNetwork(data: PostNoteData): Flow<String> {
                return flowOf(data.noteId)
            }

            override suspend fun createCall(): Flow<APIResponse<PostNoteData>> {
                return noteRemoteDataSource.insertNote(notes)
            }
        }.asFlow()
    }

    override fun getAllNoteRemote(): Flow<Resource<List<Notes>>> {
        return object : NetworkBoundRes<List<Notes>, List<NoteData>>() {
            override fun loadFromNetwork(data: List<NoteData>): Flow<List<Notes>> {
                return flowOf(DataMapper.mapNotesRemoteToDomain(data))
            }

            override suspend fun createCall(): Flow<APIResponse<List<NoteData>>> {
                return noteRemoteDataSource.getAllNote()
            }
        }.asFlow()
    }

    override fun updateNoteRemote(notes: Notes): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return noteRemoteDataSource.updateNote(notes)
            }
        }.asFlow()
    }

    override fun deleteNoteRemote(id: String): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return noteRemoteDataSource.deleteNote(id)
            }
        }.asFlow()
    }

    override fun uploadImageAttRemote(image: ByteArray, attachment: Attachment): Flow<Resource<Attachment>> {
        return object : NetworkBoundRes<Attachment, PostAttachmentData>() {
            override fun loadFromNetwork(data: PostAttachmentData): Flow<Attachment> {
                return flowOf(
                    Attachment(data.id, null, data.url, null, null, null)
                )
            }

            override suspend fun createCall(): Flow<APIResponse<PostAttachmentData>> {
                return attachmentRemoteDataSource.uploadImageAtt(image, attachment)
            }
        }.asFlow()
    }

    override fun getAttachmentRemote(id: String): Flow<Resource<List<Attachment>>> {
        return object : NetworkBoundRes<List<Attachment>, List<AttachmentData>>() {
            override fun loadFromNetwork(data: List<AttachmentData>): Flow<List<Attachment>> {
                return flowOf(DataMapper.mapAttachmentsRemoteToDomain(data))
            }

            override suspend fun createCall(): Flow<APIResponse<List<AttachmentData>>> {
                return attachmentRemoteDataSource.getAttById(id)
            }
        }.asFlow()
    }

    override fun getAllAttRemote(): Flow<Resource<List<Attachment>>> {
        return object : NetworkBoundRes<List<Attachment>, List<AttachmentData>>() {
            override fun loadFromNetwork(data: List<AttachmentData>): Flow<List<Attachment>> {
                return flowOf(DataMapper.mapAttachmentsRemoteToDomain(data))
            }

            override suspend fun createCall(): Flow<APIResponse<List<AttachmentData>>> {
                return attachmentRemoteDataSource.getAllAtt()
            }
        }.asFlow()
    }

    override suspend fun downloadAttachment(url: String, ketch: Ketch): Int {
        return attachmentRemoteDataSource.downloadAttachment(url, ketch)
    }

    override fun deleteAttById(id: String): Flow<Resource<String>> {
        return object: NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return attachmentRemoteDataSource.deleteAttById(id)
            }
        }.asFlow()
    }

    override fun logoutUser(refreshToken: String): Flow<Resource<String>> {
        return object: NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return noteRemoteDataSource.logoutUser(refreshToken)
            }
        }.asFlow()
    }

    override fun setTwoFactorAuth(id: String, pin: String): Flow<Resource<TwoFactor>> {
        return object : NetworkBoundRes<TwoFactor, TwoFactorData>() {
            override fun loadFromNetwork(data: TwoFactorData): Flow<TwoFactor> {
                return flowOf(
                    TwoFactor(
                        data.qrImage,
                        data.secret
                    )
                )
            }

            override suspend fun createCall(): Flow<APIResponse<TwoFactorData>> {
                return noteRemoteDataSource.setTwoFactorAuth(id, pin)
            }
        }.asFlow()
    }

    override fun disableTwoFactorAuth(id: String, pin: String): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return noteRemoteDataSource.disableTwoFactorAuth(id, pin)
            }
        }.asFlow()
    }

    override fun checkTwoFactorSet(id: String): Flow<Resource<Boolean>> {
        return object : NetworkBoundRes<Boolean, Boolean>() {
            override fun loadFromNetwork(data: Boolean): Flow<Boolean> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<Boolean>> {
                return noteRemoteDataSource.checkTwoFactorSet(id)
            }
        }.asFlow()
    }

    override fun checkIsRegistered(id: String): Flow<Resource<Boolean>> {
        return object : NetworkBoundRes<Boolean, Boolean>() {
            override fun loadFromNetwork(data: Boolean): Flow<Boolean> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<Boolean>> {
                return noteRemoteDataSource.checkIsRegistered(id)
            }
        }.asFlow()
    }

    override fun deleteAllNote(): Flow<Resource<String>> {
        return object : NetworkBoundRes<String, String>() {
            override fun loadFromNetwork(data: String): Flow<String> {
                return flowOf(data)
            }

            override suspend fun createCall(): Flow<APIResponse<String>> {
                return noteRemoteDataSource.deleteAllNote()
            }
        }.asFlow()
    }

    override fun twoFactorAuth(id: String, pin: String, otp: String): Flow<Resource<Login>> {
        return object : NetworkBoundRes<Login, LoginData>() {
            override fun loadFromNetwork(data: LoginData): Flow<Login> {
                return flowOf(Login(data.accessToken, data.refreshToken))
            }

            override suspend fun createCall(): Flow<APIResponse<LoginData>> {
                return noteRemoteDataSource.twoFactorAuth(id, pin, otp)
            }
        }.asFlow()
    }
}