package com.juanarton.privynote.ui.activity.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.crypto.tink.KeysetHandle
import com.juanarton.privynote.core.data.domain.model.TwoFactor
import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.privynote.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
) : ViewModel() {

    private val _logout: MutableLiveData<Resource<String>> = MutableLiveData()
    val logout: LiveData<Resource<String>> = _logout

    private val _enableTwoFactor: MutableLiveData<Resource<TwoFactor>> = MutableLiveData()
    val enableTwoFactor: LiveData<Resource<TwoFactor>> = _enableTwoFactor

    private val _disableTwoFactor: MutableLiveData<Resource<String>> = MutableLiveData()
    val disableTwoFactor: LiveData<Resource<String>> = _disableTwoFactor

    private val _checkTwoFactor: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    val checkTwoFactor: LiveData<Resource<Boolean>> = _checkTwoFactor

    private val _backupNotes: MutableLiveData<Boolean> = MutableLiveData()
    val backupNotes: LiveData<Boolean> = _backupNotes

    private val _restoreNotes: MutableLiveData<Boolean> = MutableLiveData()
    val restoreNotes: LiveData<Boolean> = _restoreNotes

    private val _deleteAllNote: MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteAllNote: LiveData<Resource<String>> = _deleteAllNote

    lateinit var sPref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor

    companion object {
        const val APP_SETTINGS = "AppSettings"
        const val THEME = "THEME"
        const val DARK = "DARK"
        const val LIGHT = "LIGHT"
        const val SYSTEM = "SYSTEM"
        const val BIOMETRIC = "BIOMETRIC"
        const val AUTO_BACKUP = "AUTO BACKUP"
        const val BACKUP_INTERVAL = "BACKUP INTERVAL"
        const val APP_PIN = "AppPin"
    }

    fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

    fun setTheme(value: String) {
        editor.putString(THEME, value)
        editor.commit()
    }

    fun getTheme(): String? {
        return sPref.getString(THEME, SYSTEM)
    }

    fun getBiometric(): Boolean {
        return sPref.getBoolean(BIOMETRIC, false)
    }

    fun setBiometric(value: Boolean) {
        editor.putBoolean(BIOMETRIC, value)
        editor.apply()
    }

    fun getAutoBackup(): Boolean {
        return sPref.getBoolean(AUTO_BACKUP, true)
    }

    fun setAutoBackup(value: Boolean) {
        editor.putBoolean(AUTO_BACKUP, value)
        editor.apply()
    }

    fun setBackupInterval(value: Int) {
        editor.putInt(BACKUP_INTERVAL, value)
        editor.commit()
    }

    fun getBackupInterval(): Int {
        return sPref.getInt(BACKUP_INTERVAL, 1)
    }

    fun setAppPin(value: Int) {
        editor.putInt(APP_PIN, value)
        editor.apply()
    }

    fun getAppPin(): Int {
        return sPref.getInt(APP_PIN, 0)
    }

    fun enableTwoFactor(id: String, pin: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.setTwoFactorAuth(id, pin).collect {
                _enableTwoFactor.value = it
            }
        }
    }

    fun disableTwoFactor(id: String, pin: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.disableTwoFactorAuth(id, pin).collect {
                _disableTwoFactor.value = it
            }
        }
    }

    fun checkTwoFactor(id: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.checkTwoFactorSet(id).collect {
                _checkTwoFactor.value = it
            }
        }
    }

    private fun getRefreshToken() = localNotesRepoUseCase.getRefreshKey()!!

    fun logout() {
        viewModelScope.launch {
            remoteNotesRepoUseCase.logoutUser(getRefreshToken()).collect {
                _logout.value = it
            }
        }
    }

    fun backupNotes(context: Context, keysetHandle: KeysetHandle) {
        viewModelScope.launch {
            localNotesRepoUseCase.backUpNotes(context, keysetHandle).collect {
                _backupNotes.value = it
            }
        }
    }

    fun restoreBackup(context: Context, keysetHandle: KeysetHandle, backupFile: ByteArray) {
        viewModelScope.launch {
            localNotesRepoUseCase.restoreNotes(context, keysetHandle, backupFile).collect {
                _restoreNotes.value = it
            }
        }
    }

    fun deleteAllNote() {
        viewModelScope.launch {
            remoteNotesRepoUseCase.deleteAllNote().collect {
                _deleteAllNote.value = it
            }
        }
    }
}