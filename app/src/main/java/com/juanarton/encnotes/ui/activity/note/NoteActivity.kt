package com.juanarton.encnotes.ui.activity.note

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.adapter.AttachmentAdapter
import com.juanarton.encnotes.core.adapter.GridSpacingItemDecoration
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.model.NotesPair
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityNoteBinding
import com.juanarton.encnotes.ui.activity.imagedetail.ImageDetailActivity
import com.juanarton.encnotes.ui.utils.Utils
import com.ketch.Ketch
import dagger.hilt.android.AndroidEntryPoint
import io.viascom.nanoid.NanoId
import java.util.Date

@AndroidEntryPoint
class NoteActivity : AppCompatActivity() {

    private var _binding: ActivityNoteBinding? = null
    private val binding get() = _binding
    private val noteViewModel: NoteViewModel by viewModels()
    private var auth = Firebase.auth
    private var id = NanoId.generate(16)
    private lateinit var notesPair: NotesPair
    private var time = Date().time
    private var act = "add"
    private var initTitle = ""
    private var initContent = ""
    private var initAttachment: MutableList<Attachment> = arrayListOf()
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var rvAdapter: AttachmentAdapter
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enableEdgeToEdge()
        _binding = ActivityNoteBinding.inflate(layoutInflater)

        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        binding?.main?.transitionName = "shared_element_end_root"
        val transform = Utils.buildContainerTransform(this)

        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementEnterTransition = transform
        window.sharedElementReturnTransition = transform

        setContentView(binding?.root)
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding?.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null

        initNoteData()

        onBackPressedDispatcher.addCallback(this) { handleBackPress() }

        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding?.apply {
                val rlParams = rlBottomTool.layoutParams as ViewGroup.MarginLayoutParams
                val tbParams = toolbar.layoutParams as ViewGroup.MarginLayoutParams
                val rvParams = rvImgAttachment.layoutParams as ViewGroup.MarginLayoutParams
                tbParams.topMargin = systemBars.top
                if (isKeyboardVisible) {
                    rlParams.bottomMargin = imeInsets.bottom
                    rlBottomTool.setPadding(
                        Utils.dpToPx(10, this@NoteActivity), 0, Utils.dpToPx(10, this@NoteActivity), 0
                    )
                } else {
                    rlParams.bottomMargin = 0
                    rlBottomTool.setPadding(
                        Utils.dpToPx(10, this@NoteActivity), 0, Utils.dpToPx(10, this@NoteActivity), systemBars.bottom
                    )
                }
                rvParams.topMargin = -systemBars.top
            }
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        binding?.apply {
            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    runnable?.let { handler.removeCallbacks(it) }
                    runnable = Runnable {
                        handleSaveNote(
                            etTitle.text.toString(),
                            etContent.text.toString()
                        )
                    }
                    runnable.let { handler.postDelayed(it, 500) }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }

            etContent.addTextChangedListener(textWatcher)
            etTitle.addTextChangedListener(textWatcher)

