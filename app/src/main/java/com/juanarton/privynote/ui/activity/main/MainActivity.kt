package com.juanarton.privynote.ui.activity.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.juanarton.privynote.R
import com.juanarton.privynote.core.adapter.GridSpacingItemDecoration
import com.juanarton.privynote.core.adapter.ItemsDetailsLookup
import com.juanarton.privynote.core.adapter.ItemsKeyProvider
import com.juanarton.privynote.core.adapter.NotesAdapter
import com.juanarton.privynote.core.data.domain.model.NotesPair
import com.juanarton.privynote.core.data.domain.model.NotesPairRaw
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.utils.AttachmentSync
import com.juanarton.privynote.core.utils.Cryptography
import com.juanarton.privynote.core.utils.NoteSync
import com.juanarton.privynote.databinding.ActivityMainBinding
import com.juanarton.privynote.ui.activity.greeting.GreetingActivity
import com.juanarton.privynote.ui.activity.note.NoteActivity
import com.juanarton.privynote.ui.activity.settings.SettingsActivity
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.APP_SETTINGS
import com.juanarton.privynote.ui.fragment.apppin.AppPinFragment
import com.juanarton.privynote.ui.fragment.apppin.PinCallback
import com.juanarton.privynote.ui.utils.BiometricHelper
import com.juanarton.privynote.ui.utils.FragmentBuilder
import com.juanarton.privynote.ui.utils.Utils
import com.ketch.Ketch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PinCallback {

    private val mainViewModel: MainViewModel by viewModels()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var rvAdapter: NotesAdapter
    private var firstRun = true
    private lateinit var tracker: SelectionTracker<String>
    private lateinit var auth: FirebaseAuth
    lateinit var ketch: Ketch
    private lateinit var localNotes: NotesPairRaw
    private var notDeletedNotes: ArrayList<NotesPair> = arrayListOf()
    private lateinit var listener: (NotesPair, MaterialCardView) -> Unit
    private var authAttempt = 1

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

        val isDarkTheme: Boolean = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }

        val color: Int = Utils.getSurfaceColor(this, isDarkTheme)

        window.statusBarColor = color and 0x00FFFFFF or (178 shl 24)
        window.navigationBarColor = color and 0x00FFFFFF or (0 shl 24)

        mainViewModel.sPref = getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
        mainViewModel.editor = mainViewModel.sPref.edit()

        val currentUser = auth.currentUser
        if (currentUser != null && mainViewModel.getIsLoggedIn()) {
            if (mainViewModel.getBiometric() == true) {
                val biometricHelper = BiometricHelper(
                    activity = this,
                    onSuccess = {
                        initView()
                    },
                    onError = {
                        FragmentBuilder.build(
                            this, AppPinFragment(getString(R.string.please_enter_pin), false, 0), android.R.id.content
                        )
                    },
                    onFailed = {
                        FragmentBuilder.build(
                            this, AppPinFragment(getString(R.string.please_enter_pin), false, 0), android.R.id.content
                        )
                    }
                )
                biometricHelper.showBiometricPrompt(this)
            } else {
                initView()
            }
        } else {
            startActivity(Intent(this, GreetingActivity::class.java))
            finish()
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
                        val index = notDeletedNotes.indexOfFirst { it.notes.id == note.notes.id }
                        when (act) {
                            "add" -> {
                                val decrypted = mainViewModel.decrypt(note.notes)
                                notDeletedNotes.add(0, NotesPair(decrypted, note.attachmentList))
                                rvAdapter.prependItem(NotesPair(decrypted, note.attachmentList))
                                note.attachmentList.let { attachment ->
                                    mainViewModel.uploadAttachment(this@MainActivity, attachment)
                                }
                                note.notes.let { enc -> mainViewModel.insertNoteRemote(enc)}
                                refreshRecyclerView()
                            }
                            "update" -> {
                                mainViewModel.getNotesBydId(note.notes.id)
                                val upload = note.attachmentList.filterNot {
                                    it in notDeletedNotes[index].attachmentList
                                }
                                mainViewModel.uploadAttachment(this@MainActivity, upload)
                                note.notes.let {enc -> mainViewModel.updateNoteRemote(enc)}
                                binding?.rvNotes?.smoothScrollToPosition(index)
                            }
                            "delete" -> {
                                mainViewModel.deleteNote(notDeletedNotes[index].notes)
                                mainViewModel.deleteNoteRemote(notDeletedNotes[index].notes)
                                mainViewModel.deleteAtt(notDeletedNotes[index].attachmentList)
                                mainViewModel.deleteAttRemote(notDeletedNotes[index].attachmentList)
                                rvAdapter.deleteItem(index)
                                notDeletedNotes.removeAt(index)
                                refreshRecyclerView()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun initView() {
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)

        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        Cryptography.initTink()

        ketch = Ketch.builder().build(this)

        mainViewModel.getNotes()

        binding?.apply {
            val photo = auth.currentUser?.photoUrl ?: R.drawable.person
            Utils.loadAvatar(
                this@MainActivity,
                photo,
                this@MainActivity as LifecycleOwner,
                searchTopBar
            )

            searchTopBar.menu.findItem(R.id.profile).setOnMenuItemClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                true
            }

             listener = { notes, materialCardView ->
                val intent = Intent(this@MainActivity, NoteActivity::class.java).apply {
                    putExtra("noteData", notes)
                }
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
                listener, mainViewModel.localNotesRepoUseCase, mainViewModel.remoteNotesRepoUseCase, ketch, this@MainActivity
            )
            rvNotes.adapter = rvAdapter

            rvAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    binding?.apply {
                        rvNotes.post { if (!rvNotes.isComputingLayout) refreshRecyclerView() }
                    }
                }
            })

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
                } else { finish() }
                refreshRecyclerView()
            }

            rvSearchResult.apply {
                val searchAdapter = NotesAdapter(
                    listener, mainViewModel.localNotesRepoUseCase, mainViewModel.remoteNotesRepoUseCase, ketch, this@MainActivity
                )
                if (rvSearchResult.itemDecorationCount < 1) {
                    rvSearchResult.addItemDecoration(
                        GridSpacingItemDecoration(
                            Utils.dpToPx(7, this@MainActivity), Utils.dpToPx(20, this@MainActivity),
                            Utils.dpToPx(20, this@MainActivity)
                        )
                    )
                }
                adapter = searchAdapter
                searchAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onChanged() {
                        super.onChanged()
                        binding?.apply {
                            rvNotes.post { if (!rvNotes.isComputingLayout) refreshRecyclerView() }
                        }
                    }
                })
                layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            }

            searchField.editText.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    runnable?.let(handler::removeCallbacks)
                    runnable = Runnable {
                        val query = s.toString().trim()
                        val filtered = notDeletedNotes.filter {
                            it.notes.notesTitle?.contains(query, true) == true ||
                            it.notes.notesContent?.contains(query, true) == true
                        }
                        if (query == "") {
                            (rvSearchResult.adapter as NotesAdapter).setData(ArrayList(emptyList<NotesPair>()))
                        } else {
                            (rvSearchResult.adapter as NotesAdapter).setData(ArrayList(filtered))
                        }
                    }
                    handler.postDelayed(runnable!!, 500)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun observeViewModel() {
        mainViewModel.getNotesPair.observe(this@MainActivity) { notesPair ->
            if (firstRun) mainViewModel.getNotesRemote()
            val notDeleted = notesPair.notes.filter { !it.isDelete }
            val notDeletedAttachment = notesPair.attachmentList.filter { !it.isDelete!! }

            val notesPairList = notDeleted.map { note ->
                val noteAttachments = notDeletedAttachment.filter { it.noteId == note.id }
                NotesPair(note, noteAttachments as ArrayList)
            }

            localNotes = notesPair
            notDeletedNotes = notesPairList as ArrayList
            mainViewModel._notDeleted.value = notesPairList
            mainViewModel.decrypt()
        }

        mainViewModel.getNotesRemote.observe(this@MainActivity) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { notes ->
                        firstRun = false

                        val sync = NoteSync.syncNotes(localNotes.notes, notes)

                        mainViewModel.syncToLocal(sync)
                        mainViewModel.syncToRemote(sync)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    firstRun = false
                    Toast.makeText(
                        this@MainActivity, Utils.buildString(getString(R.string.failed_to_sync_notes), it.message), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mainViewModel.getAttachmentRemote.observe(this@MainActivity) {
            when(it){
                is Resource.Success -> {
                    it.data?.let { attachment ->
                        val sync = AttachmentSync.syncAttachment(
                            localNotes.attachmentList,
                            attachment
                        )
                        mainViewModel.syncAttToLocal(sync, this@MainActivity)
                        mainViewModel.syncAttToRemote(sync, this@MainActivity)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    firstRun = false
                    Toast.makeText(
                        this@MainActivity, Utils.buildString(getString(R.string.failed_to_sync_notes), it.message), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mainViewModel.uploadAttachment.observe(this@MainActivity) {
            when(it){
                is Resource.Success -> {}
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Toast.makeText(
                        this@MainActivity, Utils.buildString(getString(R.string.failed_to_sync_notes), it.message), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mainViewModel.getNoteById.observe(this@MainActivity) { notePair ->
            val index = notDeletedNotes.indexOfFirst { local -> local.notes.id == notePair.notes.id }
            notDeletedNotes[index] = NotesPair(mainViewModel.decrypt(notePair.notes), notePair.attachmentList)

            rvAdapter.updateItem(
                index, notDeletedNotes[index]
            )
            refreshRecyclerView()
        }

        mainViewModel.notDeleted.observe(this@MainActivity) { notes ->
            rvAdapter.setData(notes)
            notDeletedNotes = notes as ArrayList<NotesPair>
        }

        mainViewModel.deleteAtt.observe(this) {
            when (it) {
                is Resource.Success -> {
                    it.data?.let { attachment -> mainViewModel.deleteAttFromDisk(attachment, this) }
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
            tracker = SelectionTracker.Builder("selectionItem", rvNotes, ItemsKeyProvider(rvAdapter),
                ItemsDetailsLookup(rvNotes), StorageStrategy.createStringStorage()
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

    private fun refreshRecyclerView() {
        Handler(Looper.getMainLooper()).postDelayed({
            val layoutManager = binding?.rvNotes?.layoutManager as StaggeredGridLayoutManager
            binding?.rvNotes?.smoothScrollToPosition(0)
            layoutManager.invalidateSpanAssignments()
        }, 700)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val layoutManager = binding?.rvNotes?.layoutManager as? StaggeredGridLayoutManager
        val spanCount = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        layoutManager?.spanCount = spanCount
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onPinSubmit(pin: Int, action: Int) {
        if (authAttempt <=  5) {
            if (pin == mainViewModel.getAppPin()) {
                initView()
            } else {
                FragmentBuilder.build(
                    this, AppPinFragment(getString(R.string.retry), false, 0), android.R.id.content
                )
            }
        } else {
            Toast.makeText(this, getString(R.string.you_are_not_authorized), Toast.LENGTH_SHORT).show()
            finish()
        }
        authAttempt += 1
    }
}