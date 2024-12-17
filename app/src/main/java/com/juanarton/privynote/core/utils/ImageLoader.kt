package com.juanarton.privynote.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.privynote.ui.activity.main.MainActivity
import com.ketch.Ketch
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File

class ImageLoader {
    fun loadImage(
        context: Context, url: String, imageView: ImageView, background: ImageView?,
        localNotesRepoUseCase: LocalNotesRepoUseCase, remoteNotesRepoUseCase: RemoteNotesRepoUseCase,
        lifecycleOwner: LifecycleOwner, ketch: Ketch, cpiLoading: CircularProgressIndicator?, tvProgress: TextView?,
        forceDimension: Boolean
    ) {
        val key = localNotesRepoUseCase.getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            try {
                val decryptedImage = Cryptography.decrypt(readImage(url, context), deserializedKey)
                showImage(context, imageView, decryptedImage, imageView.width, forceDimension)
                if (background != null) {
                    blurBackground(background, context, decryptedImage, background.width)
                }
            } catch (e: Exception) {
                val fullUrl = buildString {
                    append(MainActivity.baseUrl())
                    append("attachment/images/")
                    append(url)
                }

                lifecycleOwner.lifecycleScope.launch {
                    val id = remoteNotesRepoUseCase.downloadAttachment(fullUrl, ketch)
                    cpiLoading?.visibility = View.VISIBLE
                    tvProgress?.visibility = View.VISIBLE

                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        ketch.observeDownloadById(id)
                            .flowOn(Dispatchers.IO)
                            .collect { downloadModel ->
                                cpiLoading?.progress = downloadModel.progress
                                tvProgress?.text = buildString {
                                    append(downloadModel.progress)
                                    append("%")
                                }
                                if (downloadModel.status.name == "SUCCESS") {
                                    val decryptedImage = Cryptography.decrypt(readImage(url, context), deserializedKey)
                                    showImage(context, imageView, decryptedImage, imageView.width, forceDimension)
                                    if (background != null) {
                                        blurBackground(background, context, decryptedImage, background.width)
                                    }
                                    ketch.clearDb(downloadModel.id, false)
                                    cpiLoading?.visibility = View.INVISIBLE
                                    tvProgress?.visibility = View.INVISIBLE
                                }
                            }
                    }
                }
            }
        }
    }

    private fun showImage(
        context: Context, imageView: ImageView, decryptedImage: ByteArray, width: Int, forceDimension: Boolean
    ) {
        if (forceDimension) {
            val ivParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            imageView.layoutParams = ivParams
        }

        val widthHeight = calculateWidthHeight(decryptedImage, width)
        Glide.with(context)
            .load(decryptedImage)
            .override(widthHeight.first, widthHeight.second)
            .into(imageView)
    }

    private fun blurBackground(
        imageView: ImageView, context: Context, byteArray: ByteArray, width: Int
    ) {
        val ivParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        imageView.layoutParams = ivParams

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
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val file = File("${context.filesDir}"+"/images", url)
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