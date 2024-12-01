package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.authentications.login.LoginData
import com.juanarton.encnotes.core.data.api.authentications.login.LoginResponse
import com.juanarton.encnotes.core.data.api.authentications.login.PostLogin
import com.juanarton.encnotes.core.data.api.authentications.logout.DeleteLogout
import com.juanarton.encnotes.core.data.api.authentications.logout.LogoutResponse
import com.juanarton.encnotes.core.data.api.authentications.twofacor.CheckTwoFAResponse
import com.juanarton.encnotes.core.data.api.authentications.twofacor.TwoFactorData
import com.juanarton.encnotes.core.data.api.authentications.twofacor.TwoFactorResponse
import com.juanarton.encnotes.core.data.api.authentications.updatekey.PutUpdateKey
import com.juanarton.encnotes.core.data.api.note.addnote.PostNote
import com.juanarton.encnotes.core.data.api.note.addnote.PostNoteData
import com.juanarton.encnotes.core.data.api.note.addnote.PostNoteResponse
import com.juanarton.encnotes.core.data.api.note.deleteNote.DeleteResponse
import com.juanarton.encnotes.core.data.api.note.getallnote.GetAllNotesRes
import com.juanarton.encnotes.core.data.api.note.getallnote.NoteData
import com.juanarton.encnotes.core.data.api.note.updateNote.PutNote
import com.juanarton.encnotes.core.data.api.note.updateNote.PutNoteResponse
import com.juanarton.encnotes.core.data.api.user.check.PostCheck
import com.juanarton.encnotes.core.data.api.user.register.PostRegister
import com.juanarton.encnotes.core.data.api.user.register.RegisterData
import com.juanarton.encnotes.core.data.api.user.register.RegisterResponse
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRemoteDataSource @Inject constructor(
    private val context: Context,
    private val sharedPrefDataSource: SharedPrefDataSource
){
    fun registerUser(id: String, pin: String, username: String): Flow<APIResponse<RegisterData>> =
        flow {
            try {
                val response = API.services.register(PostRegister(id, pin, username))

                if (response.body() != null) {
                    val registerResponse = Gson().fromJson(response.body()!!.string(), RegisterResponse::class.java)
                    emit(APIResponse.Success(registerResponse.registerData))
                } else {
                    val registerResponse = Gson().fromJson(response.errorBody()!!.string(), RegisterResponse::class.java)
                    emit(APIResponse.Error(registerResponse.message))
                }
            } catch (e: Exception) {
                emit(APIResponse.Error(
                    buildString {
                        append(context.getString(R.string.register_failed))
                        append(e.toString())
                    }
                ))
            }
        }.flowOn(Dispatchers.IO)


    fun loginUser(id: String, pin: String, otp: String): Flow<APIResponse<LoginData>> =
        flow {
            try {
                val response = API.services.login(PostLogin(id, pin, otp))

                if (response.body() != null) {
                    val loginResponse = Gson().fromJson(response.body()!!.string(), LoginResponse::class.java)
                    emit(APIResponse.Success(loginResponse.loginData))
                } else {
                    val loginResponse = Gson().fromJson(response.errorBody()!!.string(), LoginResponse::class.java)
                    emit(APIResponse.Error(loginResponse.message))
                }
            } catch (e: Exception) {
                emit(APIResponse.Error(
                    buildString {
                        append(context.getString(R.string.login_failed))
                        append(e.toString())
                    }
                ))
            }
        }.flowOn(Dispatchers.IO)

    fun insertNote(notes: Notes): Flow<APIResponse<PostNoteData>> =
        flow {
            try {
                val response = makeAddNoteRequest(
                    notes.id, notes.notesTitle ?: "", notes.notesContent ?: "", notes.lastModified
                )

                if (response.isSuccessful) {
                    val postNoteResponse = Gson().fromJson(response.body()?.string(), PostNoteResponse::class.java)
                    emit(APIResponse.Success(postNoteResponse.postNoteData!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), PostNoteResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeAddNoteRequest(
                            notes.id, notes.notesTitle ?: "", notes.notesContent?: "", notes.lastModified
                        )

                        if (retryResponse.isSuccessful) {
                            val retryNoteResponse = Gson().fromJson(retryResponse.body()?.string(), PostNoteResponse::class.java)
                            emit(APIResponse.Success(retryNoteResponse.postNoteData!!))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), PostNoteResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.unable_add_note)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeAddNoteRequest(id: String, title: String, content: String, lastModified: Long): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.addNote(accessKey, PostNote(id, title, content, lastModified))
    }

    fun getAllNote(): Flow<APIResponse<List<NoteData>>> =
        flow {
            try {
                val response = makeGetAllNoteRequest()

                if (response.isSuccessful) {
                    val getNoteResponse = Gson().fromJson(response.body()?.string(), GetAllNotesRes::class.java)
                    emit(APIResponse.Success(getNoteResponse.dataNoteRes.noteList!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAllNotesRes::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeGetAllNoteRequest()

                        if (retryResponse.isSuccessful) {
                            val retryNoteResponse = Gson().fromJson(retryResponse.body()?.string(), GetAllNotesRes::class.java)
                            emit(APIResponse.Success(retryNoteResponse.dataNoteRes.noteList!!))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), GetAllNotesRes::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.get_notes_failed)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeGetAllNoteRequest(): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.getAllNote(accessKey)
    }

    fun updateNote(notes: Notes): Flow<APIResponse<String>> =
        flow {
            try {
                val response = makeUpdateNoteRequest(notes)

                if (response.isSuccessful) {
                    val updateResponse = Gson().fromJson(response.body()?.string(), PutNoteResponse::class.java)
                    emit(APIResponse.Success(updateResponse.message))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), PutNoteResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeUpdateNoteRequest(notes)

                        if (retryResponse.isSuccessful) {
                            val retryUpdateResponse = Gson().fromJson(retryResponse.body()?.string(), PutNoteResponse::class.java)
                            emit(APIResponse.Success(retryUpdateResponse.message))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), PutNoteResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.update_notes_failed)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeUpdateNoteRequest(notes: Notes): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        val putNote = PutNote(notes.notesTitle ?: "", notes.notesContent ?: "", notes.lastModified)
        return API.services.updateNote(notes.id, putNote, accessKey)
    }

    fun deleteNote(id: String): Flow<APIResponse<String>> =
        flow {
            try {
                val response = makeDeleteNoteRequest(id)

                if (response.isSuccessful) {
                    val deleteResponse = Gson().fromJson(response.body()?.string(), DeleteResponse::class.java)
                    emit(APIResponse.Success(deleteResponse.message))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), DeleteResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeDeleteNoteRequest(id)

                        if (retryResponse.isSuccessful) {
                            val retryDeleteResponse = Gson().fromJson(retryResponse.body()?.string(), DeleteResponse::class.java)
                            emit(APIResponse.Success(retryDeleteResponse.message))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), DeleteResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.delete_notes_failed)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeDeleteNoteRequest(id: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.deleteNote(id, accessKey)
    }

    fun logoutUser(refreshToken: String): Flow<APIResponse<String>> =
        flow {
            try {
                val response = API.services.logout(DeleteLogout(refreshToken))

                if (response.body() != null) {
                    val logoutResponse = Gson().fromJson(response.body()!!.string(), LogoutResponse::class.java)
                    emit(APIResponse.Success(logoutResponse.message))
                } else {
                    val logoutResponse = Gson().fromJson(response.errorBody()!!.string(), LogoutResponse::class.java)
                    emit(APIResponse.Error(logoutResponse.message))
                }
            } catch (e: Exception) {
                emit(APIResponse.Error(
                    buildString {
                        append(context.getString(R.string.login_failed))
                        append(e.toString())
                    }
                ))
            }
        }.flowOn(Dispatchers.IO)

    fun setTwoFactorAuth(id: String, pin: String): Flow<APIResponse<TwoFactorData>> =
        flow {
            try {
                val response = makeSetTwoFactorRequest(id, pin)

                if (response.isSuccessful) {
                    val twoFactorResponse = Gson().fromJson(response.body()?.string(), TwoFactorResponse::class.java)
                    emit(APIResponse.Success(twoFactorResponse.twoFactorData!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), TwoFactorResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeSetTwoFactorRequest(id, pin)

                        if (retryResponse.isSuccessful) {
                            val retryTwoFactorResponse = Gson().fromJson(retryResponse.body()?.string(), TwoFactorResponse::class.java)
                            emit(APIResponse.Success(retryTwoFactorResponse.twoFactorData!!))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), TwoFactorResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.set_two_factor_failed)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeSetTwoFactorRequest(id: String, pin: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.setTwoFactorAuth(PostLogin(id, pin, ""), accessKey)
    }

    fun disableTwoFactorAuth(id: String, pin: String): Flow<APIResponse<String>> =
        flow {
            try {
                val response = makeDisableTwoFactorRequest(id, pin)

                if (response.isSuccessful) {
                    val twoFactorResponse = Gson().fromJson(response.body()?.string(), TwoFactorResponse::class.java)
                    emit(APIResponse.Success(twoFactorResponse.message))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), TwoFactorResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeDisableTwoFactorRequest(id, pin)

                        if (retryResponse.isSuccessful) {
                            val retryTwoFactorResponse = Gson().fromJson(retryResponse.body()?.string(), TwoFactorResponse::class.java)
                            emit(APIResponse.Success(retryTwoFactorResponse.message))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), TwoFactorResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.disable_two_factor_failed)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeDisableTwoFactorRequest(id: String, pin: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.disableTwoFactorAuth(PostLogin(id, pin, ""), accessKey)
    }

    fun checkTwoFactorSet(id: String): Flow<APIResponse<Boolean>> =
        flow {
            try {
                val response = makeCheckTwoFactorRequest(id)

                if (response.isSuccessful) {
                    val twoFactorResponse = Gson().fromJson(response.body()?.string(), CheckTwoFAResponse::class.java)
                    emit(APIResponse.Success(twoFactorResponse.isEnabled!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), CheckTwoFAResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeCheckTwoFactorRequest(id)

                        if (retryResponse.isSuccessful) {
                            val retryTwoFactorResponse = Gson().fromJson(retryResponse.body()?.string(), CheckTwoFAResponse::class.java)
                            emit(APIResponse.Success(retryTwoFactorResponse.isEnabled!!))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), CheckTwoFAResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.check_two_factor_failed)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeCheckTwoFactorRequest(id: String): Response<ResponseBody> {
        return API.services.checkTwoFactorSet(PostLogin(id, "", ""), "")
    }

    fun checkIsRegistered(id: String): Flow<APIResponse<Boolean>> =
        flow {
            try {
                val response = API.services.checkRegistered(PostCheck(id))
                emit(APIResponse.Success(response.isRegistered))
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.check_two_factor_failed)}: $e"))
            }

        }.flowOn(Dispatchers.IO)

    private suspend fun refreshAccessKey() {
        val refreshKey = sharedPrefDataSource.getRefreshKey()!!
        val newAccessKey = API.services.updateAccessKey(PutUpdateKey(refreshKey))
        sharedPrefDataSource.setAccessKey(newAccessKey.updateKeyData.accessToken)
    }
}