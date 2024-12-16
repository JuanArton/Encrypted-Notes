package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRemoteDataSource @Inject constructor(
    private val context: Context,
    private val sharedPrefDataSource: SharedPrefDataSource
): NetworkCallTool(context) {

    companion object {
        const val TOKEN_EXPIRED_MESSAGE = "Token maximum age exceeded"
        const val PROCEED_TO_OTP_MEESAGE = "Proceed to OTP"
    }

    fun registerUser(id: String, pin: String, username: String): Flow<APIResponse<RegisterData>> =
        handleNetworkCall {
            val response = API.services.register(PostRegister(id, pin, username))

            if (response.body() != null) {
                val registerResponse = Gson().fromJson(response.body()!!.string(), RegisterResponse::class.java)
                registerResponse.registerData
            } else {
                val registerResponse = Gson().fromJson(response.errorBody()!!.string(), RegisterResponse::class.java)
                throw Exception(registerResponse.message)
            }
        }

    fun loginUser(id: String, pin: String, otp: String): Flow<APIResponse<LoginData>> =
        handleNetworkCall {
            val response = API.services.login(PostLogin(id, pin, otp))

            if (response.body() != null) {
                val loginResponse = Gson().fromJson(response.body()!!.string(), LoginResponse::class.java)
                if (loginResponse.message == PROCEED_TO_OTP_MEESAGE) {
                    throw Exception(PROCEED_TO_OTP_MEESAGE)
                } else {
                    loginResponse.loginData!!
                }
            } else {
                val loginResponse = Gson().fromJson(response.errorBody()!!.string(), LoginResponse::class.java)
                throw Exception(loginResponse.message)
            }
        }

    fun twoFactorAuth(id: String, pin: String, otp: String): Flow<APIResponse<LoginData>> =
        handleNetworkCall {
            val twoFA = API.services.twoFactorAuth(PostLogin(id, pin, otp))

            if (twoFA.body() != null) {
                val twoFAResponse = Gson().fromJson(twoFA.body()!!.string(), LoginResponse::class.java)
                twoFAResponse.loginData!!
            } else {
                val twoFAResponse = Gson().fromJson(twoFA.errorBody()!!.string(), LoginResponse::class.java)
                throw Exception(twoFAResponse.message)
            }
        }

    fun insertNote(notes: Notes): Flow<APIResponse<PostNoteData>> =
        handleNetworkCall {
            val response = makeAddNoteRequest(
                notes.id, notes.notesTitle ?: "", notes.notesContent ?: "", notes.lastModified
            )

            if (response.isSuccessful) {
                val postNoteResponse = Gson().fromJson(response.body()?.string(), PostNoteResponse::class.java)
                postNoteResponse.postNoteData!!
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), PostNoteResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeAddNoteRequest(
                        notes.id, notes.notesTitle ?: "", notes.notesContent?: "", notes.lastModified
                    )

                    if (retryResponse.isSuccessful) {
                        val retryNoteResponse = Gson().fromJson(retryResponse.body()?.string(), PostNoteResponse::class.java)
                        retryNoteResponse.postNoteData!!
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), PostNoteResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeAddNoteRequest(id: String, title: String, content: String, lastModified: Long): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.addNote(accessKey, PostNote(id, title, content, lastModified))
    }

    fun getAllNote(): Flow<APIResponse<List<NoteData>>> =
        handleNetworkCall {
            val response = makeGetAllNoteRequest()

            if (response.isSuccessful) {
                val getNoteResponse = Gson().fromJson(response.body()?.string(), GetAllNotesRes::class.java)
                getNoteResponse.dataNoteRes.noteList!!
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAllNotesRes::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeGetAllNoteRequest()

                    if (retryResponse.isSuccessful) {
                        val retryNoteResponse = Gson().fromJson(retryResponse.body()?.string(), GetAllNotesRes::class.java)
                        retryNoteResponse.dataNoteRes.noteList!!
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), GetAllNotesRes::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeGetAllNoteRequest(): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.getAllNote(accessKey)
    }

    fun updateNote(notes: Notes): Flow<APIResponse<String>> =
        handleNetworkCall {
            val response = makeUpdateNoteRequest(notes)

            if (response.isSuccessful) {
                val updateResponse = Gson().fromJson(response.body()?.string(), PutNoteResponse::class.java)
                updateResponse.message
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), PutNoteResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeUpdateNoteRequest(notes)

                    if (retryResponse.isSuccessful) {
                        val retryUpdateResponse = Gson().fromJson(retryResponse.body()?.string(), PutNoteResponse::class.java)
                        retryUpdateResponse.message
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), PutNoteResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeUpdateNoteRequest(notes: Notes): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        val putNote = PutNote(notes.notesTitle ?: "", notes.notesContent ?: "", notes.lastModified)
        return API.services.updateNote(notes.id, putNote, accessKey)
    }

    fun deleteNote(id: String): Flow<APIResponse<String>> =
        handleNetworkCall {
            val response = makeDeleteNoteRequest(id)

            if (response.isSuccessful) {
                val deleteResponse = Gson().fromJson(response.body()?.string(), DeleteResponse::class.java)
                deleteResponse.message
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), DeleteResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeDeleteNoteRequest(id)

                    if (retryResponse.isSuccessful) {
                        val retryDeleteResponse = Gson().fromJson(retryResponse.body()?.string(), DeleteResponse::class.java)
                        retryDeleteResponse.message
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), DeleteResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeDeleteNoteRequest(id: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.deleteNote(id, accessKey)
    }

    fun logoutUser(refreshToken: String): Flow<APIResponse<String>> =
        handleNetworkCall {
            val response = API.services.logout(DeleteLogout(refreshToken))

            if (response.body() != null) {
                val logoutResponse = Gson().fromJson(response.body()!!.string(), LogoutResponse::class.java)
                logoutResponse.message
            } else {
                val logoutResponse = Gson().fromJson(response.errorBody()!!.string(), LogoutResponse::class.java)
                throw Exception(logoutResponse.message)
            }
        }

    fun setTwoFactorAuth(id: String, pin: String): Flow<APIResponse<TwoFactorData>> =
        handleNetworkCall {
            val response = makeSetTwoFactorRequest(id, pin)

            if (response.isSuccessful) {
                val twoFactorResponse = Gson().fromJson(response.body()?.string(), TwoFactorResponse::class.java)
                twoFactorResponse.twoFactorData!!
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), TwoFactorResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeSetTwoFactorRequest(id, pin)

                    if (retryResponse.isSuccessful) {
                        val retryTwoFactorResponse = Gson().fromJson(retryResponse.body()?.string(), TwoFactorResponse::class.java)
                        retryTwoFactorResponse.twoFactorData!!
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), TwoFactorResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeSetTwoFactorRequest(id: String, pin: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.setTwoFactorAuth(PostLogin(id, pin, ""), accessKey)
    }

    fun disableTwoFactorAuth(id: String, pin: String): Flow<APIResponse<String>> =
        handleNetworkCall {
            val response = makeDisableTwoFactorRequest(id, pin)

            if (response.isSuccessful) {
                val twoFactorResponse = Gson().fromJson(response.body()?.string(), TwoFactorResponse::class.java)
                twoFactorResponse.message
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), TwoFactorResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeDisableTwoFactorRequest(id, pin)

                    if (retryResponse.isSuccessful) {
                        val retryTwoFactorResponse = Gson().fromJson(retryResponse.body()?.string(), TwoFactorResponse::class.java)
                        retryTwoFactorResponse.message
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), TwoFactorResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeDisableTwoFactorRequest(id: String, pin: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.disableTwoFactorAuth(PostLogin(id, pin, ""), accessKey)
    }

    fun checkTwoFactorSet(id: String): Flow<APIResponse<Boolean>> =
        handleNetworkCall {
            val response = makeCheckTwoFactorRequest(id)

            if (response.isSuccessful) {
                val twoFactorResponse = Gson().fromJson(response.body()?.string(), CheckTwoFAResponse::class.java)
                twoFactorResponse.isEnabled!!
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), CheckTwoFAResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeCheckTwoFactorRequest(id)

                    if (retryResponse.isSuccessful) {
                        val retryTwoFactorResponse = Gson().fromJson(retryResponse.body()?.string(), CheckTwoFAResponse::class.java)
                        retryTwoFactorResponse.isEnabled!!
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), CheckTwoFAResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeCheckTwoFactorRequest(id: String): Response<ResponseBody> {
        return API.services.checkTwoFactorSet(PostLogin(id, "", ""), "")
    }

    fun checkIsRegistered(id: String): Flow<APIResponse<Boolean>> =
        flow {
            try {
                val response = API.services.checkRegistered(PostCheck(id))
                emit(APIResponse.Success(response.isRegistered))
            } catch (_: UnknownHostException) {
                emit(APIResponse.Error(context.getString(R.string.cant_reach_server)))
            } catch (_: SocketTimeoutException) {
                emit(APIResponse.Error(context.getString(R.string.request_timeout)))
            } catch (_: IOException) {
                emit(APIResponse.Error(context.getString(R.string.network_error)))
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: e.message
                val regex = ":\\s*(.*)".toRegex()
                val matchResult = regex.find(errorMessage.toString())
                val realError = matchResult?.groupValues?.get(1) ?: context.getString(R.string.unknown_error)
                emit(APIResponse.Error(realError))
            }

        }.flowOn(Dispatchers.IO)

    fun deleteAllNote(): Flow<APIResponse<String>> =
        handleNetworkCall {
            val response = makeDeleteAllNoteRequest()

            if (response.isSuccessful) {
                val deleteResponse = Gson().fromJson(response.body()?.string(), DeleteResponse::class.java)
                deleteResponse.message
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), DeleteResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeDeleteAllNoteRequest()

                    if (retryResponse.isSuccessful) {
                        val retryDeleteResponse = Gson().fromJson(retryResponse.body()?.string(), DeleteResponse::class.java)
                        retryDeleteResponse.message
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), DeleteResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeDeleteAllNoteRequest(): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.deleteAllNotes(accessKey)
    }

    private suspend fun refreshAccessKey() {
        val refreshKey = sharedPrefDataSource.getRefreshKey()!!
        val newAccessKey = API.services.updateAccessKey(PutUpdateKey(refreshKey))
        sharedPrefDataSource.setAccessKey(newAccessKey.updateKeyData.accessToken)
    }
}