package com.juanarton.encnotes.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import com.juanarton.encnotes.R
import com.juanarton.encnotes.databinding.ActivityMainBinding
import com.juanarton.encnotes.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainActivtyViewModel: MainActivtyViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding

    external fun keyWork(): String

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        if (
            !mainActivtyViewModel.getIsLoggedIn() &&
            mainActivtyViewModel.getGUID().isNullOrEmpty()
        ) {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        DynamicColors.applyToActivitiesIfAvailable(application);
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val result = keyWork()
        Log.d("LauncherScreenActivity", "Native call result: $result")
    }
}