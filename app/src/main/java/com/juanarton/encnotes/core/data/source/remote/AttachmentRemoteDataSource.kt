package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import com.google.gson.Gson
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.attachments.getattachment.AttachmentData
import com.juanarton.encnotes.core.data.api.attachments.getattachment.GetAttachmentResponse
import com.juanarton.encnotes.core.data.api.authentications.updatekey.PutUpdateKey
import com.juanarton.encnotes.core.data.api.note.deleteNote.DeleteResponse
import com.juanarton.encnotes.core.data.api.note.postAttachment.PostAttachmentData
import com.juanarton.encnotes.core.data.api.note.postAttachment.PostAttachmentResponse
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import com.juanarton.encnotes.core.data.source.remote.NoteRemoteDataSource.Companion.TOKEN_EXPIRED_MESSAGE
import com.ketch.Ketch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
): NetworkCallTool(context){
    fun uploadImageAtt(
        image: ByteArray, attachment: Attachment
    ): Flow<APIResponse<PostAttachmentData>> =
        handleNetworkCall {
            val response = makeUploadImageAttRequest(image, attachment)

            if (response.isSuccessful) {
                val postImgAttResponse = Gson().fromJson(response.body()?.string(), PostAttachmentResponse::class.java)
                postImgAttResponse.postAttachmentData!!
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), PostAttachmentResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeUploadImageAttRequest(image, attachment)

                    if (retryResponse.isSuccessful) {
                        val retryAttResponse = Gson().fromJson(retryResponse.body()?.string(), PostAttachmentResponse::class.java)
                        retryAttResponse.postAttachmentData!!
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), PostAttachmentResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeUploadImageAttRequest(
        image: ByteArray, attachment: Attachment
    ): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!

        val requestBody = image.toRequestBody("image/jpeg".toMediaType(), 0, image.size)
        val imagePart = MultipartBody.Part.createFormData("data", attachment.url, requestBody)

        val json = JSONObject()
            .apply {
                put("id", attachment.id)
                put("lastModified", attachment.lastModified)
                put("isDelete", attachment.isDelete)
                put("type", attachment.type)
            }.toString().toRequestBody("application/json".toMediaType())

        return API.services.uploadImageAtt(attachment.noteId!!, imagePart, json, accessKey)
    }

    fun getAttById(id: String): Flow<APIResponse<List<AttachmentData>>> =
        handleNetworkCall {
            val response = makeGetAttRequest(id)

            if (response.isSuccessful) {
                val getAttResponse = Gson().fromJson(response.body()?.string(), GetAttachmentResponse::class.java)
                getAttResponse.getAttachmentArray!!.attachmentData
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAttachmentResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeGetAttRequest(id)

                    if (retryResponse.isSuccessful) {
                        val retryAttResponse = Gson().fromJson(retryResponse.body()?.string(), GetAttachmentResponse::class.java)
                        retryAttResponse!!.getAttachmentArray!!.attachmentData
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), GetAttachmentResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeGetAttRequest(id: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.getAttById(id, accessKey)
    }

    fun getAllAtt(): Flow<APIResponse<List<AttachmentData>>> =
        handleNetworkCall {
            val response = makeGetAllAttRequest()

            if (response.isSuccessful) {
                val getAttResponse = Gson().fromJson(response.body()?.string(), GetAttachmentResponse::class.java)
                getAttResponse.getAttachmentArray!!.attachmentData
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), GetAttachmentResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeGetAllAttRequest()

                    if (retryResponse.isSuccessful) {
                        val retryAttResponse = Gson().fromJson(retryResponse.body()?.string(), GetAttachmentResponse::class.java)
                        retryAttResponse!!.getAttachmentArray!!.attachmentData
                    } else {
                        val retryErrorResponse = Gson().fromJson(retryResponse.errorBody()?.string(), GetAttachmentResponse::class.java)
                        throw Exception(retryErrorResponse.message)
                    }
                } else {
                    throw Exception(errorResponse.message)
                }
            }
        }

    private suspend fun makeGetAllAttRequest(): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.getAllAtt(accessKey)
    }

    suspend fun downloadAttachment(url: String, ketch: Ketch): Int {
        val fileName = "/" + url.substringAfterLast("/")
        val path = context.filesDir.toString()+"/images"
        val downloads = ketch.observeDownloads().first()
        val index = downloads.indexOfFirst { it.url == url }

        if (index == -1) {
            return ketch.download(url, path, fileName)
        } else if (downloads[index].status.name != "SUCCESS") {
            ketch.clearDb(downloads[index].id, true)
            return ketch.download(url, path, fileName)
        } else {
            ketch.clearDb(downloads[index].id, true)
            return ketch.download(url, path, fileName)
        }
    }

    fun deleteAttById(id: String): Flow<APIResponse<String>> =
        handleNetworkCall {
            val response = makeDeleteAttById(id)

            if (response.isSuccessful) {
                val deleteAttResponse = Gson().fromJson(response.body()?.string(), DeleteResponse::class.java)
                deleteAttResponse.message
            } else {
                val errorResponse = Gson().fromJson(response.errorBody()?.string(), DeleteResponse::class.java)

                if (errorResponse.message == TOKEN_EXPIRED_MESSAGE) {
                    refreshAccessKey()
                    val retryResponse = makeDeleteAttById(id)

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

    private suspend fun makeDeleteAttById(id: String): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!
        return API.services.deleteAttachment(id, accessKey)
    }

    private suspend fun refreshAccessKey() {
        val refreshKey = sharedPrefDataSource.getRefreshKey()!!
        val newAccessKey = API.services.updateAccessKey(PutUpdateKey(refreshKey))
        sharedPrefDataSource.setAccessKey(newAccessKey.updateKeyData.accessToken)
    }
}