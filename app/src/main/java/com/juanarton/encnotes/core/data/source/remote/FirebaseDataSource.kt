package com.juanarton.encnotes.core.data.source.remote

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor(
    private val context: Context
): NetworkCallTool(context) {
    fun signInWithGoogle(option: GetSignInWithGoogleOption, activity: Activity): Flow<APIResponse<LoggedUser>> = flow {
        val credentialManager = CredentialManager.create(activity)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        try {
            val result = credentialManager.getCredential(request = request, context = activity)

            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idTokenString = googleIdTokenCredential.idToken

                        val auth = GoogleAuthProvider.getCredential(idTokenString, null)
                        val authResult = Firebase.auth.signInWithCredential(auth).await()

                        authResult?.user?.let {
                            val user = LoggedUser(
                                authResult.user!!.uid,
                                authResult.user!!.displayName,
                                authResult.user!!.photoUrl.toString()
                            )
                            emit(APIResponse.Success(user))
                        } ?: run {
                            emit(APIResponse.Error(activity.getString(R.string.login_failed)))
                        }
                    } else {
                        val message = buildString {
                            append(activity.getString(R.string.unexpected_type_of_credential))
                        }
                        emit(APIResponse.Error(message))
                    }
                }
            }
        } catch (e: Exception) {
            val message = buildString {
                append(context.getString(R.string.login_failed))
                append(" : $e")
            }
            emit(APIResponse.Error(message))
        }
    }.flowOn(Dispatchers.IO)

    fun loginByEmail(email: String, password: String): Flow<APIResponse<LoggedUser>> =
        handleNetworkCall {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(email, password).await()

            result?.user?.let {
                auth.currentUser?.let {
                    if (it.isEmailVerified) {
                        LoggedUser(
                            it.uid,
                            it.displayName,
                            it.photoUrl.toString()
                        )
                    } else {
                        throw Exception(context.getString(R.string.please_verify_email))
                    }
                }
            } ?: run {
                throw Exception(context.getString(R.string.login_failed))
            }
        }

    fun signInByEmail(email: String, password: String): Flow<APIResponse<LoggedUser>> =
        handleNetworkCall {
            val auth = FirebaseAuth.getInstance()

            val result = auth.createUserWithEmailAndPassword(email, password).await()

            result.user?.let { user ->
                try {
                    user.sendEmailVerification().await()
                    LoggedUser(
                        user.uid,
                        user.displayName,
                        user.photoUrl.toString()
                    )
                } catch (verificationException: Exception) {
                    val message = buildString {
                        append(context.getString(R.string.failed_to_send_verification_email))
                        append(verificationException.message)
                    }
                    throw Exception(message)
                }
            } ?: run {
                throw Exception(context.getString(R.string.user_creation_failed))
            }
        }
}