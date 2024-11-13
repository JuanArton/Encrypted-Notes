package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import com.google.gson.Gson
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.attachments.getattachment.AttachmentData
import com.juanarton.encnotes.core.data.api.attachments.getattachment.GetAttachmentResponse
import com.juanarton.encnotes.core.data.api.authentications.updatekey.PutUpdateKey
import com.juanarton.encnotes.core.data.api.note.postAttachment.PostAttachmentData
import com.juanarton.encnotes.core.data.api.note.postAttachment.PostAttachmentResponse
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRemoteDataSource @Inject constructor(
    private val context: Context,
    private val sharedPrefDataSource: SharedPrefDataSource
){
    fun uploadImageAtt(image: ByteArray, notes: Notes): Flow<APIResponse<PostAttachmentData>> =
        flow {
            try {
                val response = makeUploadImageAttRequest(image, notes)

                if (response.isSuccessful) {
                    val postImgAttResponse = Gson().fromJson(response.body()?.string(), PostAttachmentResponse::class.java)
                    emit(APIResponse.Success(postImgAttResponse.postAttachmentData!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), PostAttachmentResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeUploadImageAttRequest(image, notes)

                        if (retryResponse.isSuccessful) {
                            val retryAttResponse = Gson().fromJson(retryResponse.body()?.string(), PostAttachmentResponse::class.java)
                            emit(APIResponse.Success(retryAttResponse.postAttachmentData!!))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), PostAttachmentResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.unable_add_attachment)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeUploadImageAttRequest(image: ByteArray, notes: Notes): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!

        val requestBody = image.toRequestBody("image/jpeg".toMediaType(), 0, image.size)
        val imagePart = MultipartBody.Part.createFormData("data", "encrypted_image.jpg", requestBody)

        val json = JSONObject()
            .apply {
                put("id", NanoId.generate(16))
                put("lastModified", notes.lastModified)
            }.toString().toRequestBody("application/json".toMediaType())

        return API.services.uploadImageAtt(notes.id, imagePart, json, accessKey)
    }

    fun getAttById(id: String): Flow<APIResponse<List<AttachmentData>>> =
        flow {
            try {
                val response = makeGetAttRequest(id)

                if (response.isSuccessful) {
                    val getAttResponse = Gson().fromJson(response.body()?.string(), GetAttachmentResponse::class.java)
                    emit(APIResponse.Success(getAttResponse.getAttachmentArray!!.attachmentData))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAttachmentResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeGetAttRequest(id)

                        if (retryResponse.isSuccessful) {
                            val retryAttResponse = Gson().fromJson(retryResponse.body()?.string(), GetAttachmentResponse::class.java)
                            emit(APIResponse.Success(retryAttResponse!!.getAttachmentArray!!.attachmentData))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), GetAttachmentResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.unable_get_attachment)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeGetAttRequest(id: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.getAttById(id, accessKey)
    }

    fun getAllAtt(): Flow<APIResponse<List<AttachmentData>>> =
        flow {
            try {
                val response = makeGetAllAttRequest()

                if (response.isSuccessful) {
                    val getAttResponse = Gson().fromJson(response.body()?.string(), GetAttachmentResponse::class.java)
                    emit(APIResponse.Success(getAttResponse.getAttachmentArray!!.attachmentData))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAttachmentResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeGetAllAttRequest()

                        if (retryResponse.isSuccessful) {
                            val retryAttResponse = Gson().fromJson(retryResponse.body()?.string(), GetAttachmentResponse::class.java)
                            emit(APIResponse.Success(retryAttResponse!!.getAttachmentArray!!.attachmentData))
                        } else {
                            val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), GetAttachmentResponse::class.java)
                            emit(APIResponse.Error(retryErrorResponse.message))
                        }
                    } else {
                        emit(APIResponse.Error(errorResponse.message))
                    }
                }
            } catch (e: Exception) {
                emit(APIResponse.Error("${context.getString(R.string.unable_get_attachment)}: $e"))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeGetAllAttRequest(): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.getAllAtt(accessKey)
    }

    private suspend fun refreshAccessKey() {
        val refreshKey = sharedPrefDataSource.getRefreshKey()!!
        val newAccessKey = API.services.updateAccessKey(PutUpdateKey(refreshKey))
        sharedPrefDataSource.setAccessKey(newAccessKey.updateKeyData.accessToken)
    }
}