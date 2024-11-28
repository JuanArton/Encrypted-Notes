package com.juanarton.encnotes.ui.activity.imagedetail

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.ImageLoader
import com.juanarton.encnotes.databinding.ActivityImageDetailBinding
import com.juanarton.encnotes.ui.utils.Utils
import com.juanarton.encnotes.ui.utils.ZoomPanHelper
import com.ketch.Ketch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImageDetailActivity : AppCompatActivity() {

    private val imageDetailViewModel: ImageDetailViewModel by viewModels()
    private var _binding: ActivityImageDetailBinding? = null
    private val binding get() = _binding
    private lateinit var attachment: Attachment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityImageDetailBinding.inflate(layoutInflater)

        binding?.main?.transitionName = "shared_element_end_root"
        val transform = Utils.buildContainerTransform(this)
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementEnterTransition = transform
        window.sharedElementReturnTransition = transform

        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val attachment = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("attachment", Attachment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("attachment")
        }

        attachment?.let {
            binding?.apply {
                this@ImageDetailActivity.attachment = it

                ZoomPanHelper(ivAttachment)
                val imageLoader = ImageLoader()
                imageLoader.loadImage(
                    this@ImageDetailActivity, it.url, ivAttachment, null,
                    imageDetailViewModel.localNotesRepoUseCase, imageDetailViewModel.remoteNotesRepoUseCase,
                    this@ImageDetailActivity as LifecycleOwner,
                    Ketch.builder().build(this@ImageDetailActivity), null, null
                )

                ibDeleteAtt.setOnClickListener {
                    imageDetailViewModel.deleteAtt(attachment)
                }
            }
        }

        imageDetailViewModel.deleteAtt.observe(this) {
            when (it) {
                is Resource.Success -> {
                    it.data?.let { attachment ->
                        imageDetailViewModel.deleteAttFromDisk(attachment, this)
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        imageDetailViewModel.deleteAttFromDisk.observe(this) {
            if (it) {
                val resultIntent = Intent().apply {
                    putExtra("action", "delete")
                    putExtra("id", attachment?.id)
                }
                setResult(RESULT_OK, resultIntent)
                ActivityCompat.finishAfterTransition(this@ImageDetailActivity)
            }
        }
    }
}