package com.juanarton.encnotes.ui.activity.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.card.MaterialCardView
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
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityMainBinding
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import com.juanarton.encnotes.ui.activity.note.NoteActivity
import com.juanarton.encnotes.ui.utils.AttachmentSync
import com.juanarton.encnotes.ui.utils.NoteSync
import com.juanarton.encnotes.ui.utils.Utils
import com.ketch.Ketch
import com.lopei.collageview.CollageView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActionMode.Callback {

    private val mainViewModel: MainViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var rvAdapter: NotesAdapter
    private var firstRun = true
    private lateinit var tracker: SelectionTracker<String>
    private var actionMode: ActionMode? = null
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Cryptography.initTink()
        ketch = Ketch.builder().build(this)

        mainViewModel.getNotes()

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
            rvNotes.addItemDecoration(GridSpacingItemDecoration(Utils.dpToPx(7, this@MainActivity)))
            rvAdapter = NotesAdapter(
                listener, mainViewModel.localNotesRepoUseCase, mainViewModel.remoteNotesRepoUseCase, ketch
            )
            rvNotes.adapter = rvAdapter

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
                        if (actionMode == null) {
                            actionMode = startSupportActionMode(this@MainActivity)
                        }
                        val items = tracker.selection.size()
                        if (items > 0) {
                            actionMode?.title = "$items Selected"
                        } else {
                            actionMode?.finish()
                        }
                    }
                }
            )
            rvAdapter.tracker = tracker

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
                            mainViewModel.syncAttToLocal(sync)
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
                                    val attList = notDeletedNotes[index].attachmentList
                                    val upload = note.attachmentList.filterNot {
                                        it in notDeletedNotes[index].attachmentList
                                    }
                                    val delete = attList.filterNot { it in note.attachmentList }
                                    mainViewModel.uploadAttachment(this@MainActivity, upload)
                                    note.notes.let {enc -> mainViewModel.updateNoteRemote(enc)}
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            val url = listOf(
                "https://images.unsplash.com/photo-1542396601-dca920ea2807?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxleHBsb3JlLWZlZWR8Mnx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1542379653-b928db1b4956?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxleHBsb3JlLWZlZWR8M3x8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1521109464564-2fa2faa95858?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxleHBsb3JlLWZlZWR8Nnx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1542461927-dd68c85adc56?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxleHBsb3JlLWZlZWR8OHx8fGVufDB8fHx8fA%3D%3D",
                "https://images.unsplash.com/photo-1542640244-7e672d6cef4e?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxleHBsb3JlLWZlZWR8MTR8fHxlbnwwfHx8fHw%3D"
            )
            collageView
                .defaultPhotosForLine(3)
                .useFirstAsHeader(false)
                .loadPhotos(url)
        }
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.select_menu_item, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.note_delete -> {
                val selected = rvAdapter.noteList.filter {
                    tracker.selection.contains(it.notes.id)
                }.toMutableList()

                //mainViewModel.deleteNote(selected)
                //mainViewModel.deleteNoteRemote(selected)

                selected.forEach {
                    val index = notDeletedNotes.indexOfFirst { notes -> notes.notes.id == it.notes.id }
                    rvAdapter.deleteItem(index)
                    notDeletedNotes.removeAt(index)
                }

                tracker.clearSelection()

                true
            }
            else -> {
                false
            }
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        tracker.clearSelection()
        actionMode = null
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