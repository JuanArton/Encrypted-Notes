package com.juanarton.encnotes.ui.activity.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.validation.BaseValidator
import com.juanarton.encnotes.core.validation.EmailValidator
import com.juanarton.encnotes.core.validation.EmptyValidator
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
    var stateList: MutableList<Boolean> = MutableList(2) { false }

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

        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        val intent = Intent(this, PinActivity::class.java)

        loginViewModel.signInByGoogle.observe(this) { loggedUser ->
            handleLoginResult(loggedUser)
        }

        loginViewModel.loginByEmail.observe(this) { loggedUser ->
            handleLoginResult(loggedUser)
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
            btLogin.isEnabled = false
            ibGLogin.setOnClickListener {
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    webKey()
                )
                    .setNonce(generateNonce())
                    .build()

                loginViewModel.singWithGoogleAcc(signInWithGoogleOption, this@LoginActivity)
            }

            btLogin.setOnClickListener {
                loginViewModel.loginByEmail(etEmail.text.toString(), etPassword.text.toString())
            }

            etEmail.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    runnable?.let { handler.removeCallbacks(it) }

                    runnable = Runnable {
                        s?.let { email ->
                            val emailValidation = BaseValidator.validate(
                                EmptyValidator(email.toString()),
                                EmailValidator(email.toString())
                            )

                            stateList[0] = emailValidation.isSuccess
                            tvEmailStatus.text = getString(emailValidation.message)
                            if (emailValidation.isSuccess) {
                                tvEmailStatus.text = getString(R.string.empty)
                            }
                            setTextColor(tvEmailStatus, emailValidation.isSuccess)
                            checkState()
                        }
                    }

                    runnable?.let { handler.postDelayed(it, 50) }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            etPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    runnable?.let { handler.removeCallbacks(it) }

                    runnable = Runnable {
                        s?.let { password ->
                            val passwordValidation = EmptyValidator(password.toString()).validate()

                            stateList[1] = passwordValidation.isSuccess
                            tvPasswordStatus.text = getString(passwordValidation.message)
                            if (passwordValidation.isSuccess) {
                                tvPasswordStatus.text = getString(R.string.empty)
                            }
                            setTextColor(tvPasswordStatus, passwordValidation.isSuccess)
                            checkState()
                        }
                    }

                    runnable.let { handler.postDelayed(it, 50) }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun setTextColor(textView: TextView, state: Boolean) {
        if (state) {
            val correctColor = ContextCompat.getColor(this@LoginActivity, android.R.color.holo_green_light)
            textView.setTextColor(correctColor)
        } else {
            val errorColor = ContextCompat.getColor(this@LoginActivity, android.R.color.holo_red_dark)
            textView.setTextColor(errorColor)
        }
    }

    private fun checkState() {
        val allTrue = stateList.all { it }
        binding?.apply {
            btLogin.isEnabled = allTrue
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

