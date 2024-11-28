package com.juanarton.encnotes.ui.activity.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val localNotesRepoUseCase: LocalNotesRepoUseCase
) : ViewModel() {

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

    fun clearSharedPreferences() {
        localNotesRepoUseCase.clearSharedPreference()
    }
}