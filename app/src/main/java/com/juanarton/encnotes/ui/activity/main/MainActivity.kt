package com.juanarton.encnotes.ui.activity.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.adapter.NotesAdapter
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityMainBinding
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding

    private lateinit var auth: FirebaseAuth

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
        external fun baseUrl(): String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null && mainViewModel.getIsLoggedIn()) {
            initView()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun initView() {
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Cryptography.initTink()

        binding?.apply {
            rvNotes.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            val rvAdapter = NotesAdapter(this@MainActivity)
            rvNotes.adapter = rvAdapter

            val x = Cryptography.serializeKeySet(Cryptography.generateKeySet()) //keyset in string
            Log.d("keyset", x)

            val y = Cryptography.encrypt("anjay", Cryptography.deserializeKeySet(x)) //encrypted text
            Log.d("testEnc", y)


            Log.d("testDesc", Cryptography.decrypt(y, Cryptography.deserializeKeySet(x))) //decypted text

            auth.uid?.let { mainViewModel.insertNote(it, "test", "anjay") }

            mainViewModel.insertNote.observe(this@MainActivity) {

            }

            mainViewModel.getNotes().observe(this@MainActivity) {
                rvAdapter.submitData(lifecycle, it)
                /*rvAdapter.addLoadStateListener { loadState ->
                    if (loadState.source.append.endOfPaginationReached) {
                        if (rvAdapter.itemCount > 0) {
                            binding?.tvNoData?.visibility = View.GONE
                        }
                    }
                }*/
            }
        }
    }
}