package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.ProgressListener
import com.juanarton.encnotes.core.data.api.attachments.getattachment.AttachmentData
import com.juanarton.encnotes.core.data.api.attachments.getattachment.GetAttachmentResponse
import com.juanarton.encnotes.core.data.api.authentications.updatekey.PutUpdateKey
import com.juanarton.encnotes.core.data.api.download.ProgressResponseBody
import com.juanarton.encnotes.core.data.api.note.postAttachment.PostAttachmentData
import com.juanarton.encnotes.core.data.api.note.postAttachment.PostAttachmentResponse
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import com.ketch.Ketch
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AttachmentRemoteDataSource @Inject constructor(
    private val context: Context,
    private val sharedPrefDataSource: SharedPrefDataSource
){
    fun uploadImageAtt(
        image: ByteArray, attachment: Attachment
    ): Flow<APIResponse<PostAttachmentData>> =
        flow {
            try {
                val response = makeUploadImageAttRequest(image, attachment)

                if (response.isSuccessful) {
                    val postImgAttResponse = Gson().fromJson(response.body()?.string(), PostAttachmentResponse::class.java)
                    emit(APIResponse.Success(postImgAttResponse.postAttachmentData!!))
                } else {
                    val errorResponse = Gson().fromJson(response.errorBody()?.string(), PostAttachmentResponse::class.java)

                    if (errorResponse.message == "Token maximum age exceeded") {
                        refreshAccessKey()
                        val retryResponse = makeUploadImageAttRequest(image, attachment)

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

    private suspend fun makeUploadImageAttRequest(
        image: ByteArray, attachment: Attachment
    ): Response<ResponseBody> {
        val accessKey = sharedPrefDataSource.getAccessKey()!!

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(image)

        val hash = digest.digest().joinToString("") { "%02x".format(it) }

        val requestBody = image.toRequestBody("image/jpeg".toMediaType(), 0, image.size)
        val imagePart = MultipartBody.Part.createFormData("data", attachment.url, requestBody)

        val json = JSONObject()
            .apply {
                put("id", attachment.id)
                put("lastModified", attachment.lastModified)
                put("hash", hash)
            }.toString().toRequestBody("application/json".toMediaType())

        return API.services.uploadImageAtt(attachment.noteId!!, imagePart, json, accessKey)
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

    suspend fun downloadAttachment(url: String, ketch: Ketch): Int {
        val fileName = "/" + url.substringAfterLast("/")
        val path = context.filesDir.toString()
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

    fun downloadAttachment1(url: String): Flow<APIResponse<Int>> {
        synchronized(API.activeDownloads) {
            if (API.activeDownloads.containsKey(url)) {
                return API.activeDownloads[url]!!
            }

            val downloadStateFlow = MutableStateFlow<APIResponse<Int>>(APIResponse.Success(0))
            API.activeDownloads[url] = downloadStateFlow

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    API.progressListener = object : ProgressListener {
                        override fun onProgress(
                            bytesRead: Long,
                            contentLength: Long,
                            done: Boolean
                        ) {
                            val progress = if (contentLength > 0) {
                                ((bytesRead * 100) / contentLength).toInt()
                            } else 0
                            launch {
                                if (progress == 100) {
                                    downloadStateFlow.emit(APIResponse.Success(progress))
                                }
                            }
                        }
                    }

                    val responseBody = API.services.downloadAtt(url)

                    responseBody.byteStream().use { input ->
                        val fileName = url.substringAfterLast("/")
                        val file = File(context.filesDir, fileName)

                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }

                    delay(250)
                    downloadStateFlow.emit(APIResponse.Success(100))
                } catch (e: Exception) {
                    downloadStateFlow.emit(APIResponse.Error("Download failed: ${e.message}"))
                } finally {
                    synchronized(API.activeDownloads) {
                        API.activeDownloads.remove(url)
                    }
                }
            }

            return downloadStateFlow
        }
    }
}