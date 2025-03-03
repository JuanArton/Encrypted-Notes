package com.juanarton.privynote.ui.activity.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.crypto.tink.KeysetHandle
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.source.local.SharedPrefDataSource.Companion.FILE_NAME
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.utils.Cryptography
import com.juanarton.privynote.databinding.ActivitySettingsBinding
import com.juanarton.privynote.di.DatabaseModule.Companion.DB_NAME
import com.juanarton.privynote.receiver.BackupBroadcastReceiver.Companion.cancelAlarm
import com.juanarton.privynote.receiver.BackupBroadcastReceiver.Companion.setRepeatingAlarm
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.APP_SETTINGS
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.DARK
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.LIGHT
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.SYSTEM
import com.juanarton.privynote.ui.fragment.apppin.AppPinFragment
import com.juanarton.privynote.ui.fragment.apppin.PinCallback
import com.juanarton.privynote.ui.fragment.loading.LoadingFragment
import com.juanarton.privynote.ui.fragment.qrimage.QrSecretFragment
import com.juanarton.privynote.ui.utils.FragmentBuilder
import com.juanarton.privynote.worker.BackupWorker.Companion.cancelBackupWork
import com.juanarton.privynote.worker.BackupWorker.Companion.scheduleBackupWork
import dagger.hilt.android.AndroidEntryPoint
import xyz.kumaraswamy.autostart.Autostart

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(), PinCallback {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var is2FAUserAction = false
    private val loadingDialog = LoadingFragment()
    private lateinit var keysetHandle: KeysetHandle
    private var backupFile: ByteArray? = byteArrayOf()
    private val auth = Firebase.auth
    private lateinit var selectBackUpLauncher: ActivityResultLauncher<Intent>

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

            backUpClickMask.setOnClickListener {
                val key = settingsViewModel.getCipherKey()

                if (!key.isNullOrEmpty()) {
                    val deserializedKey = Cryptography.deserializeKeySet(key)
                    settingsViewModel.backupNotes(this@SettingsActivity, deserializedKey)
                } else {
                    Toast.makeText(
                        this@SettingsActivity, getString(R.string.unable_retrieve_key), Toast.LENGTH_SHORT
                    ).show()
                }
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

            swTwoFactor.setOnCheckedChangeListener { _, _ ->
                if (is2FAUserAction) {
                    val pinFragment = AppPinFragment(getString(R.string.please_enter_pin), false, TWO_FACTOR)
                    FragmentBuilder.build(this@SettingsActivity, pinFragment, android.R.id.content)
                }
                is2FAUserAction = true
            }

            cgThemeSelector.setOnCheckedStateChangeListener { group, _ ->
                when (group.checkedChipId) {
                    chipSystem.id -> {
                        settingsViewModel.setTheme(SYSTEM)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    chipLight.id -> {
                        settingsViewModel.setTheme(LIGHT)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    chipDark.id -> {
                        settingsViewModel.setTheme(DARK)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }
            }

            cgIntervalSelector.setOnCheckedStateChangeListener { group, _ ->
                when (group.checkedChipId) {
                    chip1.id -> {
                        cancelAlarm(this@SettingsActivity)
                        settingsViewModel.setBackupInterval(1)

                        setRepeatingAlarm(this@SettingsActivity, 1)
                        scheduleBackupWork(this@SettingsActivity, 1)
                    }
                    chip3.id -> {
                        cancelAlarm(this@SettingsActivity)
                        settingsViewModel.setBackupInterval(3)

                        setRepeatingAlarm(this@SettingsActivity, 3)
                        scheduleBackupWork(this@SettingsActivity, 3)
                    }
                    chip7.id -> {
                        cancelAlarm(this@SettingsActivity)
                        settingsViewModel.setBackupInterval(7)

                        setRepeatingAlarm(this@SettingsActivity, 7)
                        scheduleBackupWork(this@SettingsActivity, 7)
                    }
                }
            }

            swAutoBackup.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    settingsViewModel.setAutoBackup(true)

                    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                    val packageName = packageName
                    val isIgnoringOptimization = powerManager.isIgnoringBatteryOptimizations(packageName)

                    if (!isIgnoringOptimization) {
                        disableOptimizationDialog()
                    }

                    if (!Autostart.getSafeState(this@SettingsActivity)) {
                        showXiaomiAutostartDialog()
                    }

                    setRepeatingAlarm(this@SettingsActivity, settingsViewModel.getBackupInterval())
                    scheduleBackupWork(this@SettingsActivity, settingsViewModel.getBackupInterval())
                } else {
                    settingsViewModel.setAutoBackup(false)

                    cancelAlarm(this@SettingsActivity)
                    cancelBackupWork(this@SettingsActivity)

                    cgIntervalSelector.clearCheck()
                    chip1.isChecked = false
                    chip3.isChecked = false
                    chip7.isChecked = false
                }
                chip1.isEnabled = isChecked
                chip3.isEnabled = isChecked
                chip7.isEnabled = isChecked
                getBackUpInterval()
            }

            restoreClickMask.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/bin"))
                }
                selectBackUpLauncher.launch(intent)
            }

            selectBackUpLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Uri? = result.data?.data
                    data?.let {
                        val key = settingsViewModel.getCipherKey()
                        if (!key.isNullOrEmpty()) {
                            keysetHandle = Cryptography.deserializeKeySet(key)
                            val inputStream = this@SettingsActivity.contentResolver.openInputStream(it)
                            backupFile = inputStream?.readBytes()
                            settingsViewModel.deleteAllNote()
                        } else {
                            Toast.makeText(
                                this@SettingsActivity, getString(R.string.unable_retrieve_key), Toast.LENGTH_SHORT
                            ).show()
                        }
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
                    restartApp()
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    restartApp()
                }
            }
        }

        settingsViewModel.checkTwoFactor.observe(this) {
            binding?.apply {
                when(it){
                    is Resource.Success -> {
                        it.data?.let { isEnabled ->
                            cpiTFALoading.visibility = View.INVISIBLE
                            swTwoFactor.visibility = View.VISIBLE
                            set2FASwitchChecked(isEnabled)
                        }
                    }
                    is Resource.Loading -> {
                        cpiTFALoading.visibility = View.VISIBLE
                        swTwoFactor.visibility = View.INVISIBLE
                    }
                    is Resource.Error -> {
                        swTwoFactor.isClickable = false
                        cpiTFALoading.visibility = View.INVISIBLE
                        swTwoFactor.visibility = View.VISIBLE
                        swTwoFactor.isEnabled = false
                        Toast.makeText(this@SettingsActivity, it.message, Toast.LENGTH_SHORT).show()
                    }
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

        settingsViewModel.backupNotes.observe(this) {
            Toast.makeText(this@SettingsActivity, getString(R.string.backup_msg), Toast.LENGTH_SHORT).show()
        }

        settingsViewModel.restoreNotes.observe(this) {
            Toast.makeText(this@SettingsActivity, getString(R.string.restore_msg), Toast.LENGTH_SHORT).show()
            restartApp()
        }

        settingsViewModel.deleteAllNote.observe(this) {
            when(it){
                is Resource.Success -> {
                    settingsViewModel.restoreBackup(this, keysetHandle, backupFile!!)
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

            settingsViewModel.getAutoBackup().let {
                swAutoBackup.isChecked = it
                chip1.isEnabled = it
                chip3.isEnabled = it
                chip7.isEnabled = it

                if (it) {
                    getBackUpInterval()
                }
            }
        }
    }

    private fun getBackUpInterval() {
        settingsViewModel.getBackupInterval().let { interval ->
            binding?.cgIntervalSelector?.check(
                when (interval) {
                    1 -> R.id.chip1
                    3 -> R.id.chip3
                    7 -> R.id.chip7
                    else -> { R.id.chip1 }
                }
            )
        }
    }

    private fun restartApp() {
        val intent = this@SettingsActivity.packageManager.getLaunchIntentForPackage(this@SettingsActivity.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        this@SettingsActivity.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    private fun set2FASwitchChecked(checked: Boolean) {
        is2FAUserAction = false
        binding?.swTwoFactor?.isChecked = checked
    }

    private fun showXiaomiAutostartDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.auto_backup))
            .setMessage(getString(R.string.enable_mi_autostart))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                startActivity(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    )
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun disableOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.auto_backup))
            .setMessage(getString(R.string.disable_optimization_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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