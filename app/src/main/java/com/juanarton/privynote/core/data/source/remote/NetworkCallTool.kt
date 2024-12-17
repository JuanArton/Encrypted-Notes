package com.juanarton.privynote.core.data.source.remote

import android.annotation.SuppressLint
import android.content.Context
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.api.APIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class NetworkCallTool (
    private val context: Context,
) {

    @SuppressLint("StringFormatInvalid")
    protected fun <T> handleNetworkCall(call: suspend () -> Any): Flow<APIResponse<T>> = flow {
        try {
            emit(APIResponse.Success(call()))
        } catch (_: UnknownHostException) {
            emit(APIResponse.Error(context.getString(R.string.cant_reach_server)))
        } catch (_: SocketTimeoutException) {
            emit(APIResponse.Error(context.getString(R.string.request_timeout)))
        } catch (_: IOException) {
            emit(APIResponse.Error(context.getString(R.string.network_error)))
        } catch (e: Exception) {
            val errorMessage = e.toString()
            val regex = ":\\s*(.*)".toRegex()
            val matchResult = regex.find(errorMessage)
            val realError = matchResult?.groupValues?.get(1) ?: context.getString(R.string.unknown_error)
            emit(APIResponse.Error(realError))
        }
    }.flowOn(Dispatchers.IO) as Flow<APIResponse<T>>
}