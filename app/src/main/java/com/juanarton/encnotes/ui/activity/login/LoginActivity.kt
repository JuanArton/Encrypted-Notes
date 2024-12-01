package com.juanarton.encnotes.ui.activity.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.ActivityLoginBinding
import com.juanarton.encnotes.ui.activity.pin.PinActivity
import com.juanarton.encnotes.ui.fragment.loading.LoadingFragment
import com.juanarton.encnotes.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.security.MessageDigest
import java.util.UUID

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding
    private val loadingDialog = LoadingFragment()
    private var uid = ""
    private var username: String? = ""

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

        val intent = Intent(this, PinActivity::class.java)

        loginViewModel.signInByGoogle.observe(this) { loggedUser ->
            handleLoginResult(loggedUser)
        }

        loginViewModel.loginByEmail.observe(this) { loggedUser ->
            handleLoginResult(loggedUser)
        }

        loginViewModel.signInByEmail.observe(this) { loggedUser ->
            when(loggedUser){
                is Resource.Success -> {
                    loggedUser.data?.let {
                        binding?.apply {
                            tvRegisterMessage.visibility = View.VISIBLE
                            tvRegisterMessage.text = getString(R.string.register_success)
                        }
                    }
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                }
                is Resource.Loading -> {
                    FragmentBuilder.build(this, loadingDialog, android.R.id.content)
                }
                is Resource.Error -> {
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                    Toast.makeText(this, loggedUser.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        loginViewModel.checkRegistered.observe(this) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { isRegistered ->
                        intent.putExtra("uid", uid)
                        intent.putExtra("username", username)
                        intent.putExtra("isRegistered", isRegistered)
                        startActivity(intent)
                        finish()
                    }
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
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

            btSignIn.setOnClickListener {
                loginViewModel.signInByEmail(etEmail.text.toString(), etPassword.text.toString())
            }

            btLogin.setOnClickListener {
                loginViewModel.loginByEmail(etEmail.text.toString(), etPassword.text.toString())
            }
        }
    }

    private fun handleLoginResult(loggedUser: Resource<LoggedUser>) {
        when(loggedUser){
            is Resource.Success -> {
                loggedUser.data?.let {
                    uid = it.uid
                    username = it.displayName
                    loginViewModel.checkRegistered(it.uid)
                }
            }
            is Resource.Loading -> {
                FragmentBuilder.build(this, loadingDialog, android.R.id.content)
            }
            is Resource.Error -> {
                FragmentBuilder.destroyFragment(this, loadingDialog)
                Toast.makeText(this, loggedUser.message, Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

