package com.juanarton.encnotes.core.data.source.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.user.LoginData
import com.juanarton.encnotes.core.data.api.user.LoginResponse
import com.juanarton.encnotes.core.data.api.user.PostLogin
import com.juanarton.encnotes.core.data.api.user.PostRegister
import com.juanarton.encnotes.core.data.api.user.RegisterData
import com.juanarton.encnotes.core.data.api.user.RegisterResponse
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
                val response = API.services.register(PostRegister(id, pin, username))

                if (response.body() != null) {
                    val registerResponse = Gson().fromJson(response.body()!!.string(), RegisterResponse::class.java)
                    emit(APIResponse.Success(registerResponse.registerData))
                } else {
                    val registerResponse = Gson().fromJson(response.errorBody()!!.string(), RegisterResponse::class.java)
                    emit(APIResponse.Error(registerResponse.message))
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
                val response = API.services.login(PostLogin(id, pin))

                if (response.body() != null) {
                    val loginResponse = Gson().fromJson(response.body()!!.string(), LoginResponse::class.java)
                    emit(APIResponse.Success(loginResponse.loginData))
                } else {
                    val loginResponse = Gson().fromJson(response.errorBody()!!.string(), LoginResponse::class.java)
                    emit(APIResponse.Error(loginResponse.message))
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