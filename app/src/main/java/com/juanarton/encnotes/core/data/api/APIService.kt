package com.juanarton.encnotes.core.data.api

import com.juanarton.encnotes.core.data.api.authentications.login.PostLogin
import com.juanarton.encnotes.core.data.api.authentications.updatekey.PutUpdateKey
import com.juanarton.encnotes.core.data.api.authentications.updatekey.UpdateKeyResponse
import com.juanarton.encnotes.core.data.api.note.addnote.PostNote
import com.juanarton.encnotes.core.data.api.note.updateNote.PutNote
import com.juanarton.encnotes.core.data.api.user.register.PostRegister
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface APIService {
    @POST("users")
    suspend fun register(
        @Body postRegister: PostRegister
    ): Response<ResponseBody>

    @POST("authentications")
    suspend fun login(
        @Body postLogin: PostLogin
    ): Response<ResponseBody>

    @PUT("authentications")
    suspend fun updateAccessKey(
        @Body putUpdateKey: PutUpdateKey
    ): UpdateKeyResponse

    @POST("note")
    suspend fun addNote(
        @Header("Authorization") accessToken: String,
        @Body postNote: PostNote
    ): Response<ResponseBody>

    @GET("notes/collection")
    suspend fun getAllNote(
        @Header("Authorization") accessToken: String,
    ): Response<ResponseBody>

    @PUT("note")
    suspend fun updateNote(
        @Query("id") id: String,
        @Body putNote: PutNote,
        @Header("Authorization") accessToken: String
    ): Response<ResponseBody>

    @DELETE("note")
    suspend fun deleteNote(
        @Query("id") id: String,
        @Header("Authorization") accessToken: String,
    ): Response<ResponseBody>

    @Multipart
    @POST("upload/images")
    suspend fun uploadImageAtt(
        @Query("id") id: String,
        @Part image: MultipartBody.Part,
        @Part("Attachment") putAttachment: RequestBody,
        @Header("Authorization") accessToken: String
    ): Response<ResponseBody>

    @GET("attachment")
    suspend fun getAttById(
        @Query("id") id: String,
        @Header("Authorization") accessToken: String,
    ): Response<ResponseBody>

    @GET("attachment/collections")
    suspend fun getAllAtt(
        @Header("Authorization") accessToken: String,
    ): Response<ResponseBody>
}