package com.juanarton.encnotes.ui.activity.pin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.ActivityPinBinding
import com.juanarton.encnotes.ui.LoadingDialog
import com.juanarton.encnotes.ui.activity.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PinActivity : AppCompatActivity() {

    private val pinViewModel: PinViewModel by viewModels()
    private var _binding: ActivityPinBinding? = null
    private val binding get() = _binding
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var pin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadingDialog = LoadingDialog(this)

        val uid = intent.getStringExtra("uid")
        val username = intent.getStringExtra("username")

        if (!uid.isNullOrEmpty() && !username.isNullOrEmpty()) {
            pinViewModel.registerUser.observe(this) { result ->
                when(result){
                    is Resource.Success -> {
                        if (::pin.isInitialized && !result.data.isNullOrEmpty()) {
                            pinViewModel.loginUser(uid, pin)
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.pin_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Pin Activity", "Loading")
                        loadingDialog.show()
                    }
                    is Resource.Error -> {
                        Log.d("test", result.message!!)
                        if (result.message == "User already exist") {
                            pinViewModel.loginUser(uid, pin)
                        } else {
                            Toast.makeText(
                                this,
                                result.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            loadingDialog.dismiss()
                        }
                    }
                }
            }

            pinViewModel.loginUser.observe(this) { result ->
                when(result){
                    is Resource.Success -> {
                        result.data?.let { login ->
                            lifecycleScope.launch {
                                val setAccKey = pinViewModel.setAccessKey(login.accessToken)
                                val setRefKey = pinViewModel.setRefreshKey(login.refreshToken)
                                val setLoggedIn = pinViewModel.setIsLoggedIn(true)

                                if (setAccKey && setRefKey && setLoggedIn) {
                                    startActivity(Intent(this@PinActivity, MainActivity::class.java))
                                } else {
                                    Toast.makeText(
                                        this@PinActivity,
                                        getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loadingDialog.dismiss()
                                }
                            }
                            loadingDialog.dismiss()
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Pin Activity", "Loading")
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            this,
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingDialog.dismiss()
                    }
                }
            }

            binding?.otpView?.setOtpCompletionListener {
                pin = it
                pinViewModel.registerUser(uid, pin, username)
            }
        }
    }
}