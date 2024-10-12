package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.user.LoginData
import com.juanarton.encnotes.core.data.api.user.PostLogin
import com.juanarton.encnotes.core.data.api.user.PostRegister
import com.juanarton.encnotes.core.data.api.user.RegisterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteDataSource @Inject constructor(
    private val context: Context,
){
    fun registerUser(id: String, pin: String, username: String): Flow<APIResponse<RegisterData>> =
        flow {
            try {
                val result = API.services.register(
                    PostRegister(id, pin, username)
                )
                if (result.status == "success") {
                    emit(APIResponse.Success(result.registerData))
                } else {
                    emit(APIResponse.Error(result.message))
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
                val result = API.services.login(
                    PostLogin(id, pin)
                )
                if (result.status == "success") {
                    emit(APIResponse.Success(result.loginData))
                } else {
                    emit(APIResponse.Error(result.message))
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
}