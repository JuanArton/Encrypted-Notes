package com.juanarton.privynote.ui.fragment.colorpicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.juanarton.privynote.databinding.FragmentColorPickerBinding
import com.skydoves.colorpickerview.listeners.ColorListener

class ColorPickerFragment : Fragment() {

    private var _binding: FragmentColorPickerBinding? = null
    private val binding get() = _binding

    private var callback: ColorPickerCallback? = null
    private var target = ""

    fun setOnColorSelected(callback: ColorPickerCallback) {
        this.callback = callback
    }

    fun target(target: String) {
        this.target = target
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentColorPickerBinding.inflate(inflater, container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            var selectedColor = 0
            colorPickerView.setColorListener(object : ColorListener{
                override fun onColorSelected(color: Int, fromUser: Boolean) {
                    colorBox.setBackgroundColor(color)
                    selectedColor = color
                }
            })

            colorPickerView.attachBrightnessSlider(brightnessSlide)

            btDone.setOnClickListener {
                if (selectedColor != 0) {
                    callback?.onColorSelected(selectedColor, target)
                }
                parentFragmentManager.beginTransaction().remove(this@ColorPickerFragment).commit()
            }

            btCancel.setOnClickListener {
                parentFragmentManager.beginTransaction().remove(this@ColorPickerFragment).commit()
            }
        }
    }
}