package com.juanarton.encnotes.ui.activity.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.adapter.NotesAdapter
import com.juanarton.encnotes.core.adapter.NotesPagingAdapter
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityMainBinding
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import com.juanarton.encnotes.ui.activity.note.NoteActivity
import com.juanarton.encnotes.ui.utils.DataSync
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var rvAdapter: NotesAdapter
    private var localNotes: List<Notes> = mutableListOf()

    private lateinit var auth: FirebaseAuth

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
        external fun baseUrl(): String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementsUseOverlay = false

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

        mainViewModel.getNotes()
        mainViewModel.getNotesRemote()

        binding?.apply {
            val listener: (Notes) -> Unit = {
            }

            rvNotes.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            rvAdapter = NotesAdapter(listener, arrayListOf())
            rvNotes.adapter = rvAdapter

            mainViewModel.getNotes.observe(this@MainActivity) { noteList ->
                val notDeleted = noteList.filter {
                    !it.isDelete
                }
                rvAdapter.setData(notDeleted)
                localNotes = noteList
            }

            mainViewModel.getNotesRemote.observe(this@MainActivity) {
                when(it){
                    is Resource.Success -> {
                        it.data?.let { notes ->
                            val sync = DataSync.syncNotes(localNotes, notes)
                            mainViewModel.syncToLocal(sync)
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Main Activity", "Loading")
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            buildString {
                                append(getString(R.string.failed_to_sync_notes))
                                append(it.message)
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                        it.message?.let { it1 -> Log.d("okht", it1) }
                    }
                }
            }

            fabAddNote.setOnClickListener {
                val intent = Intent(this@MainActivity, NoteActivity::class.java)
                Intent.FLAG_ACTIVITY_NO_ANIMATION
                val options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@MainActivity,
                        fabAddNote,
                        "shared_element_end_root",
                    )
                activityResultLauncher.launch(intent, options)
            }

            activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (Build.VERSION.SDK_INT >= 33) {
                    if (result.resultCode == RESULT_OK) {
                        val notes = result.data?.getParcelableExtra("notesData", Notes::class.java)
                        notes?.let {
                            rvAdapter.prependItem(it)
                        }
                    }
                } else {
                    if (result.resultCode == RESULT_OK) {
                        val notes = result.data?.getParcelableExtra<Notes>("notesData")
                        notes?.let {
                            rvAdapter.prependItem(it)
                        }
                    }
                }
            }
        }
    }
}