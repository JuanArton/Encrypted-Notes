package com.juanarton.encnotes.ui.fragment.apppin

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.juanarton.encnotes.R
import com.juanarton.encnotes.databinding.FragmentAppPinBinding
import com.juanarton.encnotes.ui.activity.settings.SettingsActivity.Companion.TWO_FACTOR

class AppPinFragment(
    private val pinMessage: String,
    private val isSetPassword: Boolean,
    private val action: Int,
) : Fragment() {

    private var _binding: FragmentAppPinBinding? = null
    private val binding get() = _binding
    private var listener: PinListener? = null
    private var firstPassword = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PinListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement FragmentListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppPinBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            tvPinMessage.text = pinMessage

            if (action == TWO_FACTOR) {
                otpView.itemCount = 6
            }

            otpView.setOtpCompletionListener {
                if (isSetPassword) {
                    if (firstPassword == 0) {
                        firstPassword = it.toInt()
                        otpView.text = Editable.Factory.getInstance().newEditable("")
                        tvPinMessage.text = getString(R.string.please_reenter_pin)
                    } else if (firstPassword == it.toInt()) {
                        listener?.onPinSubmit(it.toInt(), action)
                        destroySelf()
                    }
                } else {
                    listener?.onPinSubmit(it.toInt(), action)
                    destroySelf()
                }
            }
        }
    }

    fun destroySelf() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }
}