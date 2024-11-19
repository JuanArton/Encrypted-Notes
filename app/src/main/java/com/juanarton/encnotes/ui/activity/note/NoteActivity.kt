package com.juanarton.encnotes.ui.activity.note

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.transition.Transition
import android.util.Log
import android.util.TypedValue
import android.view.Window
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.adapter.AttachmentAdapter
import com.juanarton.encnotes.core.adapter.GridSpacingItemDecoration
import com.juanarton.encnotes.core.adapter.NotesAdapter
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityNoteBinding
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
    private lateinit var notes: Notes
    private var time = Date().time
    private var act = "add"
    private var initTitle = ""
    private var initContent = ""
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private var newAttachment: ArrayList<Attachment> = arrayListOf()
    private lateinit var rvAdapter: AttachmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enableEdgeToEdge()
        _binding = ActivityNoteBinding.inflate(layoutInflater)

        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        binding?.main?.transitionName = "shared_element_end_root"
        val transform = buildContainerTransform()
        transform.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition?) {}
            override fun onTransitionEnd(transition: Transition?) {
                val typedValue = TypedValue()
                this@NoteActivity.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerLow, typedValue, true)
                val window: Window = this@NoteActivity.window
                window.navigationBarColor = typedValue.data
            }
            override fun onTransitionCancel(transition: Transition?) {}
            override fun onTransitionPause(transition: Transition?) {}
            override fun onTransitionResume(transition: Transition?) {}
        })
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementEnterTransition = transform
        window.sharedElementReturnTransition = transform

        setContentView(binding?.root)
        super.onCreate(savedInstanceState)

        initNoteData()

        onBackPressedDispatcher.addCallback(this) { handleBackPress() }

        observeViewModel()

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
                    runnable?.let { handler.postDelayed(it, 500) }
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
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    noteViewModel.addAtt(it, contentResolver, this)
                }
            }
        }
    }

    private fun initNoteData() {
        val notesTmp = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("noteData", Notes::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("noteData")
        }

        val attachments: ArrayList<Attachment>? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra("attachmentData", Attachment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("attachmentData")
        }

        notesTmp?.let {
            notes = it
            act = "update"
            binding?.apply {
                etTitle.setText(notes.notesTitle)
                etContent.setText(notes.notesContent)
            }
            id = it.id
            initContent = it.notesContent.toString()
            initTitle = it.notesTitle.toString()
        }

        attachments?.let {
            binding?.apply {
                val span = if (attachments.size < 3 || attachments.size == 4) attachments.size else 3

                if (attachments.size < 3) {
                    rvImgAttachment.layoutManager = GridLayoutManager(this@NoteActivity, span)
                } else if (attachments.size == 4) {
                    rvImgAttachment.layoutManager = GridLayoutManager(this@NoteActivity, 2)
                } else if (attachments.size == 3) {
                    rvImgAttachment.layoutManager = FlexboxLayoutManager(this@NoteActivity).apply {
                        flexDirection = FlexDirection.ROW
                        maxLine = 2
                    }
                } else {
                    rvImgAttachment.layoutManager = StaggeredGridLayoutManager(span, LinearLayoutManager.VERTICAL)
                }

                if (attachments.size > 1) {
                    rvImgAttachment.addItemDecoration(GridSpacingItemDecoration(Utils.dpToPx(5, this@NoteActivity)))
                }

                rvAdapter = AttachmentAdapter(
                    noteViewModel.localNotesRepoUseCase, noteViewModel.remoteNotesRepoUseCase,
                    Ketch.builder().build(this@NoteActivity)
                )
                rvImgAttachment.adapter = rvAdapter
                rvAdapter.setData(it)
            }
        }
    }

    private fun handleBackPress() {
        binding?.apply {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            if (act == "add") {
                if (title.isBlank() && content.isBlank() && newAttachment.size == 0) {
                    noteViewModel.permanentDelete(id)
                    ActivityCompat.finishAfterTransition(this@NoteActivity)
                }
                else if (title.isNotBlank() || content.isNotBlank() || newAttachment.size != 0) {
                    setResult(etTitle.text.toString(), etContent.text.toString())
                }
                else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
            }
            else if (act == "update") {
                if (title != initTitle || content != initContent || newAttachment.size != 0) {
                    setResult(etTitle.text.toString(), etContent.text.toString())
                }
                else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
            }
        }
    }

    private fun setResult (title: String, content: String) {
        if (title != initTitle || content != initContent || newAttachment.size != 0) {
            val resultIntent = Intent().apply {
                putExtra(
                    "notesData",
                    Notes(id, title, content, false, time)
                )
                putExtra("notesEncrypted", notes)
                putExtra("newAtt", newAttachment)
                putExtra("action", act)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            ActivityCompat.finishAfterTransition(this@NoteActivity)
        } else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
    }

    private fun handleSaveNote(title: String, content: String) {
        time = Date().time
        val key = noteViewModel.getCipherKey()
        val ownerId = auth.uid

        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)
            if (ownerId != null) {
                if (::notes.isInitialized) {
                    id = notes.id
                    notes = Notes(
                        notes.id,
                        Cryptography.encrypt(title, deserializedKey),
                        Cryptography.encrypt(content, deserializedKey),
                        false,
                        time
                    )
                    noteViewModel.updateNoteLocal(notes)
                } else {
                    notes = Notes(
                        id,
                        Cryptography.encrypt(title, deserializedKey),
                        Cryptography.encrypt(content, deserializedKey),
                        false,
                        time
                    )
                    noteViewModel.insertNote(notes)
                }
            } else {
                Toast.makeText(
                    this@NoteActivity,
                    buildString {
                        append(getString(R.string.unable_add_note))
                        append(" : ")
                        append(getString(R.string.empty_uid))
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this@NoteActivity,
                getString(R.string.unable_retrieve_key),
                Toast.LENGTH_SHORT
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
                } else notes.id
                binding?.apply {
                    handleSaveNote(etTitle.text.toString(), etContent.text.toString())
                }
                noteViewModel.insertAtt(
                    Attachment(
                        NanoId.generate(16), id, it.second, false, Date().time
                    )
                )
            }
        }

        noteViewModel.insertAtt.observe(this) {
            when (it) {
                is Resource.Success -> {
                    it.data?.let { it1 -> newAttachment.add(0, it1) }
                }
                is Resource.Loading -> {
                    Log.d("Note Activity", "Loading")
                }
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

    private fun buildContainerTransform(): MaterialContainerTransform {
        val typedValue = TypedValue()
        val theme = this.theme
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)

        val color: Int = if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            typedValue.data
        } else {
            val drawable = ContextCompat.getDrawable(this, typedValue.resourceId)
            (drawable as? ColorDrawable)?.color ?: Color.TRANSPARENT
        }

        return MaterialContainerTransform().apply {
            addTarget(R.id.main)
            scrimColor = color
            containerColor = Color.TRANSPARENT
            endContainerColor = color
            startContainerColor = color
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}