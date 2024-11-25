package com.juanarton.encnotes.ui.utils

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.search.SearchBar
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.ui.activity.main.MainActivity

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
}