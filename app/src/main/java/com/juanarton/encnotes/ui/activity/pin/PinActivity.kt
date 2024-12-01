package com.juanarton.encnotes.ui.activity.pin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.ActivityPinBinding
import com.juanarton.encnotes.ui.activity.twofactor.TwoFactorActivity
import com.juanarton.encnotes.ui.fragment.copykey.CopyKeyFragment
import com.juanarton.encnotes.ui.fragment.insertkey.InsertKeyFragment
import com.juanarton.encnotes.ui.fragment.loading.LoadingFragment
import com.juanarton.encnotes.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.viascom.nanoid.NanoId

@AndroidEntryPoint
class PinActivity : AppCompatActivity() {

    private val pinViewModel: PinViewModel by viewModels()
    private var _binding: ActivityPinBinding? = null
    private val binding get() = _binding
    private val loadingDialog = LoadingFragment()
    private lateinit var pin: String
    private var firstPin = 0
    val auth = Firebase.auth

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
        val isRegistered = intent.getBooleanExtra("isRegistered", false)
        Log.d("status", isRegistered.toString())
        Log.d("status4", uid.toString())
        Log.d("status5", username.toString())

        if (!uid.isNullOrEmpty() && username.isNotEmpty()) {
            pinViewModel.registerUser.observe(this) { result ->
                when(result){
                    is Resource.Success -> {
                        if (::pin.isInitialized && !result.data.isNullOrEmpty()) {
                            FragmentBuilder.destroyFragment(this, loadingDialog)
                            val fragment = CopyKeyFragment()

                            val bundle = Bundle()
                            bundle.putString("uid", uid)
                            bundle.putString("pin", pin)

                            fragment.arguments = bundle

                            FragmentBuilder.build(this, fragment, android.R.id.content)
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

            pinViewModel.checkTwoFactor.observe(this) { result ->
                when(result){
                    is Resource.Success -> {
                        FragmentBuilder.destroyFragment(this, loadingDialog)
                        if (result.data == true) {
                            val intent = Intent(this, TwoFactorActivity::class.java)
                            intent.putExtra("uid", uid)
                            intent.putExtra("pin", pin)
                            startActivity(intent)
                            finish()
                        } else {
                            val fragment = InsertKeyFragment(true)

                            val bundle = Bundle()
                            bundle.putString("uid", uid)
                            bundle.putString("pin", pin)

                            fragment.arguments = bundle

                            FragmentBuilder.build(this, fragment, android.R.id.content)
                        }
                    }
                    is Resource.Loading -> {
                        FragmentBuilder.build(this, loadingDialog, android.R.id.content)
                    }
                    is Resource.Error -> {
                        FragmentBuilder.destroyFragment(this, loadingDialog)
                        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
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
                            pinViewModel.registerUser(uid, pin, username)
                        }
                    } else {
                        pin = it
                        btSubmit.visibility = View.VISIBLE
                        pinViewModel.checkTwoFactor(auth.uid.toString())
                    }
                }

                btSubmit.setOnClickListener {
                    if (!isRegistered) {
                        pinViewModel.registerUser(uid, pin, username)
                    } else {
                        pinViewModel.checkTwoFactor(auth.uid.toString())
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