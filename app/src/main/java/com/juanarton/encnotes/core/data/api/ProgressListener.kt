package com.juanarton.encnotes.core.data.api

interface ProgressListener {
    fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean)
}