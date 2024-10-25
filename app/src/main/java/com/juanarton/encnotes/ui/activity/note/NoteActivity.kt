package com.juanarton.encnotes.ui.activity.note

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Window
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class NoteActivity : AppCompatActivity() {

    private var _binding: ActivityNoteBinding? = null
    private val binding get() = _binding
    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enableEdgeToEdge()
        _binding = ActivityNoteBinding.inflate(layoutInflater)

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

        onBackPressedDispatcher.addCallback(this) {
            handleBackpress()
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

    private fun handleBackpress() {
        lifecycleScope.launch {
            binding?.apply {
                val ownerId = auth.uid
                val title = etTitle.text.toString()
                val content = etContent.text.toString()

                if (ownerId != null && content.isNotBlank()) {
                    val notes = Notes(
                        NanoId.generate(16),
                        ownerId,
                        title,
                        content,
                        false,
                        Date().time
                    )

                    val result = noteViewModel.insertNote(
                        notes
                    )
                    when(result){
                        is Resource.Success -> {
                            val resultIntent = Intent().apply {
                                putExtra("notesData", notes)
                            }
                            setResult(RESULT_OK, resultIntent)
                            this@NoteActivity.finish()
                        }
                        is Resource.Loading -> {
                            Log.d("Note Activity", "Loading")
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                this@NoteActivity,
                                result.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                if (ownerId == null){
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
            }
        }
    }
}