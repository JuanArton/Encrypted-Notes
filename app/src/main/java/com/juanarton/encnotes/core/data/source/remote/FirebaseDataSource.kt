package com.juanarton.encnotes.core.data.source.remote

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.domain.LoggedUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor() {
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
                        try {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            val idTokenString = googleIdTokenCredential.idToken

                            val auth = GoogleAuthProvider.getCredential(idTokenString, null)

                            val task = FirebaseAuth.getInstance().signInWithCredential(auth)

                            val authResult = Tasks.await(task)

                            if (authResult.user != null) {
                                val user = LoggedUser(
                                    authResult.user!!.uid,
                                    authResult.user!!.displayName,
                                    authResult.user!!.photoUrl.toString()
                                )
                                emit(APIResponse.Success(user))
                            } else {
                                emit(APIResponse.Error(activity.getString(R.string.login_gagal)))
                            }
                        } catch (e: Exception) {
                            val message = buildString {
                                append(activity.getString(R.string.login_gagal))
                                append(" : $e")
                            }
                            emit(APIResponse.Error(message))
                        }
                    } else {
                        emit(APIResponse.Error(activity.getString(R.string.unexpected_type_of_credential)))
                    }
                }
            }
        } catch (e: GetCredentialException) {
            val message = buildString {
                append(activity.getString(R.string.failed_to_get_credential))
                append(" : $e")
            }
            emit(APIResponse.Error(message))
        }
    }
}