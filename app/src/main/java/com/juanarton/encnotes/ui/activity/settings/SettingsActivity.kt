package com.juanarton.encnotes.ui.activity.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.ActivitySettingsBinding
import com.juanarton.encnotes.di.DatabaseModule.Companion.DB_NAME
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.APP_SETTINGS
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.DARK
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.LIGHT
import com.juanarton.encnotes.ui.activity.settings.SettingsViewModel.Companion.SYSTEM
import com.juanarton.encnotes.ui.fragment.apppin.AppPinFragment
import com.juanarton.encnotes.ui.fragment.apppin.PinListener
import com.juanarton.encnotes.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(), PinListener {

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

        settingsViewModel.sPref = getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
        settingsViewModel.editor = settingsViewModel.sPref.edit()

        setUiState()
        stateObserver()

        binding?.apply {
            btLogout.setOnClickListener {
                settingsViewModel.logout()
            }

            swBiometric.setOnCheckedChangeListener { _, isChecked ->
                when (isChecked) {
                    true -> {
                        if (settingsViewModel.getAppPin() == 0) {
                            FragmentBuilder.build(
                                this@SettingsActivity, AppPinFragment(getString(R.string.please_set_new_pin), true), android.R.id.content
                            )
                        } else {
                            settingsViewModel.setBiometric(true)
                        }
                    }
                    false -> {
                        settingsViewModel.setBiometric(false)
                    }
                }
            }

            cgThemeSelector.setOnCheckedStateChangeListener { group, _ ->
                when (group.checkedChipId) {
                    chipSystem.id -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        settingsViewModel.setTheme(SYSTEM)
                    }
                    chipLight.id -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        settingsViewModel.setTheme(LIGHT)
                    }
                    chipDark.id -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        settingsViewModel.setTheme(DARK)
                    }
                }
            }
        }
    }

    private fun stateObserver() {
        settingsViewModel.logout.observe(this@SettingsActivity) {
            this@SettingsActivity.deleteSharedPreferences(FILE_NAME)
            this@SettingsActivity.deleteDatabase(DB_NAME)
            this@SettingsActivity.filesDir.deleteRecursively()
            this@SettingsActivity.cacheDir.deleteRecursively()

            when(it){
                is Resource.Success -> {
                    val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
    }

    private fun setUiState() {
        binding?.apply {
            settingsViewModel.getTheme().let {
                cgThemeSelector.check(
                    when (it) {
                        SYSTEM -> R.id.chipSystem
                        LIGHT -> R.id.chipLight
                        DARK -> R.id.chipDark
                        else -> { R.id.chipSystem }
                    }
                )
            }

            settingsViewModel.getBiometric().let {
                swBiometric.isChecked = it == true
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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onPinSubmit(pin: Int) {
        Log.d("test", "work4")
        settingsViewModel.setAppPin(pin)
        if (settingsViewModel.getAppPin() != 0) {
            settingsViewModel.setBiometric(true)
        } else {
            binding?.swBiometric?.isChecked = false
        }
    }
}