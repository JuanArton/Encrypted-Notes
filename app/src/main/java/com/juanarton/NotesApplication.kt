package com.juanarton

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.APP_SETTINGS
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.DARK
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.LIGHT
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.SYSTEM
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.THEME
import dagger.hilt.android.HiltAndroidApp
import java.lang.reflect.Field
import javax.inject.Inject


@HiltAndroidApp
open class NotesApplication : Application(), Configuration.Provider{

    @Inject
    lateinit var workerFactory : HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        val sharedPreferences = getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
        val theme = sharedPreferences.getString(THEME, SYSTEM)

        setDefaultFont(this, "DEFAULT", "fonts/robotoregular.ttf")
        setDefaultFont(this, "MONOSPACE", "fonts/robotomono.ttf");
        setDefaultFont(this, "SERIF", "fonts/notoserif.ttf");
        setDefaultFont(this, "SANS_SERIF", "fonts/robotoregular.ttf");

        when (theme) {
            SYSTEM -> { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
            LIGHT -> { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
            DARK -> { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        }
    }

    private fun setDefaultFont(
        context: Context,
        staticTypefaceFieldName: String?, fontAssetName: String?
    ) {
        val regular = Typeface.createFromAsset(
            context.assets,
            fontAssetName
        )
        replaceFont(staticTypefaceFieldName, regular)
    }

    private fun replaceFont(
        staticTypefaceFieldName: String?,
        newTypeface: Typeface?
    ) {
        try {
            val staticField: Field = Typeface::class.java
                .getDeclaredField(staticTypefaceFieldName)
            staticField.isAccessible = true
            staticField.set(null, newTypeface)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }


    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}