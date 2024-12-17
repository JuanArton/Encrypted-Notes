package com.juanarton.encnotes.ui.utils

import android.animation.ValueAnimator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.search.SearchBar
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.juanarton.encnotes.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Utils {
    fun dpToPx(dp: Int, context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun buildContainerTransform(context: Context): MaterialContainerTransform {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)

        val color: Int = if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            typedValue.data
        } else {
            val drawable = ContextCompat.getDrawable(context, typedValue.resourceId)
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

    fun expandSearchBar(
        ripple: MaterialCardView, searchBar: SearchBar, toolbar: MaterialToolbar
    ) {
        val toolbarWidth = toolbar.width
        val toolbarHeight = toolbar.height

        ripple.visibility = View.VISIBLE
        searchBar.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                searchBar.visibility = View.INVISIBLE
            }
            .start()

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float

            val newRippleWidth = (ripple.width + (toolbarWidth - ripple.width) * progress).toInt()
            val newRippleHeight = (ripple.height + (toolbarHeight - ripple.height) * progress).toInt()

            ripple.layoutParams = ripple.layoutParams.apply {
                width = newRippleWidth
                height = newRippleHeight
            }
            ripple.requestLayout()

            if (progress == 1F) {
                ripple.shapeAppearanceModel = ripple.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(0F)
                    .build()
            }
        }
        animator.duration = 300
        animator.start()
    }

    fun restoreSearchBar(
        ripple: MaterialCardView, searchBar: SearchBar, context: Context,
    ) {
        val originalRippleWidth = searchBar.width
        val originalRippleHeight = dpToPx(50, context)
        searchBar.alpha = 0f
        searchBar.visibility = View.VISIBLE
        searchBar.animate()
            .alpha(1f)
            .setDuration(150)
            .start()

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float

            val newRippleWidth = (ripple.width - (ripple.width - originalRippleWidth) * progress).toInt()
            val newRippleHeight = (ripple.height - (ripple.height - originalRippleHeight) * progress).toInt()

            ripple.shapeAppearanceModel = ripple.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(dpToPx(25, context).toFloat())
                .build()

            ripple.layoutParams = ripple.layoutParams.apply {
                width = newRippleWidth
                height = newRippleHeight
            }
            ripple.requestLayout()

            if (progress == 1F) {
                ripple.shapeAppearanceModel = ripple.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(dpToPx(10, context).toFloat())
                    .build()

                ripple.visibility = View.INVISIBLE
            }
        }
        animator.duration = 300
        animator.start()
    }

    fun buildString(vararg strings: Any?): String {
        return buildString {
            for (str in strings) {
                append(str)
            }
        }
    }

    fun parseTimeToDate(timeMillis: Long, context: Context): String {
        val currentTime = System.currentTimeMillis()

        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTime }
        val targetCalendar = Calendar.getInstance().apply { timeInMillis = timeMillis }

        return when {
            currentCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
                    currentCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR) -> {
                val differenceInMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime - timeMillis)
                val hours = differenceInMinutes / 60
                val minutes = differenceInMinutes % 60

                buildString {
                    append(context.getString(R.string.edited))
                    append(" ")
                    if (hours > 0) {
                        append(hours)
                        append(" ")
                        append(context.getString(R.string.hours_ago))
                    } else if (minutes <= 1){
                        append(context.getString(R.string.just_now))
                    } else {
                        append(minutes)
                        append(" ")
                        append(context.getString(R.string.minutes_ago))
                    }
                }
            }
            currentCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
                    currentCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR) + 1 -> {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                buildString {
                    append(context.getString(R.string.edited))
                    append(" ")
                    append(context.getString(R.string.yesterday))
                    append(" ")
                    append(dateFormat.format(Date(timeMillis)))
                }
            }
            else -> {
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                buildString {
                    append(context.getString(R.string.edited))
                    append(" ")
                    append(dateFormat.format(Date(timeMillis)))
                }
            }
        }
    }

    fun loadAvatar(context: Context, url: Any, lifecycleOwner: LifecycleOwner, searchBar: SearchBar) {
        try {
            Glide.with(context)
                .load(url)
                .centerCrop()
                .circleCrop()
                .sizeMultiplier(0.50f)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.let { renderProfileImage(it, lifecycleOwner, searchBar) }
                        return true
                    }

                }).submit()
        } catch (_: Exception) {}
    }

    fun renderProfileImage(resource:Drawable, lifecycleOwner: LifecycleOwner, searchView: SearchBar) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
            searchView.menu.findItem(R.id.profile).icon = resource
        }
    }

    fun getSurfaceColor(context: Context, isDarkTheme: Boolean): Int {
        val typedValue = TypedValue()

        if (isDarkTheme) {
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimarySurface, typedValue, true)
        } else {
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        }

        return typedValue.data
    }

    fun uriToByteArray(uri: Uri, contentResolver: ContentResolver): ByteArray {
        contentResolver.openInputStream(uri).use { inputStream ->
            inputStream?.let {
                val buffer = ByteArray(8192)
                val byteArrayOutputStream = ByteArrayOutputStream()
                var bytesRead: Int
                val bufferedInputStream = BufferedInputStream(it)

                while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                return byteArrayOutputStream.toByteArray()
            } ?: throw IllegalArgumentException("Unable to open URI: $uri")
        }
    }
}