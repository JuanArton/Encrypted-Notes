package com.juanarton.privynote.ui.activity.note

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import com.github.onecode369.wysiwyg.WYSIWYG
import com.juanarton.privynote.R
import com.juanarton.privynote.databinding.ActivityNoteBinding
import com.juanarton.privynote.ui.fragment.colorpicker.ColorPickerCallback
import com.juanarton.privynote.ui.fragment.colorpicker.ColorPickerFragment
import com.juanarton.privynote.ui.utils.FragmentBuilder

class RTEHelper(val binding: ActivityNoteBinding, val context: Activity) {
    private var headerButtons: ArrayList<ImageButton> = arrayListOf()

    companion object {
        const val BACKGROUND = "BACKGROUNDCOLOR"
        const val TEXT = "TEXTCOLOR"
    }

    init {
        binding.apply {
            setupEditor()
            setupToolbar()
            etTitle.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                handleTitleFocus(hasFocus)
            }
        }
    }

    private fun ActivityNoteBinding.setupEditor() {
        val surfaceColor = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, surfaceColor, true)

        etContent.apply {
            setEditorBackgroundColor(surfaceColor.data)
            setEditorFontColor(tvEditedAt.textColors.defaultColor)
            setEditorFontSize(16)

            setPlaceholder(context.getString(R.string.note))
            setOnDecorationChangeListener(object : WYSIWYG.OnDecorationStateListener {
                override fun onStateChangeListener(text: String?, types: List<WYSIWYG.Type>?) {
                    resetToolbarButtons()
                    types?.forEach { setButtonState(it) }
                }
            })
        }
    }

    private fun ActivityNoteBinding.setupToolbar() {
        rtToolbar.apply {
            ibCloseToolbar.setOnClickListener {
                hideRTToolbar()
                basicToolbar.apply {
                    startAnimation(AlphaAnimation(0.0f, 1.0f).apply { duration = 300; startOffset = 300 })
                    visibility = View.VISIBLE
                }
            }

            btClearFormatting.setOnClickListener {
                resetToolbarButtons()
                etContent.removeFormat()
            }

            etContent.apply {
                headerButtons = arrayListOf(ibHeader1, ibHeader2, ibHeader3, ibHeader4, ibHeader5, ibHeader6)
                headerButtons.forEachIndexed { index, button ->
                    button.setOnClickListener { handleHeaderClick(button, index + 1) }
                }

                listOf(
                    ibAlignLeft to ::setAlignLeft,
                    ibAlignRight to ::setAlignRight,
                    ibAlignCenter to ::setAlignCenter,
                    ibAlignJustify to ::setAlignJustifyFull,
                    ibBold to ::setBold,
                    ibItalic to ::setItalic,
                    ibUnderline to ::setUnderline,
                    ibStrikeThrough to ::setStrikeThrough,
                    ibSubscript to ::setSubscript,
                    ibSuperscript to ::setSuperscript
                ).forEach { (button, action) ->
                    button.setOnClickListener { handleToolbarButtonClick(button, action) }
                }

                val colorPickerFragment = ColorPickerFragment()
                colorPickerFragment.setOnColorSelected(object : ColorPickerCallback{
                    override fun onColorSelected(color: Int, target: String) {
                        if (target == TEXT) {
                            etContent.setTextColor(color)
                        } else {
                            etContent.setTextBackgroundColor(color)
                        }
                    }
                })

                ibTextColor.setOnClickListener {
                    colorPickerFragment.target(TEXT)
                    FragmentBuilder.build(context as FragmentActivity, colorPickerFragment, android.R.id.content)
                }

                ibBackgroundColor.setOnClickListener {
                    colorPickerFragment.target(BACKGROUND)
                    FragmentBuilder.build(context as FragmentActivity, colorPickerFragment, android.R.id.content)
                }
            }
        }
    }

    private fun resetToolbarButtons() {
        binding.rtToolbar.run {
            listOf(
                ibHeader1, ibHeader2, ibHeader3, ibHeader4, ibHeader5, ibHeader6,
                ibAlignLeft, ibAlignRight, ibAlignCenter, ibAlignJustify,
                ibBold, ibItalic, ibUnderline, ibStrikeThrough, ibSubscript, ibSuperscript
            ).forEach {
                it.background = AppCompatResources.getDrawable(context, R.drawable.rounded_button)
                it.tag = false
            }
        }
    }

    private fun handleHeaderClick(button: ImageButton, level: Int) {
        binding.etContent.setHeading(level)
        setActiveButtonGroup(button)
    }

    private fun handleToolbarButtonClick(button: ImageButton, action: () -> Unit) {
        action()
        toggleButtonState(button)
    }

    private fun setActiveButtonGroup(activeButton: ImageButton) {
        headerButtons.forEach { button ->
            button.background = AppCompatResources.getDrawable(
                context,
                if (button == activeButton) R.drawable.rounded_button_selected else R.drawable.rounded_button
            )
        }
    }

    private fun toggleButtonState(button: ImageButton) {
        val isSelected = button.tag == true
        button.apply {
            background = AppCompatResources.getDrawable(
                context,
                if (isSelected) R.drawable.rounded_button else R.drawable.rounded_button_selected
            )
            tag = !isSelected
        }
    }

    private fun setButtonState(type: WYSIWYG.Type) {
        binding.rtToolbar.apply {
            val button = when (type) {
                WYSIWYG.Type.BOLD -> ibBold
                WYSIWYG.Type.ITALIC -> ibItalic
                WYSIWYG.Type.SUBSCRIPT -> ibSubscript
                WYSIWYG.Type.SUPERSCRIPT -> ibSuperscript
                WYSIWYG.Type.STRIKETHROUGH -> ibStrikeThrough
                WYSIWYG.Type.UNDERLINE -> ibUnderline
                WYSIWYG.Type.H1 -> ibHeader1
                WYSIWYG.Type.H2 -> ibHeader2
                WYSIWYG.Type.H3 -> ibHeader3
                WYSIWYG.Type.H4 -> ibHeader4
                WYSIWYG.Type.H5 -> ibHeader5
                WYSIWYG.Type.H6 -> ibHeader6
                WYSIWYG.Type.JUSTIFYCENTER -> ibAlignCenter
                WYSIWYG.Type.JUSTIFYFULL -> ibAlignJustify
                WYSIWYG.Type.JUSTUFYLEFT -> ibAlignLeft
                WYSIWYG.Type.JUSTIFYRIGHT -> ibAlignRight
                else -> null
            }
            button?.let { toggleButtonState(it) }
        }
    }

    private fun handleTitleFocus(hasFocus: Boolean) {
        binding.apply {
            hideRTToolbar()
            val color = if (hasFocus) etTitle.hintTextColors.defaultColor else etTitle.textColors.defaultColor
            ibTextFormat.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun hideRTToolbar() {
        binding.cvRtBar.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_down).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    binding.cvRtBar.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation) {}
            })
        })
    }
}
