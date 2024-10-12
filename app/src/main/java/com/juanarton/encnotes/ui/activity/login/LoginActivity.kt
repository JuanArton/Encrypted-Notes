package com.juanarton.encnotes.ui.activity.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.ActivityLoginBinding
import com.juanarton.encnotes.ui.LoadingDialog
import com.juanarton.encnotes.ui.activity.main.MainActivity
import com.juanarton.encnotes.ui.activity.pin.PinActivity
import dagger.hilt.android.AndroidEntryPoint
import java.security.MessageDigest
import java.util.UUID

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding
    private lateinit var loadingDialog: LoadingDialog

    private external fun webKey(): String

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadingDialog = LoadingDialog(this)

        loginViewModel.loggedUser.observe(this) { loggedUser ->
            when(loggedUser){
                is Resource.Success -> {
                    loggedUser.data?.let {
                        Toast.makeText(
                            this,
                            getString(R.string.login_success),
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this, PinActivity::class.java)
                        intent.putExtra("uid", it.uid)
                        intent.putExtra("username", it.displayName)
                        startActivity(intent)
                        finish()
                    }
                    loadingDialog.dismiss()
                }
                is Resource.Loading -> {
                    loadingDialog.show()
                }
                is Resource.Error -> {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this,
                        loggedUser.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding?.apply {
            ibGLogin.setOnClickListener {
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    webKey()
                )
                    .setNonce(generateNonce())
                    .build()

                loginViewModel.singWithGoogleAcc(signInWithGoogleOption, this@LoginActivity)
            }
        }
    }

    private fun generateNonce(): String {
        val ranNonce: String = UUID.randomUUID().toString()
        val bytes: ByteArray = ranNonce.toByteArray()
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        val digest: ByteArray = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

