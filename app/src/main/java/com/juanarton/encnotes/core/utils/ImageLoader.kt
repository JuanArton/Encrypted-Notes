package com.juanarton.encnotes.core.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import com.juanarton.encnotes.databinding.AttachmentItemViewBinding
import com.juanarton.encnotes.ui.activity.main.MainViewModel
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File

class ImageLoader() {
    fun loadImage(
        context: Context, url: String, binding: AttachmentItemViewBinding,
        mainViewModel: MainViewModel, lifecycleOwner: LifecycleOwner
    ) {
        val key = mainViewModel.getCipherKey()
        binding.apply {
            if (!key.isNullOrEmpty()) {
                val deserializedKey = Cryptography.deserializeKeySet(key)

                try {
                    val decryptedImage = Cryptography.decrypt(readImage(url, context), deserializedKey)
                    showImage(context, ivAttachmentImg, decryptedImage, ivAttachmentImg.width)
                    blurBackground(ivAttachmentImgBg, context, decryptedImage, ivAttachmentImgBg.width)
                } catch (e: Exception) {
                    val fullUrl = buildString {
                        append("http://192.168.0.100:5500/attachment/images/")
                        append(url)
                    }

                    lifecycleOwner.lifecycleScope.launch {
                        val id = mainViewModel.downloadAttachment(fullUrl)

                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            mainViewModel.ketch.observeDownloadById(id)
                                .flowOn(Dispatchers.IO)
                                .collect { downloadModel ->
                                    if (downloadModel.status.name == "SUCCESS") {
                                        val decryptedImage = Cryptography.decrypt(readImage(url, context), deserializedKey)
                                        showImage(context, ivAttachmentImg, decryptedImage, ivAttachmentImg.width)
                                        blurBackground(ivAttachmentImgBg, context, decryptedImage, ivAttachmentImgBg.width)
                                        mainViewModel.ketch.clearDb(downloadModel.id, false)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun showImage(
        context: Context, imageView: ImageView, decryptedImage: ByteArray, width: Int
    ) {
        val widthHeight = calculateWidthHeight(decryptedImage, width)
        Glide.with(context)
            .load(decryptedImage)
            .override(widthHeight.first, widthHeight.second)
            .into(imageView)
    }

    private fun blurBackground(
        imageView: ImageView, context: Context, byteArray: ByteArray, width: Int
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Glide.with(context)
                .load(byteArray)
                .transform(BlurTransformation(30))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        } else {
            Glide.with(context)
                .load(byteArray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)

            imageView.setRenderEffect(
                RenderEffect.createBlurEffect(
                    30.0F, 30.0F, Shader.TileMode.CLAMP
                )
            )
        }
    }

    private fun readImage(url: String, context: Context): ByteArray {
        val file = File(context.filesDir, url)
        return file.inputStream().use { inputStream ->
            BufferedInputStream(inputStream).readBytes()
        }
    }

    private fun calculateWidthHeight(byteArray: ByteArray, width: Int): Pair<Int, Int> {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        val imageViewHeight = (width / aspectRatio).toInt()

        return Pair(width, imageViewHeight)
    }
}