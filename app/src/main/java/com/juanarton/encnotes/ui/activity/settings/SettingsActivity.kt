package com.juanarton.encnotes.ui.activity.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource.Companion.FILE_NAME
import com.juanarton.encnotes.databinding.ActivitySettingsBinding
import com.juanarton.encnotes.di.DatabaseModule.Companion.DB_NAME
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.DARK
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.LIGHT
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.SYSTEM
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        binding?.apply {
            settingsViewModel.getTheme(sharedPreferences).let {
                cgThemeSelector.check(
                    when (it) {
                        SYSTEM -> R.id.chipSystem
                        LIGHT -> R.id.chipLight
                        DARK -> R.id.chipDark
                        else -> { R.id.chipSystem }
                    }
                )
            }

            cgThemeSelector.setOnCheckedStateChangeListener { group, _ ->
                when (group.checkedChipId) {
                    chipSystem.id -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        settingsViewModel.setTheme(sharedPreferences.edit(), SYSTEM)
                    }
                    chipLight.id -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        settingsViewModel.setTheme(sharedPreferences.edit(), LIGHT)
                    }
                    chipDark.id -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        settingsViewModel.setTheme(sharedPreferences.edit(), DARK)
                    }
                }
            }

            btLogout.setOnClickListener {
                settingsViewModel.clearSharedPreferences()
                this@SettingsActivity.deleteSharedPreferences(FILE_NAME)
                this@SettingsActivity.deleteDatabase(DB_NAME)
                this@SettingsActivity.filesDir.deleteRecursively()
                this@SettingsActivity.cacheDir.deleteRecursively()
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                ActivityCompat.finishAfterTransition(this@SettingsActivity)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}