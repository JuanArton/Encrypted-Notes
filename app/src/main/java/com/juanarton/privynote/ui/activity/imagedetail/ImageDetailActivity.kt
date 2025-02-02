package com.juanarton.privynote.ui.activity.imagedetail

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.utils.ImageLoader
import com.juanarton.privynote.databinding.ActivityImageDetailBinding
import com.juanarton.privynote.ui.utils.Utils
import com.juanarton.privynote.ui.utils.ZoomPanHelper
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

            binding?.apply {
                val tbParams = toolbar.layoutParams as ViewGroup.MarginLayoutParams
                tbParams.topMargin = systemBars.top

                bottomMenu.setPadding(0, 0, 0, systemBars.bottom)
            }

            v.setPadding(systemBars.left, 0, systemBars.right, 0)
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
                    Ketch.builder().build(this@ImageDetailActivity), null, null, false
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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}