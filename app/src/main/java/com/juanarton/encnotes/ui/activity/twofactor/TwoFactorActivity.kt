package com.juanarton.encnotes.ui.activity.twofactor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.ActivityTwoFactorBinding
import com.juanarton.encnotes.ui.fragment.insertkey.InsertKeyFragment
import com.juanarton.encnotes.ui.fragment.loading.LoadingFragment
import com.juanarton.encnotes.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TwoFactorActivity : AppCompatActivity() {

    private var _binding: ActivityTwoFactorBinding? = null
    private val binding get() = _binding
    private val twoFactorViewModel: TwoFactorViewModel by viewModels()
    private var otp = ""
    private val loadingDialog = LoadingFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityTwoFactorBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val uid = intent.getStringExtra("uid")
        val pin = intent.getStringExtra("pin")

        if (!uid.isNullOrEmpty() && !pin.isNullOrEmpty()) {
            twoFactorViewModel.twoFactorAuth.observe(this) { result ->
                when(result){
                    is Resource.Success -> {
                        result.data?.let { login ->
                            lifecycleScope.launch {
                                val setAccKey = twoFactorViewModel.setAccessKey(login.accessToken)
                                val setRefKey = twoFactorViewModel.setRefreshKey(login.refreshToken)
                                val setLoggedIn = twoFactorViewModel.setIsLoggedIn(true)

                                if (setAccKey && setRefKey && setLoggedIn) {
                                    FragmentBuilder.destroyFragment(this@TwoFactorActivity, loadingDialog)

                                    binding?.btSubmit?.isEnabled = false
                                    FragmentBuilder.build(
                                        this@TwoFactorActivity,
                                        InsertKeyFragment(),
                                        android.R.id.content
                                    )

                                } else {
                                    Toast.makeText(
                                        this@TwoFactorActivity,
                                        getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    FragmentBuilder.destroyFragment(this@TwoFactorActivity, loadingDialog)
                                }
                            }
                        }
                    }
                    is Resource.Loading -> {
                        FragmentBuilder.build(this@TwoFactorActivity, loadingDialog, android.R.id.content)
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            this@TwoFactorActivity,
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        FragmentBuilder.destroyFragment(this@TwoFactorActivity, loadingDialog)
                    }
                }
            }

            binding?.apply {
                btSubmit.visibility = View.INVISIBLE

                otpView.setOtpCompletionListener {
                    otp = it
                    twoFactorViewModel.twoFactorAuth(uid, pin, it)
                    btSubmit.visibility = View.VISIBLE
                }

                btSubmit.setOnClickListener {
                    twoFactorViewModel.twoFactorAuth(uid, pin, otp)
                }
            }
        }
    }
}