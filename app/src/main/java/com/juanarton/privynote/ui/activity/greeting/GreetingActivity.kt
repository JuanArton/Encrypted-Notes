package com.juanarton.privynote.ui.activity.greeting

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.juanarton.privynote.R
import com.juanarton.privynote.databinding.ActivityGreetingBinding
import com.juanarton.privynote.ui.activity.login.LoginActivity
import com.juanarton.privynote.ui.activity.register.RegisterActivity

class GreetingActivity : AppCompatActivity() {

    private var _binding: ActivityGreetingBinding? = null
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityGreetingBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding?.apply {
            btRegister.setOnClickListener {
                startActivity(Intent(this@GreetingActivity, RegisterActivity::class.java))
                finish()
            }

            btLogin.setOnClickListener {
                startActivity(Intent(this@GreetingActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}