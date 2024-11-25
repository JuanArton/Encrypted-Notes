package com.juanarton.encnotes.ui.activity.main

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.search.SearchBar
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.adapter.GridSpacingItemDecoration
import com.juanarton.encnotes.core.adapter.ItemsDetailsLookup
import com.juanarton.encnotes.core.adapter.ItemsKeyProvider
import com.juanarton.encnotes.core.adapter.NotesAdapter
import com.juanarton.encnotes.core.data.domain.model.NotesPair
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.AttachmentSync
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.core.utils.NoteSync
import com.juanarton.encnotes.databinding.ActivityMainBinding
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import com.juanarton.encnotes.ui.activity.note.NoteActivity
import com.juanarton.encnotes.ui.utils.Utils
import com.ketch.Ketch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var rvAdapter: NotesAdapter
    private var firstRun = true
    private lateinit var tracker: SelectionTracker<String>
    private lateinit var auth: FirebaseAuth
    lateinit var ketch: Ketch
    private var notDeletedNotes: ArrayList<NotesPair> = arrayListOf()

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
        setSupportActionBar(binding?.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        Cryptography.initTink()
        ketch = Ketch.builder().build(this)

        mainViewModel.getNotes()

        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimarySurface, typedValue, true)
        window.statusBarColor = typedValue.data and 0x00FFFFFF or (178 shl 24)

        binding?.apply {
            val listener: (
                NotesPair, MaterialCardView
            ) -> Unit = { notes, materialCardView ->
                val intent = Intent(this@MainActivity, NoteActivity::class.java)
                intent.putExtra("noteData", notes)
                Intent.FLAG_ACTIVITY_NO_ANIMATION
                val options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@MainActivity,
                        materialCardView,
                        "shared_element_end_root",
                    )
                activityResultLauncher.launch(intent, options)
            }

            val staggeredLayout = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            rvNotes.layoutManager = staggeredLayout

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                val tbParams = toolbar.layoutParams as ViewGroup.MarginLayoutParams
                tbParams.topMargin = systemBars.top

                if (rvNotes.itemDecorationCount < 1) {
                    rvNotes.addItemDecoration(
                        GridSpacingItemDecoration(
                            Utils.dpToPx(7, this@MainActivity),
                            tbParams.height + Utils.dpToPx(10, this@MainActivity),
                            systemBars.bottom
                        )
                    )
                }
                val rvParams = rvNotes.layoutParams as FrameLayout.LayoutParams
                rvParams.bottomMargin = systemBars.bottom

                v.setPadding(systemBars.left, 0, systemBars.right, 0)
                insets
            }

            rvAdapter = NotesAdapter(
                listener, mainViewModel.localNotesRepoUseCase, mainViewModel.remoteNotesRepoUseCase, ketch
            )
            rvNotes.adapter = rvAdapter

            setupTracker()

            observeViewModel()

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

            ibDelete.setOnClickListener {
                val selected = rvAdapter.noteList.filter {
                    tracker.selection.contains(it.notes.id)
                }.toMutableList()
                val listNotes = selected.map { it.notes }
                val listAttachment = selected.map { it.attachmentList }

                mainViewModel.deleteNote(listNotes)
                mainViewModel.deleteNoteRemote(listNotes)

                listAttachment.forEach {
                    mainViewModel.deleteAtt(it)
                    mainViewModel.deleteAttRemote(it)
                }

                selected.forEach {
                    val index = notDeletedNotes.indexOfFirst { notes -> notes.notes.id == it.notes.id }
                    rvAdapter.deleteItem(index)
                    notDeletedNotes.removeAt(index)
                }
                tracker.clearSelection()
            }

            ibClose.setOnClickListener {
                tracker.clearSelection()
            }

            onBackPressedDispatcher.addCallback(this@MainActivity) {
                if (tracker.selection.size() > 0) {
                    tracker.clearSelection()
                } else {
                    finish()
                }
            }

            activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val action = result.data?.getStringExtra("action")

                    val note = if (Build.VERSION.SDK_INT >= 33) {
                        result.data?.getParcelableExtra("notesEncrypted", NotesPair::class.java)
                    } else {
                        result.data?.getParcelableExtra("notesEncrypted")
                    }

                    action?.let { act ->
                        note?.let {
                            when (act) {
                                "add" -> {
                                    val decrypted = mainViewModel.decrypt(note.notes)
                                    notDeletedNotes.add(0, note)
                                    rvAdapter.prependItem(NotesPair(decrypted, note.attachmentList))
                                    binding?.rvNotes?.smoothScrollToPosition(0)
                                    note.attachmentList.let { attachment ->
                                        mainViewModel.uploadAttachment(this@MainActivity, attachment)
                                    }
                                    note.notes.let { enc -> mainViewModel.insertNoteRemote(enc)}
                                }
                                "update" -> {
                                    mainViewModel.getNotesBydId(note.notes.id)
                                    val index = notDeletedNotes.indexOfFirst { it.notes.id == note.notes.id }
                                    val upload = note.attachmentList.filterNot {
                                        it in notDeletedNotes[index].attachmentList
                                    }
                                    mainViewModel.uploadAttachment(this@MainActivity, upload)
                                    note.notes.let {enc -> mainViewModel.updateNoteRemote(enc)}
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        mainViewModel.getNotesPair.observe(this@MainActivity) { notesPair ->
            if (firstRun) mainViewModel.getNotesRemote()
            val notDeleted = notesPair.notes.filter {
                !it.isDelete
            }
            val notDeletedAttachment = notesPair.attachmentList.filter {
                !it.isDelete!!
            }

            val notesPairList = notDeleted.map { note ->
                val noteAttachments = notDeletedAttachment.filter { it.noteId == note.id }
                NotesPair(note, noteAttachments as ArrayList)
            }

            notDeletedNotes = notesPairList as ArrayList
            mainViewModel._notDeleted.value = notesPairList
            mainViewModel.decrypt()
        }

        mainViewModel.getNotesRemote.observe(this@MainActivity) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { notes ->
                        firstRun = false
                        val sync = NoteSync.syncNotes(
                            mainViewModel._getNotesPair.value?.notes ?: emptyList(),
                            notes
                        )
                        mainViewModel.syncToLocal(sync)
                        mainViewModel.syncToRemote(sync)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    firstRun = false
                    Toast.makeText(this@MainActivity,
                        buildString {
                            append(getString(R.string.failed_to_sync_notes))
                            append(it.message)
                        }, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mainViewModel.getAttachmentRemote.observe(this@MainActivity) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { attachment ->
                        val sync = AttachmentSync.syncAttachment(
                            mainViewModel._getNotesPair.value?.attachmentList ?: emptyList(),
                            attachment
                        )
                        mainViewModel.syncAttToLocal(sync, this@MainActivity)
                        mainViewModel.syncAttToRemote(sync, this@MainActivity)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    firstRun = false
                    Toast.makeText(this@MainActivity,
                        buildString {
                            append(getString(R.string.failed_to_sync_notes))
                            append(it.message)
                        }, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mainViewModel.uploadAttachment.observe(this@MainActivity) {
            when(it){
                is Resource.Success -> {}
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Toast.makeText(this@MainActivity,
                        buildString {
                            append(getString(R.string.failed_to_sync_notes))
                            append(it.message)
                        }, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mainViewModel.getNoteById.observe(this@MainActivity) { notePair ->
            val index = notDeletedNotes.indexOfFirst { local ->
                local.notes.id == notePair.notes.id
            }
            notDeletedNotes[index] = notePair
            rvAdapter.updateItem(
                index, NotesPair(mainViewModel.decrypt(notePair.notes), notePair.attachmentList)
            )
        }

        mainViewModel.notDeleted.observe(this@MainActivity) { notes ->
            rvAdapter.setData(notes)
            notDeletedNotes = notes as ArrayList<NotesPair>
        }

        mainViewModel.deleteAtt.observe(this) {
            when (it) {
                is Resource.Success -> {
                    it.data?.let { attachment ->
                        mainViewModel.deleteAttFromDisk(attachment, this)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTracker() {
        binding?.apply {
            tracker = SelectionTracker.Builder(
                "selectionItem",
                rvNotes,
                ItemsKeyProvider(rvAdapter),
                ItemsDetailsLookup(rvNotes),
                StorageStrategy.createStringStorage()
            ).withSelectionPredicate(
                SelectionPredicates.createSelectAnything()
            ).build()

            tracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()

                    binding?.apply {
                        if (tracker.selection.size() > 0) {
                            Utils.expandSearchBar(rippleView, searchTopBar, toolbar)
                        } else {
                            Utils.restoreSearchBar(rippleView, searchTopBar, this@MainActivity)
                        }
                    }
                }
            })
            rvAdapter.tracker = tracker
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val spanCount = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        binding?.rvNotes?.layoutManager = StaggeredGridLayoutManager(spanCount, LinearLayoutManager.VERTICAL)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}