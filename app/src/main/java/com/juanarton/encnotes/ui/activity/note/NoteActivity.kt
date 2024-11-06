package com.juanarton.encnotes.ui.activity.note

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Window
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.databinding.ActivityNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Date

@AndroidEntryPoint
class NoteActivity : AppCompatActivity() {

    private var _binding: ActivityNoteBinding? = null
    private val binding get() = _binding
    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private var id = NanoId.generate(16)
    private lateinit var notes: Notes
    private var time = Date().time
    private var isBackpresed = false
    private var act = "add"
    private var initTitle = ""
    private var initContent = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enableEdgeToEdge()
        _binding = ActivityNoteBinding.inflate(layoutInflater)

        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null

        binding?.main?.transitionName = "shared_element_end_root"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementEnterTransition = buildContainerTransform()
        window.sharedElementReturnTransition = buildContainerTransform()

        setContentView(binding?.root)
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth

        initNoteData()

        onBackPressedDispatcher.addCallback(this) { handleBackPress() }

        observeNoteState()

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
        }
    }

    private fun initNoteData() {
        val notesTmp = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("noteData", Notes::class.java)
        } else {
            intent.getParcelableExtra("noteData")
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
    }

    private fun handleBackPress() {
        binding?.apply {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()

            if (act == "add") {
                if (title.isBlank() && content.isBlank()) {
                    noteViewModel.permanentDelete(id)
                    ActivityCompat.finishAfterTransition(this@NoteActivity)
                }
                else if (title.isNotBlank() || content.isNotBlank()) {
                    setResult(etTitle.text.toString(), etContent.text.toString())
                }
                else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
            }
            else if (act == "update") {
                if (title != initTitle || content != initContent) {
                    setResult(etTitle.text.toString(), etContent.text.toString())
                }
                else { ActivityCompat.finishAfterTransition(this@NoteActivity) }
            }
        }
        isBackpresed = true
    }

    private fun setResult (title: String, content: String) {
        if (title != initTitle || content != initContent) {
            val resultIntent = Intent().apply {
                putExtra(
                    "notesData",
                    Notes(
                        id,
                        title,
                        content,
                        false,
                        time
                    )
                )
                putExtra("notesEncrypted", notes)
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

    private fun observeNoteState() {
        noteViewModel.addNoteRemote.observe(this) {
            when(it){
                is Resource.Success -> {
                    Toast.makeText(
                        this@NoteActivity,
                        getString(R.string.notes_saved_in_cloud),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is Resource.Loading -> {
                    Log.d("Note Activity1", "Loading")
                }
                is Resource.Error -> {
                    Toast.makeText(
                        this@NoteActivity,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        noteViewModel.updateNoteLocal.observe(this) { observeNoteResource(it) }
        noteViewModel.addNoteLocal.observe(this) { observeNoteResource(it) }
    }

    private fun observeNoteResource(resource: Resource<*>) {
        when (resource) {
            is Resource.Success -> {}
            is Resource.Loading -> {
                Log.d("Note Activity", "Loading")
            }
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

    /*@OptIn(DelicateCoroutinesApi::class)
    override fun onDestroy() {
        super.onDestroy()

        if (::notes.isInitialized) {
            val weakViewModel = WeakReference(noteViewModel)
            GlobalScope.launch(Dispatchers.IO) {
                weakViewModel.get()?.let { viewModel ->
                    when (act) {
                        "add" -> viewModel.insertNoteRemote(notes)
                        "update" -> viewModel.updateNoteRemote(notes)
                    }
                }
            }
        }
    }*/
}