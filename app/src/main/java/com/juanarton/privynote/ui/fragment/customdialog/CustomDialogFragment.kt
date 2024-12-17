package com.juanarton.privynote.ui.fragment.customdialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.juanarton.privynote.databinding.FragmentCustomDialogBinding

class CustomDialogFragment(
    private var title: String? = null, private var content: String? = null,
    private var positiveButton: String? = null, private var negativeButton: String? = null
) : Fragment() {

    private var _binding: FragmentCustomDialogBinding? = null
    private val binding get() = _binding
    private var listener: CustomDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CustomDialogListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement FragmentListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCustomDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            if (!title.isNullOrEmpty()) tvTitle.text = title else tvTitle.visibility = View.GONE
            if (!content.isNullOrEmpty()) tvContent.text = content else tvContent.visibility = View.GONE
            if (!positiveButton.isNullOrEmpty()) btTrue.text = positiveButton else btTrue.visibility = View.GONE
            if (!negativeButton.isNullOrEmpty()) btFalse.text = negativeButton else btFalse.visibility = View.GONE

            btTrue.setOnClickListener {
                listener?.onButtonPressed(true)
                parentFragmentManager.beginTransaction().remove(this@CustomDialogFragment).commit()
            }

            btFalse.setOnClickListener {
                listener?.onButtonPressed(false)
                parentFragmentManager.beginTransaction().remove(this@CustomDialogFragment).commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}