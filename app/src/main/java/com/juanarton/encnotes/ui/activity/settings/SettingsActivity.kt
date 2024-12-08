package com.juanarton.encnotes.ui.activity.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
import com.juanarton.encnotes.ui.fragment.loading.LoadingFragment
import com.juanarton.encnotes.ui.fragment.qrimage.QrSecretFragment
import com.juanarton.encnotes.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue
import android.os.Process

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(), PinListener {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var isUserAction = true
    private val loadingDialog = LoadingFragment()
    val auth = Firebase.auth

    companion object {
        const val APP_PROTECTION = 1
        const val TWO_FACTOR = 2
    }

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
        settingsViewModel.checkTwoFactor(auth.uid.toString())

        binding?.apply {
            btLogout.setOnClickListener {
                settingsViewModel.logout()
            }

            swBiometric.setOnCheckedChangeListener { _, isChecked ->
                when (isChecked) {
                    true -> {
                        if (settingsViewModel.getAppPin() == 0) {
                            FragmentBuilder.build(
                                this@SettingsActivity,
                                AppPinFragment(getString(R.string.please_set_new_pin), true, APP_PROTECTION),
                                android.R.id.content
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

            swTwoFactor.setOnCheckedChangeListener { _, isChecked ->
                if (isUserAction) {
                    val pinFragment = AppPinFragment(getString(R.string.please_enter_pin), false, TWO_FACTOR)
                    FragmentBuilder.build(this@SettingsActivity, pinFragment, android.R.id.content)
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
                    val intent = this@SettingsActivity.packageManager.getLaunchIntentForPackage(this@SettingsActivity.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    this@SettingsActivity.startActivity(intent)
                    Process.killProcess(Process.myPid())
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    val intent = this@SettingsActivity.packageManager.getLaunchIntentForPackage(this@SettingsActivity.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    this@SettingsActivity.startActivity(intent)
                    Process.killProcess(Process.myPid())
                }
            }
        }

        settingsViewModel.checkTwoFactor.observe(this) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { isEnabled ->
                        setSwitchChecked(isEnabled)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Toast.makeText(this@SettingsActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        settingsViewModel.enableTwoFactor.observe(this) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { twoFactor ->
                        FragmentBuilder.destroyFragment(this, loadingDialog)
                        FragmentBuilder.build(
                            this@SettingsActivity, QrSecretFragment(twoFactor.qrImage, twoFactor.secret),
                            android.R.id.content
                        )
                    }
                }
                is Resource.Loading -> {
                    FragmentBuilder.build(this, loadingDialog, android.R.id.content)
                }
                is Resource.Error -> {
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                    binding?.swTwoFactor?.isChecked = false
                    Toast.makeText(this@SettingsActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        settingsViewModel.disableTwoFactor.observe(this) {
            when(it){
                is Resource.Success -> {
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                }
                is Resource.Loading -> {
                    FragmentBuilder.build(this, loadingDialog, android.R.id.content)
                }
                is Resource.Error -> {
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                    binding?.swTwoFactor?.isChecked = true
                    Toast.makeText(this@SettingsActivity, it.message, Toast.LENGTH_SHORT).show()
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

    fun setSwitchChecked(checked: Boolean) {
        isUserAction = false
        binding?.swTwoFactor?.isChecked = checked
        isUserAction = true
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

    override fun onPinSubmit(pin: Int, action: Int) {
        if (action == APP_PROTECTION) {
            settingsViewModel.setAppPin(pin)
            if (settingsViewModel.getAppPin() != 0) {
                settingsViewModel.setBiometric(true)
            } else {
                binding?.swBiometric?.isChecked = false
            }
        } else if (action == TWO_FACTOR) {
            binding?.apply {
                if (swTwoFactor.isChecked) {
                    settingsViewModel.enableTwoFactor(
                        auth.uid.toString(),
                        pin.toString()
                    )
                } else {
                    settingsViewModel.disableTwoFactor(
                        auth.uid.toString(),
                        pin.toString()
                    )
                }
            }
        }
    }
}