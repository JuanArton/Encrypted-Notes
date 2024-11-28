package com.juanarton.encnotes.core.data.source.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefDataSource @Inject constructor(
    context: Context
) {
    companion object {
        const val FILE_NAME = "AppState"
        const val IS_LOGGED_IN = "isLoggedIn"
        const val IS_GUEST = "isGuest"
        const val REFRESH_KEY = "refreshKey"
        const val ACCESS_KEY = "accessKey"
        const val CIPHER_KEY = "cipherKey"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setIsLoggedIn(isLoggedIn: Boolean) =
        sharedPreferences.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).commit()

    fun getIsLoggedIn() = sharedPreferences.getBoolean(IS_LOGGED_IN, false)

    fun setIsGuest(isGuest: Boolean) =
        sharedPreferences.edit().putBoolean(IS_GUEST, isGuest).commit()

    fun getIsGuest() = sharedPreferences.getBoolean(IS_GUEST, false)

    fun setRefreshKey(refreshKey: String) =
        sharedPreferences.edit().putString(REFRESH_KEY, refreshKey).commit()

    fun getRefreshKey() = sharedPreferences.getString(REFRESH_KEY, null)

    fun setAccessKey(accessKey: String) =
        sharedPreferences.edit().putString(
            ACCESS_KEY, buildString{
                append("Bearer ")
                append(accessKey)
            }
        ).commit()

    fun getAccessKey() = sharedPreferences.getString(ACCESS_KEY, null)

    fun setCipherKey(cipherKey: String) =
        sharedPreferences.edit().putString(CIPHER_KEY, cipherKey).commit()

    fun getCipherKey() = sharedPreferences.getString(CIPHER_KEY, null)

    fun clearSharedPreference() = sharedPreferences.edit().clear().apply()
}