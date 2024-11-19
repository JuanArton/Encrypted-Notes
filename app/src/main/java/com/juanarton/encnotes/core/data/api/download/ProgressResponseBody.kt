package com.juanarton.encnotes.core.data.api.download

import com.juanarton.encnotes.core.data.api.ProgressListener
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: ProgressListener
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source = object : ForwardingSource(source) {
        var totalBytesRead = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) {
                totalBytesRead += bytesRead
                progressListener.onProgress(totalBytesRead, responseBody.contentLength(), bytesRead == -1L)
            }
            return bytesRead
        }
    }
}


private fun BufferedSource.setListener(listener: (bytesRead: Long) -> Unit): BufferedSource {
    return object : ForwardingSource(this) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) listener(bytesRead)
            return bytesRead
        }
    }.buffer()
}


