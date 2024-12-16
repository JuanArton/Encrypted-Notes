package com.juanarton.encnotes.core.data.source.remote

import android.util.Log
import com.juanarton.encnotes.core.data.api.APIResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

abstract class NetworkBoundRes<ResultType, RequestType> {

    private val result: Flow<Resource<ResultType>> =
        flow {
            emit(Resource.Loading())
            when(val apiResponse = createCall().first()){
                is APIResponse.Success -> {
                    emitAll(loadFromNetwork(apiResponse.data).map {
                        Resource.Success(it)
                    })
                }

                is APIResponse.Error -> {
                    emit(Resource.Error(apiResponse.errorMessage))
                }
            }
        }

    protected abstract suspend fun createCall(): Flow<APIResponse<RequestType>>

    protected abstract fun loadFromNetwork(data: RequestType): Flow<ResultType>

    fun asFlow(): Flow<Resource<ResultType>> = result
}