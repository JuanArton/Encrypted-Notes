package com.juanarton.privynote.ui.activity.pin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.utils.Cryptography
import com.juanarton.privynote.databinding.ActivityPinBinding
import com.juanarton.privynote.ui.activity.twofactor.TwoFactorActivity
import com.juanarton.privynote.ui.fragment.copykey.CopyKeyFragment
import com.juanarton.privynote.ui.fragment.insertkey.InsertKeyFragment
import com.juanarton.privynote.ui.fragment.loading.LoadingFragment
import com.juanarton.privynote.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PinActivity : AppCompatActivity() {

    private val pinViewModel: PinViewModel by viewModels()
    private var _binding: ActivityPinBinding? = null
    private val binding get() = _binding
    private val loadingDialog = LoadingFragment()
    private lateinit var pin: String
    private var firstPin = 0
    val key = Cryptography.serializeKeySet(Cryptography.generateKeySet())

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

        val uid = intent.getStringExtra("uid")
        val username = intent.getStringExtra("username")?: buildString {
            append("User")
            append(NanoId.generate(7))
        }
        val encryptedUsername = Cryptography.encrypt(username, Cryptography.deserializeKeySet(key))
        val isRegistered = intent.getBooleanExtra("isRegistered", false)

        if (!uid.isNullOrEmpty() && encryptedUsername.isNotEmpty()) {
            pinViewModel.registerUser.observe(this) { result ->
                when(result){
                    is Resource.Success -> {
                        if (::pin.isInitialized && !result.data.isNullOrEmpty()) {
                            FragmentBuilder.destroyFragment(this, loadingDialog)
                            pinViewModel.loginUser(uid, pin, "")
                        } else {
                            Toast.makeText(this, getString(R.string.pin_empty), Toast.LENGTH_SHORT).show()
                        }
                    }
                    is Resource.Loading -> {
                        FragmentBuilder.build(this, loadingDialog, android.R.id.content)
                    }
                    is Resource.Error -> {
                        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
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
                                    FragmentBuilder.destroyFragment(this@PinActivity, loadingDialog)
                                    binding?.btSubmit?.isEnabled = false
                                    FragmentBuilder.build(
                                        this@PinActivity,
                                        InsertKeyFragment(),
                                        android.R.id.content
                                    )

                                    if (isRegistered) {
                                        val fragment = InsertKeyFragment()
                                        FragmentBuilder.build(this@PinActivity, fragment, android.R.id.content)
                                    } else {
                                        FragmentBuilder.destroyFragment(this@PinActivity, loadingDialog)
                                        binding?.btSubmit?.isEnabled = false
                                        val fragment = CopyKeyFragment().apply {
                                            arguments = Bundle().apply {
                                                putString("KEY_STRING", key)
                                            }
                                        }
                                        FragmentBuilder.build(this@PinActivity, fragment, android.R.id.content)
                                    }
                                } else {
                                    Toast.makeText(
                                        this@PinActivity,
                                        getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    FragmentBuilder.destroyFragment(this@PinActivity, loadingDialog)
                                }
                            }
                        }
                    }
                    is Resource.Loading -> {
                        FragmentBuilder.build(this@PinActivity, loadingDialog, android.R.id.content)
                    }
                    is Resource.Error -> {
                        if (result.message == "Proceed to OTP") {
                            val intent = Intent(this, TwoFactorActivity::class.java)
                            intent.putExtra("uid", uid)
                            intent.putExtra("pin", pin)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@PinActivity,
                                result.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            FragmentBuilder.destroyFragment(this@PinActivity, loadingDialog)
                        }
                    }
                }
            }

            binding?.apply {
                btSubmit.visibility = View.INVISIBLE

                otpView.setOtpCompletionListener {
                    if (!isRegistered) {
                        if (firstPin == 0) {
                            pin = it
                            firstPin = it.toInt()
                            otpView.text = Editable.Factory.getInstance().newEditable("")
                            tvPinHead.text = getString(R.string.please_reenter_pin)
                        } else if (firstPin == it.toInt()) {
                            btSubmit.visibility = View.VISIBLE
                            pinViewModel.registerUser(uid, pin, encryptedUsername)
                        }
                    } else {
                        pin = it
                        btSubmit.visibility = View.VISIBLE
                        pinViewModel.loginUser(uid, pin, "")
                    }
                }

                btSubmit.setOnClickListener {
                    if (!isRegistered) {
                        pinViewModel.registerUser(uid, pin, encryptedUsername)
                    } else {
                        pinViewModel.loginUser(uid, pin, "")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}