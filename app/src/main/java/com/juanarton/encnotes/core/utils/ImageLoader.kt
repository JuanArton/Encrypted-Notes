package com.juanarton.encnotes.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.databinding.AttachmentItemViewBinding
import com.ketch.Ketch
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File

class ImageLoader {
    fun loadImage(
        context: Context, url: String, binding: AttachmentItemViewBinding,
        localNotesRepoUseCase: LocalNotesRepoUseCase, remoteNotesRepoUseCase: RemoteNotesRepoUseCase,
        lifecycleOwner: LifecycleOwner, ketch: Ketch
    ) {
        val key = localNotesRepoUseCase.getCipherKey()
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
                        val id = remoteNotesRepoUseCase.downloadAttachment(fullUrl, ketch)

                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            ketch.observeDownloadById(id)
                                .flowOn(Dispatchers.IO)
                                .collect { downloadModel ->
                                    if (downloadModel.status.name == "SUCCESS") {
                                        val decryptedImage = Cryptography.decrypt(readImage(url, context), deserializedKey)
                                        showImage(context, ivAttachmentImg, decryptedImage, ivAttachmentImg.width)
                                        blurBackground(ivAttachmentImgBg, context, decryptedImage, ivAttachmentImgBg.width)
                                        ketch.clearDb(downloadModel.id, false)
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
        val widthHeight = calculateWidthHeight(byteArray, width)
        val transparentImage = transparentImage(byteArray)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Glide.with(context)
                .load(transparentImage)
                .override(widthHeight.first, widthHeight.second)
                .transform(BlurTransformation(30), CenterCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        } else {
            Glide.with(context)
                .load(transparentImage)
                .override(widthHeight.first, widthHeight.second)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(imageView)

            imageView.setRenderEffect(
                RenderEffect.createBlurEffect(
                    60.0F, 60.0F, Shader.TileMode.CLAMP
                )
            )
        }
    }

    private fun transparentImage(byteArray: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        val width = bitmap.width
        val height = bitmap.height
        val transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(transparentBitmap)
        val paint = Paint()
        paint.alpha = (0.3 * 255).toInt()
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return transparentBitmap
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