package com.juanarton.encnotes.core.data.api

sealed class APIResponse<out T> {
    data class Success<out T> (val data: T): APIResponse<T>()
    data class Error(val errorMessage: String?): APIResponse<Nothing>()
}
