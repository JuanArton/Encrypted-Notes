package com.juanarton.privynote.ui.activity.register

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
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.domain.model.LoggedUser
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.validation.BaseValidator
import com.juanarton.privynote.core.validation.EmailValidator
import com.juanarton.privynote.core.validation.EmptyValidator
import com.juanarton.privynote.core.validation.PasswordValidator
import com.juanarton.privynote.core.validation.UsernameValidation
import com.juanarton.privynote.databinding.ActivityRegisterBinding
import com.juanarton.privynote.ui.activity.login.LoginActivity
import com.juanarton.privynote.ui.fragment.customdialog.CustomDialogFragment
import com.juanarton.privynote.ui.fragment.customdialog.CustomDialogListener
import com.juanarton.privynote.ui.fragment.loading.LoadingFragment
import com.juanarton.privynote.ui.utils.FragmentBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.security.MessageDigest
import java.util.UUID

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity(), CustomDialogListener {

    private var _binding: ActivityRegisterBinding? = null
    private val binding get() = _binding
    private val registerViewModel: RegisterViewModel by viewModels()
    private val loadingDialog = LoadingFragment()
    private var uid = ""
    private var username: String? = ""
    var stateList: MutableList<Boolean> = MutableList(4) { false }

    private external fun webKey(): String

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        registerViewModel.signInByGoogle.observe(this) { loggedUser ->
            handleLoginResult(loggedUser)
        }

        registerViewModel.signInByEmail.observe(this) { loggedUser ->
            when(loggedUser){
                is Resource.Success -> {
                    FragmentBuilder.destroyFragment(this, loadingDialog)
                    loggedUser.data?.let {
                        binding?.apply {
                            val fragment = CustomDialogFragment(
                                getString(R.string.instruction),
                                getString(R.string.register_success),
                                getString(R.string.ok)
                            )
                            FragmentBuilder.build(this@RegisterActivity, fragment, android.R.id.content)
                        }
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

        binding?.apply {
            btRegister.isEnabled = false

            setEditTextListener()

            ibGLogin.setOnClickListener {
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    webKey()
                )
                    .setNonce(generateNonce())
                    .build()

                registerViewModel.singWithGoogleAcc(signInWithGoogleOption, this@RegisterActivity)
            }

            btRegister.setOnClickListener {
                registerViewModel.signInByEmail(etEmail.text.toString(), etPassword.text.toString())
            }
        }
    }

    private fun setEditTextListener() {
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        binding?.apply {
            etUsername.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    runnable?.let { handler.removeCallbacks(it) }

                    runnable = Runnable {
                        s?.let { username ->
                            val usernameValidation = BaseValidator.validate(
                                EmptyValidator(username.toString()),
                                UsernameValidation(username.toString())
                            )
                            stateList[0] = usernameValidation.isSuccess
                            tvUsernameStatus.text = getString(usernameValidation.message)
                            setTextColor(tvUsernameStatus, usernameValidation.isSuccess)
                            checkState()
                        }
                    }

                    runnable?.let { handler.postDelayed(it, 50) }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

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

                            stateList[1] = emailValidation.isSuccess
                            tvEmailStatus.text = getString(emailValidation.message)
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
                            val passwordValidation = BaseValidator.validate(
                                EmptyValidator(password.toString()),
                                PasswordValidator(password.toString())
                            )

                            stateList[2] = passwordValidation.isSuccess
                            tvPasswordStatus.text = getString(passwordValidation.message)
                            var color = 0
                            if (passwordValidation.isSuccess) {
                                when (passwordValidation.message) {
                                    R.string.weak_password -> {
                                        color = ContextCompat.getColor(this@RegisterActivity, android.R.color.holo_red_dark)
                                    }
                                    R.string.medium_password -> {
                                        color = ContextCompat.getColor(this@RegisterActivity, android.R.color.holo_orange_dark)
                                    }
                                    R.string.strong_password -> {
                                        color = ContextCompat.getColor(this@RegisterActivity, android.R.color.holo_green_light)
                                    }
                                }
                                tvPasswordStatus.setTextColor(color)
                            } else {
                                val color = ContextCompat.getColor(this@RegisterActivity, android.R.color.holo_red_dark)
                                tvPasswordStatus.setTextColor(color)
                            }
                            checkState()

                            if (etPasswordRepeat.text.toString().isNotEmpty()) {
                                checkRepeatPassword()
                            }
                        }
                    }

                    runnable?.let { handler.postDelayed(it, 50) }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            etPasswordRepeat.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    runnable?.let { handler.removeCallbacks(it) }

                    runnable = Runnable {
                        s?.let { password ->
                            checkRepeatPassword()
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
            val correctColor = ContextCompat.getColor(this@RegisterActivity, android.R.color.holo_green_light)
            textView.setTextColor(correctColor)
        } else {
            val errorColor = ContextCompat.getColor(this@RegisterActivity, android.R.color.holo_red_dark)
            textView.setTextColor(errorColor)
        }
    }

    private fun checkRepeatPassword() {
        binding?.apply {
            if (etPassword.text.toString() != etPasswordRepeat.text.toString()){
                stateList[3] = false
                tvRePasswordStatus.text = getString(R.string.password_doesnt_match)
                setTextColor(tvRePasswordStatus, false)
            } else {
                stateList[3] = true
                tvRePasswordStatus.text = getString(R.string.password_correct)
                setTextColor(tvRePasswordStatus, true)
            }
            checkState()
        }
    }

    private fun checkState() {
        val allTrue = stateList.all { it }
        binding?.apply {
            btRegister.isEnabled = allTrue
        }
    }

    private fun handleLoginResult(loggedUser: Resource<LoggedUser>) {
        when(loggedUser){
            is Resource.Success -> {
                loggedUser.data?.let {
                    uid = it.uid
                    username = it.displayName
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

    override fun onButtonPressed(ok: Boolean) {
        if (ok) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}