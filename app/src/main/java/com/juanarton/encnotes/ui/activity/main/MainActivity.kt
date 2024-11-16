package com.juanarton.encnotes.ui.activity.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityMainBinding
import com.juanarton.encnotes.ui.activity.login.LoginActivity
import com.juanarton.encnotes.ui.activity.note.NoteActivity
import com.juanarton.encnotes.ui.utils.AttachmentSync
import com.juanarton.encnotes.ui.utils.NoteSync
import com.juanarton.encnotes.ui.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActionMode.Callback {

    private val mainViewModel: MainViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var rvAdapter: NotesAdapter
    private var localNotes: ArrayList<Notes> = arrayListOf()
    private var notDeletedNotes: ArrayList<Notes> = arrayListOf()

    private var localAttachment: ArrayList<Attachment> = arrayListOf()
    private var notDeleteAttachment: ArrayList<Attachment> = arrayListOf()

    private var firstRun = true
    private lateinit var tracker: SelectionTracker<String>
    private var actionMode: ActionMode? = null
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

        binding?.apply {
            val listener: (Notes, MaterialCardView) -> Unit = { notes, materialCardView ->
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

            rvNotes.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            rvNotes.addItemDecoration(GridSpacingItemDecoration(Utils.dpToPx(7, this@MainActivity)))
            rvAdapter = NotesAdapter(listener, mainViewModel)
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

            mainViewModel.getNotes.observe(this@MainActivity) { noteList ->
                if (firstRun) mainViewModel.getNotesRemote()
                val notDeleted = noteList.filter {
                    !it.isDelete
                }
                localNotes = noteList as ArrayList<Notes>
                mainViewModel._notDeleted.value = notDeleted
                mainViewModel.decrypt()
            }

            mainViewModel.getAttachment.observe(this@MainActivity) { attachments ->
                val notDeleted = attachments.filter { !it.isDelete!! }
                notDeleteAttachment = notDeleted as ArrayList<Attachment>
                Log.d("test3", notDeleteAttachment.toString())
                localAttachment = attachments as ArrayList<Attachment>
                mainViewModel._notDeletedAtt.value = notDeleted
            }

            mainViewModel.getNotesRemote.observe(this@MainActivity) {
                when(it){
                    is Resource.Success -> {
                        it.data?.let { notes ->
                            firstRun = false
                            val sync = NoteSync.syncNotes(localNotes, notes)
                            mainViewModel.syncToLocal(sync)
                            mainViewModel.syncToRemote(sync)
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Main Activity", "Loading")
                    }
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
                            val sync = AttachmentSync.syncAttachment(localAttachment, attachment)
                            mainViewModel.syncAttToLocal(sync)
                            //mainViewModel.syncToRemote(sync)
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Main Activity", "Loading")
                    }
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

            mainViewModel.notDeleted.observe(this@MainActivity) { notes ->
                Log.d("test2", notDeleteAttachment.toString())
                rvAdapter.setData(notes, notDeleteAttachment)
                notDeletedNotes = notes as ArrayList<Notes>
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
                    val encryptedNote = if (Build.VERSION.SDK_INT >= 33) {
                        result.data?.getParcelableExtra("notesEncrypted", Notes::class.java)
                    } else {
                        result.data?.getParcelableExtra("notesEncrypted")
                    }

                    val note = if (Build.VERSION.SDK_INT >= 33) {
                        result.data?.getParcelableExtra("notesData", Notes::class.java)
                    } else {
                        result.data?.getParcelableExtra("notesData")
                    }
                    action?.let { act ->
                        note?.let {
                            when (act) {
                                "add" -> {
                                    rvAdapter.prependItem(it)
                                    binding?.rvNotes?.smoothScrollToPosition(0)
                                    notDeletedNotes.add(0, note)
                                    encryptedNote?.let { enc -> mainViewModel.insertNoteRemote(enc)}
                                }
                                "update" -> {
                                    val index = notDeletedNotes.indexOfFirst { local -> local.id == note.id }
                                    notDeletedNotes[index] = note
                                    rvAdapter.updateItem(index, note)
                                    encryptedNote?.let {enc -> mainViewModel.updateNoteRemote(enc)}
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
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
                    tracker.selection.contains(it.id)
                }.toMutableList()

                mainViewModel.deleteNote(selected)
                mainViewModel.deleteNoteRemote(selected)

                selected.forEach {
                    val index = notDeletedNotes.indexOfFirst { notes -> notes.id == it.id }
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
}