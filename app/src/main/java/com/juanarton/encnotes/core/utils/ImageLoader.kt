package com.juanarton.encnotes.core.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.juanarton.encnotes.core.data.api.API
import com.juanarton.encnotes.core.data.api.APIResponse
import com.juanarton.encnotes.core.data.api.ProgressListener
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.ui.activity.main.MainViewModel
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File

object ImageLoader {
    fun loadImage(
        context: Context, url: String, imageView: ImageView, mainViewModel: MainViewModel, lifecycleOwner: LifecycleOwner
    ) {
        val key = mainViewModel.getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            try {
                val decryptedImage = Cryptography.decrypt(readImage(url, context), deserializedKey)
                Glide.with(context)
                    .load(decryptedImage)
                    .into(imageView)
            } catch (e: Exception) {
                val _downladAttachment: MutableLiveData<Resource<Int>> = MutableLiveData()
                val downladAttachment: LiveData<Resource<Int>> = _downladAttachment

                val fullUrl = buildString {
                    append("http://192.168.0.100:5500/attachment/images/")
                    append(url)
                }

                lifecycleOwner.lifecycleScope.launch {
                    API.progressListener = object : ProgressListener {
                        override fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
                            val downloaded = if (contentLength > 0) {
                                ((bytesRead * 100) / contentLength).toInt()
                            } else 0
                            Log.d("test", downloaded.toString())
                        }
                    }

                    mainViewModel.downloadAttachment(fullUrl, true).collect {
                        _downladAttachment.value = it
                    }

                    downladAttachment.observe(lifecycleOwner) {
                        when(it){
                            is Resource.Success -> {
                                it.data?.let { progress ->
                                    Log.d("test10", progress.toString())
                                    if (progress == 100) {
                                        val retryDecrypt = Cryptography.decrypt(readImage(url, context), deserializedKey)
                                        Glide.with(context)
                                            .load(retryDecrypt)
                                            .into(imageView)
                                    }
                                }
                            }
                            is Resource.Loading -> {
                                Log.d("Main Activity", "Loading")
                            }
                            is Resource.Error -> {
                                Log.d("test8", it.message.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readImage(url: String, context: Context): ByteArray {
        val file = File(context.filesDir, url)
        Log.d("test13", file.toString())
        return file.inputStream().use { inputStream ->
            BufferedInputStream(inputStream).readBytes()
        }
    }
}