            ibAdd.setOnClickListener {
                selectImage()
            }
        }

        selectImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let { noteViewModel.addAtt(it, contentResolver, this) }
            }
        }
    }

    private fun initNoteData() {
        val listener: (Attachment, ImageView) -> Unit = { attachment, imageView ->
            val intent = Intent(this@NoteActivity, ImageDetailActivity::class.java)
            intent.putExtra("attachment", attachment)
            Intent.FLAG_ACTIVITY_NO_ANIMATION
            val options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this@NoteActivity,
                    imageView,
                    "shared_element_end_root",
                )
            activityResultLauncher.launch(intent, options)
        }

        rvAdapter = AttachmentAdapter(
            listener, noteViewModel.localNotesRepoUseCase, noteViewModel.remoteNotesRepoUseCase,
            Ketch.builder().build(this@NoteActivity), this@NoteActivity
        )

        binding?.apply {
            val span = if (::notesPair.isInitialized) calculateSpan() else 1
            rvImgAttachment.layoutManager = GridLayoutManager(this@NoteActivity, span)
            rvImgAttachment.adapter = rvAdapter
        }

        val notesTmp = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("noteData", NotesPair::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("noteData")
        }

        notesTmp?.let {
            notesPair = it
            act = "update"
            id = it.notes.id
            initContent = it.notes.notesContent.toString()
            initTitle = it.notes.notesTitle.toString()
            binding?.apply {
                etTitle.setText(notesPair.notes.notesTitle)
                etContent.setText(notesPair.notes.notesContent)
                tvEditedAt.text = Utils.parseTimeToDate(it.notes.lastModified, this@NoteActivity)
                rvImgAttachment.layoutManager = GridLayoutManager(this@NoteActivity, calculateSpan())
                rvImgAttachment.adapter = rvAdapter
            }
            initAttachment.addAll(it.attachmentList)
            rvAdapter.setData(it.attachmentList.asReversed())
        }

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val action = result.data?.getStringExtra("action")
                val id = result.data?.getStringExtra("id")

                action?.let { act ->
                    id?.let {
                        when (act) {
                            "delete" -> {
                                val previousSpan = calculateSpan()
                                val index = notesPair.attachmentList.asReversed().indexOfFirst { it.id == id }
                                notesPair.attachmentList.removeAt(index)
                                val newSpan = calculateSpan()
                                binding?.apply {
                                    handleSaveNote(etTitle.text.toString(), etContent.text.toString())
                                    val layoutManager = binding?.rvImgAttachment?.layoutManager as GridLayoutManager
                                    layoutManager.spanCount = calculateSpan()
                                    if (previousSpan != newSpan) {
                                        rvAdapter.deleteData(index)
                                        rvAdapter.notifyDataSetChanged()
                                    }
                                }
                                noteViewModel.deleteAttRemote(id)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun handleBackPress() {
        binding?.apply {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            if (act == "add") {
                if (title.isBlank() && content.isBlank() && !::notesPair.isInitialized) {
                    ActivityCompat.finishAfterTransition(this@NoteActivity)
                }
                else if (title.isNotBlank() || content.isNotBlank() || notesPair.attachmentList.isNotEmpty()) {
                    setResult()
                }
                else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
            }
            else if (act == "update") {
                if (title != initTitle || content != initContent || notesPair.attachmentList != initAttachment) {
                    setResult()
                }
                else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
            }
        }
    }

    private fun setResult () {
        val resultIntent = Intent().apply {
            putExtra("notesEncrypted", notesPair)
            putExtra("action", act)
        }
        setResult(RESULT_OK, resultIntent)
        ActivityCompat.finishAfterTransition(this@NoteActivity)
    }

    private fun handleSaveNote(title: String, content: String) {
        time = Date().time
        val key = noteViewModel.getCipherKey()
        val ownerId = auth.uid

        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)
            if (ownerId != null) {
                if (::notesPair.isInitialized) {
                    id = notesPair.notes.id
                    notesPair.notes = Notes(
                        notesPair.notes.id,
                        Cryptography.encrypt(title, deserializedKey),
                        Cryptography.encrypt(content, deserializedKey),
                        false,
                        time
                    )
                    noteViewModel.updateNoteLocal(notesPair.notes)
                } else {
                    val notes = Notes(
                        id,
                        Cryptography.encrypt(title, deserializedKey),
                        Cryptography.encrypt(content, deserializedKey),
                        false,
                        time
                    )
                    notesPair = NotesPair(notes, arrayListOf())
                    noteViewModel.insertNote(notesPair.notes)
                }
                binding?.tvEditedAt?.text = Utils.parseTimeToDate(notesPair.notes.lastModified, this@NoteActivity)
            } else {
                Toast.makeText(
                    this@NoteActivity, Utils.buildString(getString(R.string.unable_add_note), " : ", getString(R.string.empty_uid)), Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this@NoteActivity, getString(R.string.unable_retrieve_key), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        noteViewModel.updateNoteLocal.observe(this) { observeNoteResource(it) }
        noteViewModel.addNoteLocal.observe(this) { observeNoteResource(it) }
        noteViewModel.addAtt.observe(this) {
            if (it.first) {
                var id = ""
                id = if (act == "add") {
                    this.id
                } else notesPair.notes.id
                binding?.apply {
                    handleSaveNote(etTitle.text.toString(), etContent.text.toString())
                }
                noteViewModel.insertAtt(
                    Attachment(NanoId.generate(16), id, it.second, false, "image", Date().time)
                )
            }
        }

        noteViewModel.insertAtt.observe(this) {
            when (it) {
                is Resource.Success -> {
                    it.data?.let { attachment ->
                        val previousSpan = calculateSpan()
                        notesPair.attachmentList.add(0, attachment)
                        if(notesPair.attachmentList.isNotEmpty()) {
                            binding?.apply {
                                val newSpan = calculateSpan()
                                val layoutManager = rvImgAttachment.layoutManager as GridLayoutManager
                                layoutManager.spanCount = newSpan
                                if (previousSpan != newSpan) {
                                    rvAdapter.addData(attachment)
                                    rvAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Toast.makeText(this@NoteActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeNoteResource(resource: Resource<*>) {
        when (resource) {
            is Resource.Success -> {}
            is Resource.Loading -> {}
            is Resource.Error -> {
                Toast.makeText(this@NoteActivity, resource.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateSpan(): Int {
        binding?.apply {
            val gridSpacingDecoration = GridSpacingItemDecoration(Utils.dpToPx(3, this@NoteActivity), 0, 0)
            if (notesPair.attachmentList.size > 1) {
                rvImgAttachment.addItemDecoration(gridSpacingDecoration)
            } else {
                rvImgAttachment.removeItemDecoration(gridSpacingDecoration)
            }
        }
        return when (notesPair.attachmentList.size) {
            in 1..3 -> notesPair.attachmentList.size
            4 -> 2
            else -> 3
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.select_menu_item, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                ActivityCompat.finishAfterTransition(this@NoteActivity)
                true
            }
            R.id.note_delete -> {
                act = "delete"
                setResult()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}