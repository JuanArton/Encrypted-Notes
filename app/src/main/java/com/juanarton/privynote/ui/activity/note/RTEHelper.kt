package com.juanarton.privynote.ui.activity.note

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import com.juanarton.privynote.R
import com.juanarton.privynote.databinding.ActivityNoteBinding

class RTEHelper(val binding: ActivityNoteBinding, val context: Activity) {
    init {
        binding.apply {
            val surfaceColor = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, surfaceColor, true)

            etContent.setEditorBackgroundColor(surfaceColor.data)
            etContent.setEditorFontColor(tvEditedAt.textColors.defaultColor)
            etContent.loadCSS()

            rtToolbar.apply {
                ibCloseToolbar.setOnClickListener {
                    val slideDown = AnimationUtils.loadAnimation(context, R.anim.slide_down)

                    slideDown.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            cvRtBar.visibility = View.GONE

                            val view = if (etTitle.isFocused) etTitle else etContent
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(view.windowToken, 0)

                            view.requestFocus()
                            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })

                    cvRtBar.startAnimation(slideDown)
                }
            }
        }
    }
}