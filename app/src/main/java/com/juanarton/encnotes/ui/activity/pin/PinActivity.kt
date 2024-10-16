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
import com.juanarton.encnotes.ui.fragment.copykey.CopyKeyFragment
import com.juanarton.encnotes.ui.fragment.insertkey.InsertKeyFragment
import com.juanarton.encnotes.ui.utils.FragmentBuilder
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
                            loadingDialog.dismiss()
                            val fragment = CopyKeyFragment()

                            val bundle = Bundle()
                            bundle.putString("uid", uid)
                            bundle.putString("pin", pin)

                            fragment.arguments = bundle

                            FragmentBuilder.build(this, fragment, android.R.id.content)
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
                        loadingDialog.dismiss()
                        if (result.message == "User already exist") {
                            val fragment = InsertKeyFragment()

                            val bundle = Bundle()
                            bundle.putString("uid", uid)
                            bundle.putString("pin", pin)

                            fragment.arguments = bundle

                            FragmentBuilder.build(this, fragment, android.R.id.content)
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

            binding?.otpView?.setOtpCompletionListener {
                pin = it
                pinViewModel.registerUser(uid, pin, username)
            }
        }
    }
}