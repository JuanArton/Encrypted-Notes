package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import com.google.gson.Gson
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.authentications.login.LoginData
import com.juanarton.encnotes.core.data.api.authentications.login.LoginResponse
import com.juanarton.encnotes.core.data.api.authentications.login.PostLogin
import com.juanarton.encnotes.core.data.api.authentications.updatekey.PutUpdateKey
import com.juanarton.encnotes.core.data.api.authentications.updatekey.UpdateKeyData
import com.juanarton.encnotes.core.data.api.authentications.updatekey.UpdateKeyResponse
import com.juanarton.encnotes.core.data.api.note.addnote.PostNote
import com.juanarton.encnotes.core.data.api.note.addnote.PostNoteData
import com.juanarton.encnotes.core.data.api.note.addnote.PostNoteResponse
import com.juanarton.encnotes.core.data.api.note.getallnote.GetAllNotesRes
import com.juanarton.encnotes.core.data.api.note.getallnote.NoteData
import com.juanarton.encnotes.core.data.api.user.register.PostRegister
import com.juanarton.encnotes.core.data.api.user.register.RegisterData
import com.juanarton.encnotes.core.data.api.user.register.RegisterResponse
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
class RemoteDataSource @Inject constructor(
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


    fun loginUser(id: String, pin: String): Flow<APIResponse<LoginData>> =
        flow {
            try {
                val response = API.services.login(PostLogin(id, pin))

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

    fun addNote(id: String, title: String, content: String, lastModified: Long): Flow<APIResponse<PostNoteData>> =
        flow {
            try {
                val response = makeAddNoteRequest(id, title, content, lastModified)

                if (response.isSuccessful) {
                    val postNoteResponse = Gson().fromJson(response.body()?.string(), PostNoteResponse::class.java)
                    emit(APIResponse.Success(postNoteResponse.postNoteData!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), PostNoteResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeAddNoteRequest(id, title, content, lastModified)

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
                    emit(APIResponse.Success(getNoteResponse.noteList!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAllNotesRes::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeGetAllNoteRequest()

                        if (retryResponse.isSuccessful) {
                            val retryNoteResponse = Gson().fromJson(retryResponse.body()?.string(), GetAllNotesRes::class.java)
                            emit(APIResponse.Success(retryNoteResponse.noteList!!))
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

    private suspend fun refreshAccessKey() {
        val refreshKey = sharedPrefDataSource.getRefreshKey()!!
        val newAccessKey = API.services.updateAccessKey(PutUpdateKey(refreshKey))
        sharedPrefDataSource.setAccessKey(newAccessKey.updateKeyData.accessToken)
    }
}