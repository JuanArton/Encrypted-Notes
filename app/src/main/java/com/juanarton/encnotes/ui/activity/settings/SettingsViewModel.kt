package com.juanarton.encnotes.ui.activity.settings

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
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

    companion object {
        const val THEME = "THEME"
        const val DARK = "DARK"
        const val LIGHT = "LIGHT"
        const val SYSTEM = "SYSTEM"
    }

    fun setTheme(editor: SharedPreferences.Editor, value: String) {
        editor.putString(THEME, value)
        editor.apply()
    }

    fun getTheme(sPref: SharedPreferences): String? {
        return sPref.getString(THEME, SYSTEM)
    }

    fun getRefreshToken() = localNotesRepoUseCase.getRefreshKey()!!

    fun logout() {
        viewModelScope.launch {
            remoteNotesRepoUseCase.logoutUser(getRefreshToken()).collect {
                _logout.value = it
            }
        }
    }
}